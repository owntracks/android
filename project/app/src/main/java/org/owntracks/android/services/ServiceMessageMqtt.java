package org.owntracks.android.services;

import android.annotation.TargetApi;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Build;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.util.Log;

import org.eclipse.paho.client.mqttv3.IMqttActionListener;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.IMqttToken;
import org.eclipse.paho.client.mqttv3.MqttAsyncClient;
import org.eclipse.paho.client.mqttv3.MqttCallbackExtended;
import org.eclipse.paho.client.mqttv3.MqttClientPersistence;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.MqttPersistable;
import org.eclipse.paho.client.mqttv3.MqttPersistenceException;
import org.eclipse.paho.client.mqttv3.MqttPingSender;
import org.eclipse.paho.client.mqttv3.internal.ClientComms;
import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;
import org.json.JSONException;
import org.json.JSONObject;
import org.owntracks.android.App;
import org.owntracks.android.BuildConfig;
import org.owntracks.android.messages.MessageBase;
import org.owntracks.android.messages.MessageCmd;
import org.owntracks.android.messages.MessageEvent;
import org.owntracks.android.messages.MessageLocation;
import org.owntracks.android.messages.MessageTransition;
import org.owntracks.android.messages.MessageWaypoint;
import org.owntracks.android.messages.MessageWaypoints;
import org.owntracks.android.support.Events;
import org.owntracks.android.support.OutgoingMessageProcessor;
import org.owntracks.android.support.PausableThreadPoolExecutor;
import org.owntracks.android.support.Preferences;
import org.owntracks.android.support.SocketFactory;
import org.owntracks.android.support.StatisticsProvider;
import org.owntracks.android.support.interfaces.StatefulServiceMessageEndpoint;
import org.owntracks.android.support.Parser;
import org.owntracks.android.services.ServiceMessage.EndpointState;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.List;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import timber.log.Timber;

public class ServiceMessageMqtt implements OutgoingMessageProcessor, RejectedExecutionHandler, StatefulServiceMessageEndpoint {
	private static final String TAG = "ServiceMessageMqtt";
	public static final String RECEIVER_ACTION_RECONNECT = "org.owntracks.android.RECEIVER_ACTION_RECONNECT";
    public static final String RECEIVER_ACTION_PING = "org.owntracks.android.RECEIVER_ACTION_PING";
	private static final int MAX_INFLIGHT_MESSAGES = 10;


	private ServiceProxy context;

	private PausableThreadPoolExecutor pubPool;
	private BroadcastReceiver idleReceiver;
	private PowerManager powerManager;
	private ServiceMessage service;

	@Override
	public void onSetService(ServiceMessage service) {
		this.service = service;
		changeState(EndpointState.INITIAL);
		doStart();
	}

	@Override
	public boolean sendMessage(MessageBase message) {
		Log.v(TAG, "sendMessage base: " + message + " " + message.getClass());

		if(state == ServiceMessage.EndpointState.ERROR_CONFIGURATION) {
			Log.e(TAG, "dropping outgoing message due to incomplete configuration");
			return false;
		}

		message.setOutgoingProcessor(this);
		Log.v(TAG, "enqueueing message to pubPool. running: " + pubPool.isRunning() + ", q size:" + pubPool.getQueue().size());
		StatisticsProvider.setInt(StatisticsProvider.SERVICE_MESSAGE_QUEUE_LENGTH, pubPool.getQueueLength());

		this.pubPool.queue(message);
		return true;
	}

	@Override
	public void processOutgoingMessage(MessageBase message) {
		message.setTopic(Preferences.getPubTopicBase());
		publishMessage(message);
	}

	@Override
	public void processOutgoingMessage(MessageCmd message) {
		message.setTopic(Preferences.getPubTopicCommands());
		publishMessage(message);
	}

	@Override
	public void processOutgoingMessage(MessageEvent message) {
		publishMessage(message);
	}

	@Override
	public void processOutgoingMessage(MessageLocation message) {
		message.setTopic(Preferences.getPubTopicLocations());
		message.setQos(Preferences.getPubQosLocations());
		message.setRetained(Preferences.getPubRetainLocations());

		publishMessage(message);
	}

	@Override
	public void processOutgoingMessage(MessageTransition message) {
		message.setTopic(Preferences.getPubTopicEvents());
		message.setQos(Preferences.getPubQosEvents());
		message.setRetained(Preferences.getPubRetainEvents());

		publishMessage(message);
	}

	@Override
	public void processOutgoingMessage(MessageWaypoint message) {
		message.setTopic(Preferences.getPubTopicWaypoints());
		message.setQos(Preferences.getPubQosWaypoints());
		message.setRetained(Preferences.getPubRetainWaypoints());

		publishMessage(message);
	}

	@Override
	public void processOutgoingMessage(MessageWaypoints message) {
		message.setTopic(Preferences.getPubTopicWaypoints());
		message.setQos(Preferences.getPubQosWaypoints());
		message.setRetained(Preferences.getPubRetainWaypoints());

		publishMessage(message);
	}


	private IMqttActionListener iCallbackPublish = new IMqttActionListener() {

		@Override
		public void onSuccess(IMqttToken token) {
			if(!(token.getUserContext() instanceof MessageBase))
				return;

			Timber.v("messageId: %s", MessageBase.class.cast(token.getUserContext()).getMessageId());
			service.onMessageDelivered(MessageBase.class.cast(token.getUserContext()).getMessageId());

			if(pubPool.isPaused() && mqttClient.getPendingDeliveryTokens().length <= MAX_INFLIGHT_MESSAGES) {
				Timber.v("resuming pubPool that was paused due to back pressure. Currently outstanding tokens: %s", mqttClient.getPendingDeliveryTokens().length);
				pubPool.resume();
			}

		}

		@Override
		public void onFailure(IMqttToken token, Throwable exception) {
			if(!(token instanceof MessageBase))
				return;

			Timber.v("messageId: %s", MessageBase.class.cast(token).getMessageId());
			exception.printStackTrace();
			service.onMessageDeliveryFailed(MessageBase.class.cast(token.getUserContext()).getMessageId());
		}
	};

	private MqttCallbackExtended iCallbackClient = new MqttCallbackExtended() {
		@Override
		public void connectComplete(boolean reconnect, String serverURI) {
			Timber.v("reconnect:%s, serverUri:%s", reconnect, serverURI);
		}

		@Override
		public void deliveryComplete(IMqttDeliveryToken token) {
			//Handled by iCallbackPublish

		}

		@Override
		public void connectionLost(Throwable cause) {
			Timber.e(cause, "");
			pubPool.pause();
			//pingHandler.stop(); Ping handler is automatically stopped by mqttClient
			reconnectHandler.schedule();

		}

		@Override
		public void messageArrived(String topic, MqttMessage message) throws Exception {

			try {
				MessageBase m = Parser.deserializeSync(message.getPayload());
				if(!m.isValidMessage()) {
					Timber.e("message failed validation: %s", message.getPayload());
					return;
				}

				m.setTopic(getBaseTopic(m, topic));
				m.setRetained(message.isRetained());
				m.setQos(message.getQos());
				service.onMessageReceived(m);
			} catch (Exception e) {
				Timber.e("payload:%s ", new String(message.getPayload()));
			}

		}

	};



	private void publishMessage(MessageBase message) {

		Log.v(TAG, "publishMessage: " + message + ", q size: " + pubPool.getQueue().size());
		try {
			MqttMessage m = new MqttMessage();
			m.setPayload(Parser.serializeSync(message).getBytes());
			m.setQos(message.getQos());
			m.setRetained(message.getRetained());


			if(this.mqttClient == null) {
				Log.e(TAG, "forcing null of mqttclient");
				this.pubPool.pause();
				this.pubPool.requeue(message);
				return;
			}
			Log.v(TAG, "publishing message " + message + " to topic " + message.getTopic() );
			IMqttDeliveryToken pubToken = this.mqttClient.publish(message.getTopic(), m);
			pubToken.setActionCallback(iCallbackPublish);
			pubToken.setUserContext(message);

			if(this.mqttClient.getPendingDeliveryTokens().length >= MAX_INFLIGHT_MESSAGES) {
				Log.v(TAG, "pausing pubPool due to back preassure. Outstanding tokens: " + this.mqttClient.getPendingDeliveryTokens().length);
				this.pubPool.pause();
			}
		} catch (MqttException e) {
			Log.e(TAG, "processIncomingMessage: MqttException. " + e.getCause() + " " + e.getReasonCode() + " " + e.getMessage());
			e.printStackTrace();
		} catch (Parser.EncryptionException e) {
			e.printStackTrace();
		} catch (IOException e) {
			Log.e(TAG, "processIncomingMessage: JsonProcessingException");
			e.printStackTrace();
		}

	}




	private static ServiceMessage.EndpointState state = ServiceMessage.EndpointState.INITIAL;

	private CustomMqttClient mqttClient;
	private Thread workerThread;
	private static Exception error;
    private MqttClientPersistence persistenceStore;
    private ReconnectHandler reconnectHandler;
	private PingHandler pingHandler;
	private boolean connectedWithCleanSession;

	private MqttConnectOptions lastConnectionOptions;
	private String lastConnectionId = null;



	public void onCreate(ServiceProxy context) {
		Timber.v("loaded MQTT endoint");
		this.context = context;
        this.persistenceStore = new CustomMemoryPersistence();
        this.reconnectHandler = new ReconnectHandler(context);
		this.powerManager = PowerManager.class.cast(context.getSystemService(Context.POWER_SERVICE));
		initPausedPubPool();
		registerReceiver();
	}

	private void initPausedPubPool() {
		Log.v(TAG, "Executor initPausedPubPool with new paused queue");
		if(pubPool != null && !pubPool.isShutdown()) {
			Log.v(TAG, "Executor shutting down existing executor " + pubPool);
			pubPool.shutdownNow();
		}
		this.pubPool = new PausableThreadPoolExecutor(1,1,1, TimeUnit.MINUTES,new LinkedBlockingQueue<Runnable>());
		this.pubPool.setRejectedExecutionHandler(this);
		Log.v(TAG, "Executor created new executor instance: " + pubPool);
		pubPool.pause(); // pause until client is setup and connected
	}


	@Override
	public void rejectedExecution(Runnable r, ThreadPoolExecutor executor) {
		Log.v(TAG, "Executor task execution rejected for executor " + executor + " -> task: " + r);
	}

	@Override
	public void onStartCommand(Intent intent, int flags, int startId) {
		Log.v(TAG, "onStartCommand intent:"+intent.getAction());

		if(ServiceMessageMqtt.RECEIVER_ACTION_RECONNECT.equals(intent.getAction())) {
			Log.v(TAG,	 "onStartCommand ServiceMessageMqtt.RECEIVER_ACTION_RECONNECT");
			doStart();

		} else if(ServiceMessageMqtt.RECEIVER_ACTION_PING.equals(intent.getAction())) {
			Log.v(TAG,	 "onStartCommand ServiceMessageMqtt.RECEIVER_ACTION_PING");

			if(pingHandler != null)
				pingHandler.ping(intent);
			else
				doStart();
		}
	}

	private void doStart() {
		doStart(false);
	}

	private void doStart(final boolean force) {
		Thread thread1 = new Thread() {
			@Override
			public void run() {
				Log.v(TAG, "running thread");
				handleStart(force);
				if (this == ServiceMessageMqtt.this.workerThread)
					ServiceMessageMqtt.this.workerThread = null;
			}

			@Override
			public void interrupt() {
				Log.v(TAG, "worker thread interrupt");
				if (this == ServiceMessageMqtt.this.workerThread)
					ServiceMessageMqtt.this.workerThread = null;
				super.interrupt();
			}
		};
		thread1.start();
	}

	void handleStart(boolean force) {


		Log.v(TAG, "handleStart: force:" + force);
        if(!Preferences.canConnect()) {
			changeState(EndpointState.ERROR_CONFIGURATION);
			return;
        }

		// Respect user's wish to stay disconnected. Overwrite with force = true
		// to reconnect manually afterwards
		if ((state == EndpointState.DISCONNECTED_USERDISCONNECT) && !force) {
			return;
		}

		if (isConnecting()) {
			Log.d(TAG, "handleStart: isConnecting:true");
			return;
		}

		// Check if there is a data connection. If not, try again in some time.
		if (!isOnline()) {
			Log.e(TAG, "handleStart: isOnline:false");
			changeState(EndpointState.ERROR_DATADISABLED);

			if(pingHandler != null) {
				pingHandler.stop();
			}
			reconnectHandler.schedule(); // we will try again to connect after some time
			return;
		}

		if (isDisconnected()) {
				Log.v(TAG, "handleStart: isDisconnected:true");
				changeState(EndpointState.DISCONNECTED);

				if (connect())
					onConnect();
				else // connect didn't work
					reconnectHandler.schedule();

		} else {
			Log.d(TAG, "handleStart: isDisconnected() == false");
		}
	}

	private boolean isDisconnected() {

		return (state == EndpointState.INITIAL)
				|| (state == EndpointState.DISCONNECTED)
				|| (state == EndpointState.DISCONNECTED_USERDISCONNECT)
				|| (state == EndpointState.ERROR_DATADISABLED)
				|| (state == EndpointState.ERROR)

				// In some cases the internal state may diverge from the mqtt
				// client state.
				|| !isConnected();
	}

	private boolean init() {
		if (this.mqttClient != null) {
			return true;
		}

		try {
			String prefix = "tcp";

			if (Preferences.getTls()) {
				if (Preferences.getWs())
					prefix = "wss";
				else
					prefix = "ssl";
			} else {
				if (Preferences.getWs())
					prefix = "ws";
			}

			String cid = Preferences.getClientId(true);
            String connectString = prefix + "://" + Preferences.getHost() + ":" + Preferences.getPort();
			Log.v(TAG, "init() mode: " + Preferences.getModeId());
			Log.v(TAG, "init() client id: " + cid);
			Log.v(TAG, "init() connect string: " + connectString);

            this.pingHandler = new PingHandler(context);
			this.mqttClient = new CustomMqttClient(connectString, cid, persistenceStore, pingHandler);
			this.mqttClient.setCallback(iCallbackClient);
			Timber.v("clientInstance:%s", this.mqttClient);
		} catch (Exception e) {
			// something went wrong!
			this.mqttClient = null;
			changeState(e);
            return false;
		}
        return true;
	}

	private boolean connect() {
        this.workerThread = Thread.currentThread(); // We connect, so we're the worker thread
        changeState(EndpointState.CONNECTING);
		Log.v(TAG, "connect() mode: " + Preferences.getModeId());

		error = null; // clear previous error on connect
		if(!init()) {
            return false;
        }

		try {
			 lastConnectionOptions = new MqttConnectOptions();
			if (Preferences.getAuth()) {
				lastConnectionOptions.setPassword(Preferences.getPassword().toCharArray());
				lastConnectionOptions.setUserName(Preferences.getUsername());
			}

			if (Preferences.getTls()) {
				String tlsCaCrt = Preferences.getTlsCaCrtName();
				String tlsClientCrt = Preferences.getTlsClientCrtName();

				SocketFactory.SocketFactoryOptions socketFactoryOptions = new SocketFactory.SocketFactoryOptions();

				if (tlsCaCrt.length() > 0) {
					try {
						socketFactoryOptions.withCaInputStream(context.openFileInput(tlsCaCrt));
					} catch (FileNotFoundException e) {
						e.printStackTrace();
					}
				}

				if (tlsClientCrt.length() > 0)						{
					try {
						socketFactoryOptions.withClientP12InputStream(context.openFileInput(tlsClientCrt)).withClientP12Password(Preferences.getTlsClientCrtPassword());
					} catch (FileNotFoundException e1) {
						e1.printStackTrace();
					}
				}



				lastConnectionOptions.setSocketFactory(new SocketFactory(socketFactoryOptions));
			}


            setWill(lastConnectionOptions);
			lastConnectionOptions.setKeepAliveInterval(Preferences.getKeepalive());
			lastConnectionOptions.setConnectionTimeout(30);
			connectedWithCleanSession = Preferences.getCleanSession();
			lastConnectionOptions.setCleanSession(connectedWithCleanSession);

			this.mqttClient.connect(lastConnectionOptions).waitForCompletion();
			changeState(EndpointState.CONNECTED);

			return true;

		} catch (Exception e) { // Catch paho and socket factory exceptions
			Log.e(TAG, e.toString());
            e.printStackTrace();
			changeState(e);
			return false;
		}
	}

	private void setWill(MqttConnectOptions m) {
        try {
            JSONObject lwt = new JSONObject();
            lwt.put("_type", "lwt");
            lwt.put("tst", (int) TimeUnit.MILLISECONDS.toSeconds(System.currentTimeMillis()));

            m.setWill(Preferences.getPubTopicBase(true), lwt.toString().getBytes(), 0, false);
        } catch(JSONException ignored) {}

	}

	private String getConnectionId() {
		return mqttClient.getCurrentServerURI()+"/"+lastConnectionOptions.getUserName();
	}

	private void onConnect() {

		// Check if we're connecting to the same broker that we were already connected to
		String connectionId = getConnectionId();
		if(lastConnectionId != null && !connectionId.equals(lastConnectionId)) {
			App.getEventBus().post(new Events.BrokerChanged());
			lastConnectionId = connectionId;
			Log.v(TAG, "lastConnectionId changed to: " + lastConnectionId);
		}

		reconnectHandler.stop();

		// Establish observer to monitor wifi and radio connectivity
		if (connectedWithCleanSession)
			onCleanSessionConnect();
		else
			onUncleanSessionConnect();

		onSessionConnect();
		pubPool.resume();

	}


	private void onCleanSessionConnect() {
	}

	private void onUncleanSessionConnect() {
	}

	private void onSessionConnect() {
		subscribToInitialTopics();
	}

	public void subscribToInitialTopics() {
		List<String> topics = new ArrayList<>();
		String subTopicBase = Preferences.getSubTopic();

		if(!Preferences.getSub()) // Don't subscribe if base topic is invalid
			return;
		else if(subTopicBase.endsWith("#")) { // wildcard sub will match everything anyway
			topics.add(subTopicBase);
		} else {

			topics.add(subTopicBase);
			if(Preferences.getInfo())
				topics.add(subTopicBase + Preferences.getPubTopicInfoPart());

			if (!Preferences.isModeMqttPublic())
				topics.add(Preferences.getPubTopicBase(true) + Preferences.getPubTopicCommandsPart());

			if (!Preferences.isModeMqttPublic()) {
				topics.add(subTopicBase + Preferences.getPubTopicEventsPart());
				topics.add(subTopicBase + Preferences.getPubTopicWaypointsPart());
			}


		}

		subscribe(topics.toArray(new String[topics.size()]));

	}


    private void subscribe(String[] topics) {
		if(!isConnected()) {
            Log.e(TAG, "subscribe when not connected");
            return;
        }
        for(String s : topics) {
            Log.v(TAG, "subscribe() - Will subscribe to: " + s);
        }
		try {
			int qos[] = getSubTopicsQos(topics);

			this.mqttClient.subscribe(topics, qos);

		} catch (Exception e) {
			e.printStackTrace();
		}
    }


	private int[] getSubTopicsQos(String[] topics) {
		int[] qos = new int[topics.length];
		Arrays.fill(qos, 2);
		return qos;
	}

	@SuppressWarnings("unused")
	private void unsubscribe(String[] topics) {
		if(!isConnected()) {
			Log.e(TAG, "subscribe when not connected");
			return;
		}

		for(String s : topics) {
			Log.v(TAG, "unsubscribe() - Will unsubscribe from: " + s);
		}

		try {
			mqttClient.unsubscribe(topics);
		} catch (Exception e) {
			e.printStackTrace();
		}
    }


	private void disconnect(boolean fromUser) {
		Log.v(TAG, "disconnect. from user: " + fromUser);

		if (isConnecting()) {
            return;
        }

		pingHandler.stop();

		try {
			if (isConnected()) {
				Log.v(TAG, "Disconnecting");
				this.mqttClient.disconnect(0);
			}
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			this.mqttClient = null;

			if (this.workerThread != null) {
				this.workerThread.interrupt();
			}

			if (fromUser)
				changeState(EndpointState.DISCONNECTED_USERDISCONNECT);
			else
				changeState(EndpointState.DISCONNECTED);
		}
	}



	public void reconnect() {
		disconnect(false);
		doStart(true);
	}

	@Override
	public void disconnect() {
		disconnect(true);
	}

	private void changeState(Exception e) {
		error = e;
		changeState(EndpointState.ERROR, e);
	}

	private void changeState(EndpointState newState) {
		changeState(newState, null);
	}

	private void changeState(EndpointState newState, Exception e) {
		state = newState;
		service.onEndpointStateChanged(newState, e);
	}

	private boolean isOnline() {
		ConnectivityManager cm = (ConnectivityManager) this.context.getSystemService(Context.CONNECTIVITY_SERVICE);
		NetworkInfo netInfo = cm.getActiveNetworkInfo();

        if(netInfo != null && netInfo.isAvailable() && netInfo.isConnected()) {
            return true;
        } else {
            Log.e(TAG, "isOnline == false. activeNetworkInfo: "+ (netInfo != null) +", available:" + (netInfo != null && netInfo.isAvailable()) + ", connected:" + (netInfo != null && netInfo.isConnected()));
            return false;
        }
	}

	public boolean isConnected() {
		return this.mqttClient != null && this.mqttClient.isConnected(  );
	}

	public boolean isConnecting() {
		return (this.mqttClient != null)
				&& (state == EndpointState.CONNECTING);
	}

	@Override
	public void onDestroy() {
		// disconnect immediately
		disconnect(false);
		unregisterReceiver();
		changeState(EndpointState.DISCONNECTED);
	}

	public static EndpointState getState() {
		return state;
	}

	@Override
	public boolean isReady() {
		return this.service != null && this.mqttClient != null;
	}


	public Exception getError() {
        return error;
    }



	private String getBaseTopic(MessageBase message, String topic){

		if (message.getBaseTopicSuffix() != null && topic.endsWith(message.getBaseTopicSuffix())) {
			return topic.substring(0, (topic.length() - message.getBaseTopicSuffix().length()));
		} else {
			return topic;
		}
	}

	@Subscribe
	public void onEvent(Events.Dummy e) {
	}


	public void clearQueues() {
		initPausedPubPool();
		StatisticsProvider.setInt(StatisticsProvider.SERVICE_MESSAGE_QUEUE_LENGTH, 0);
    }

	@Subscribe
	public void onEvent(Events.ModeChanged e) {
		Log.v(TAG, "ModeChanged. Disconnecting and draining message queue");
        disconnect(false);
        clearQueues();
    }

	@Subscribe
	public void onEvent(Events.BrokerChanged e) {
        clearQueues();
    }



    // Custom blocking MqttClient that allows to specify a MqttPingSender
    private static final class CustomMqttClient extends MqttAsyncClient {
        public CustomMqttClient(String serverURI, String clientId, MqttClientPersistence persistence, MqttPingSender pingSender) throws MqttException {
            super(serverURI, clientId, persistence, pingSender);// Have to call do the AsyncClient init twice as there is no other way to setup a client with a ping sender (thanks Paho)
        }
    }

	@TargetApi(Build.VERSION_CODES.M)
	private void onDeviceIdleChanged() {
		if(powerManager.isDeviceIdleMode()) {
			Timber.v("idleMode: enabled");
		} else {
			Timber.v("idleMode: disabled");

		}
	}

	private void unregisterReceiver() {
		if(idleReceiver != null)
			context.unregisterReceiver(idleReceiver);
	}


	private void registerReceiver() {
		IntentFilter filter = new IntentFilter();
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
			filter.addAction(PowerManager.ACTION_DEVICE_IDLE_MODE_CHANGED);

			idleReceiver = new BroadcastReceiver() {
				@Override
				public void onReceive(Context context, Intent intent) {
					onDeviceIdleChanged();
				}
			};
			context.registerReceiver(idleReceiver, filter);
		}

	}


	class PingHandler implements MqttPingSender {
        static final String TAG = "PingHandler";

        private ClientComms comms;
        private Context context;
		private WakeLock wakelock;

		private boolean releaseWakeLock() {
			if(wakelock != null && wakelock.isHeld()){
				Log.d(TAG, "Release lock ok(" + ServiceProxy.WAKELOCK_TAG_BROKER_PING + "):" + System.currentTimeMillis());

				wakelock.release();
				return true;
			}
			Log.d(TAG, "Release lock underlock or null (" + ServiceProxy.WAKELOCK_TAG_BROKER_PING + "):" + System.currentTimeMillis());
			return false;
		}

        public void ping(Intent intent) {
			Log.v(TAG, "sending");

			if (wakelock == null) {
				wakelock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, ServiceProxy.WAKELOCK_TAG_BROKER_PING);
			}

			if(!wakelock.isHeld())
				wakelock.acquire();

			if(comms == null) {
				Log.v(TAG, "comms is null, running doStart()");
				PendingIntent p = ServiceProxy.getBroadcastIntentForService(this.context, ServiceProxy.SERVICE_MESSAGE, RECEIVER_ACTION_RECONNECT, null);
				try {
					p.send();
				} catch (PendingIntent.CanceledException ignored) {

				} finally {
					Log.v(TAG, "releaseWakeLock 1");
					releaseWakeLock();
				}
				return;
			}


			IMqttToken token = comms.checkForActivity(new IMqttActionListener() {

				@Override
				public void onSuccess(IMqttToken asyncActionToken) {
					Log.d(TAG, "Success. Release lock(" + ServiceProxy.WAKELOCK_TAG_BROKER_PING + "):" + System.currentTimeMillis());
					Log.v(TAG, "releaseWakeLock 2 onSuccess");

					releaseWakeLock();
				}

				@Override
				public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
					Log.d(TAG, "Failure. Release lock(" + ServiceProxy.WAKELOCK_TAG_BROKER_PING + "):" + System.currentTimeMillis());
					Log.v(TAG, "releaseWakeLock 3 onFailure");

					releaseWakeLock();
				}
			});

		}

        public PingHandler(Context c) {
            if (c == null) {
                throw new IllegalArgumentException( "Neither service nor client can be null.");
            }
            this.context = c;
        }

        @Override
        public void init(ClientComms comms) {
            Log.v(TAG, "init " + this);
			this.comms = comms;
        }

        @Override
        public void start() {
            Log.v(TAG, "start " + this);
			if(comms != null)
	            schedule(comms.getKeepAlive());
        }

        @Override
        public void stop() {
            Log.v(TAG, "stop " + this);
            AlarmManager alarmManager = (AlarmManager) context.getSystemService(ServiceProxy.ALARM_SERVICE);
            alarmManager.cancel(ServiceProxy.getBroadcastIntentForService(context, ServiceProxy.SERVICE_MESSAGE, ServiceMessageMqtt.RECEIVER_ACTION_PING, null));
        }

		// Schedules a BroadcastIntent that will trigger a ping message when received.
		// It will be received by ServiceMessageMqtt.onStartCommand which recreates the service in case it has been stopped
		// onStartCommand will then deliver the intent to the ping(...) method if the service was alive or it will trigger a new connection attempt
        @Override
        public void schedule(long delayInMilliseconds) {

			long targetTstMs = System.currentTimeMillis() + delayInMilliseconds;
			AlarmManager alarmManager = (AlarmManager) context.getSystemService(ServiceProxy.ALARM_SERVICE);
			PendingIntent p = ServiceProxy.getBroadcastIntentForService(context, ServiceProxy.SERVICE_MESSAGE, ServiceMessageMqtt.RECEIVER_ACTION_PING, null);
			if (Build.VERSION.SDK_INT >= 19) {
				alarmManager.setExact(AlarmManager.RTC_WAKEUP, targetTstMs, p);
			} else {
				alarmManager.set(AlarmManager.RTC_WAKEUP, targetTstMs, p);
			}

			Log.v(TAG, "scheduled ping at tst " + (targetTstMs) +" (current: " + System.currentTimeMillis() +" /"+ delayInMilliseconds+ ")");

        }
    }

    class ReconnectHandler {
		private static final String TAG = "ReconnectHandler";
		private static final int BACKOFF_INTERVAL_MAX = 6;
		private int backoff = 0;

		private final Context context;
        private boolean hasStarted;


        public ReconnectHandler(Context context) {
            this.context = context;
        }




        public void stop() {
            Log.v(TAG, "stopping reconnect handler");
			backoff = 0;
            AlarmManager alarmManager = (AlarmManager) context.getSystemService(ServiceProxy.ALARM_SERVICE);
            alarmManager.cancel(ServiceProxy.getBroadcastIntentForService(this.context, ServiceProxy.SERVICE_MESSAGE, RECEIVER_ACTION_RECONNECT, null));

            if (hasStarted) {
                hasStarted = false;
            }
        }

        public void schedule() {
			AlarmManager alarmManager = (AlarmManager) context.getSystemService(ServiceProxy.ALARM_SERVICE);
			long delayInMilliseconds;
			if(BuildConfig.DEBUG)
				 delayInMilliseconds = TimeUnit.SECONDS.toMillis(10);
			else
				delayInMilliseconds =  (long)Math.pow(2, backoff) * TimeUnit.MINUTES.toMillis(30);

			Log.v(TAG, "scheduling reconnect handler delay:"+delayInMilliseconds);

			PendingIntent p = ServiceProxy.getBroadcastIntentForService(this.context, ServiceProxy.SERVICE_MESSAGE, RECEIVER_ACTION_RECONNECT, null);
			if (Build.VERSION.SDK_INT >= 19) {
				alarmManager.setExact(AlarmManager.RTC_WAKEUP, System.currentTimeMillis() + delayInMilliseconds, p);
			} else {
				alarmManager.set(AlarmManager.RTC_WAKEUP, System.currentTimeMillis() + delayInMilliseconds, p);
			}

			if(backoff <= BACKOFF_INTERVAL_MAX)
				backoff++;
		}

	}

    private static final class CustomMemoryPersistence implements MqttClientPersistence {
        private static Hashtable data;

        public CustomMemoryPersistence(){

        }

        @Override
        public void open(String s, String s2) throws MqttPersistenceException {
            if(data == null) {
                data = new Hashtable();
            }
        }

		@SuppressWarnings("unused")
        private Integer getSize(){
            return data.size();
        }

        @Override
        public void close() throws MqttPersistenceException {

        }

        @Override
        public void put(String key, MqttPersistable persistable) throws MqttPersistenceException {
            data.put(key, persistable);
        }

        @Override
        public MqttPersistable get(String key) throws MqttPersistenceException {
            return (MqttPersistable)data.get(key);
        }

        @Override
        public void remove(String key) throws MqttPersistenceException {
            data.remove(key);
        }

        @Override
        public Enumeration keys() throws MqttPersistenceException {
            return data.keys();
        }

        @Override
        public void clear() throws MqttPersistenceException {
            data.clear();
        }

        @Override
        public boolean containsKey(String key) throws MqttPersistenceException {
            return data.containsKey(key);
        }
    }

}
