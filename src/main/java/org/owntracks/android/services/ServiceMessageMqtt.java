package org.owntracks.android.services;

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

import com.fasterxml.jackson.core.JsonProcessingException;

import org.eclipse.paho.client.mqttv3.IMqttActionListener;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.IMqttToken;
import org.eclipse.paho.client.mqttv3.MqttAsyncClient;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttClientPersistence;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.MqttPersistable;
import org.eclipse.paho.client.mqttv3.MqttPersistenceException;
import org.eclipse.paho.client.mqttv3.MqttPingSender;
import org.eclipse.paho.client.mqttv3.internal.ClientComms;
import org.json.JSONException;
import org.json.JSONObject;
import org.owntracks.android.R;
import org.owntracks.android.messages.MessageBase;
import org.owntracks.android.messages.MessageCmd;
import org.owntracks.android.messages.MessageEncrypted;
import org.owntracks.android.messages.MessageEvent;
import org.owntracks.android.messages.MessageLocation;
import org.owntracks.android.messages.MessageTransition;
import org.owntracks.android.messages.MessageUnknown;
import org.owntracks.android.messages.MessageWaypoint;
import org.owntracks.android.messages.MessageWaypoints;
import org.owntracks.android.support.EncryptionProvider;
import org.owntracks.android.support.Events;
import org.owntracks.android.support.OutgoingMessageProcessor;
import org.owntracks.android.support.PausableThreadPoolExecutor;
import org.owntracks.android.support.Preferences;
import org.owntracks.android.support.SocketFactory;
import org.owntracks.android.support.StatisticsProvider;
import org.owntracks.android.support.interfaces.MessageReceiver;
import org.owntracks.android.support.interfaces.MessageSender;
import org.owntracks.android.support.interfaces.ServiceMessageEndpoint;
import org.owntracks.android.support.receiver.Parser;
import org.owntracks.android.services.ServiceMessage.EndpointState;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.List;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import de.greenrobot.event.EventBus;

public class ServiceMessageMqtt implements MqttCallback, ProxyableService, OutgoingMessageProcessor, RejectedExecutionHandler, ServiceMessageEndpoint {
	private static final String TAG = "ServiceMessageMqtt";
	public static final String RECEIVER_ACTION_RECONNECT = "org.owntracks.android.RECEIVER_ACTION_RECONNECT";
    public static final String RECEIVER_ACTION_PING = "org.owntracks.android.RECEIVER_ACTION_PING";
	private static final int MAX_INFLIGHT_MESSAGES = 10;


	private ServiceProxy context;

	private PausableThreadPoolExecutor pubPool;
	private MessageSender messageSender;
	private MessageReceiver messageReceiver;

	@Override
	public void sendMessage(MessageBase message) {
		Log.v(TAG, "sendMessage base: " + message + " " + message.getClass());

		if(state == ServiceMessage.EndpointState.DISCONNECTED_CONFIGINCOMPLETE) {
			Log.e(TAG, "dropping outgoing message due to incomplete configuration");
			return;
		}

		message.setOutgoingProcessor(this);
		Log.v(TAG, "enqueueing message to pubPool. running: " + pubPool.isRunning() + ", q size:" + pubPool.getQueue().size());
		StatisticsProvider.setInt(StatisticsProvider.SERVICE_BROKER_QUEUE_LENGTH, pubPool.getQueueLength());

		this.pubPool.queue(message);
		this.messageSender.onMessageQueued(message);
	}

	@Override
	public void setMessageSenderCallback(MessageSender callback) {
		this.messageSender = callback;
	}

	@Override
	public void setMessageReceiverCallback(MessageReceiver callback) {
		this.messageReceiver = callback;
	}




	@Override
	public void processMessage(MessageBase message) {
		message.setTopic(Preferences.getPubTopicBase());
		publishMessage(message);
	}

	@Override
	public void processMessage(MessageCmd message) {
		message.setTopic(Preferences.getPubTopicCommands());
		publishMessage(message);
	}

	@Override
	public void processMessage(MessageEvent message) {
		publishMessage(message);
	}

	@Override
	public void processMessage(MessageLocation message) {
		message.setTopic(Preferences.getPubTopicLocations());
		message.setQos(Preferences.getPubQosLocations());
		message.setRetained(Preferences.getPubRetainLocations());

		publishMessage(message);
	}

	@Override
	public void processMessage(MessageTransition message) {
		message.setTopic(Preferences.getPubTopicEvents());
		message.setQos(Preferences.getPubQosEvents());
		message.setRetained(Preferences.getPubRetainEvents());

		publishMessage(message);
	}

	@Override
	public void processMessage(MessageWaypoint message) {
		message.setTopic(Preferences.getPubTopicWaypoints());
		message.setQos(Preferences.getPubQosWaypoints());
		message.setRetained(Preferences.getPubRetainWaypoints());

		publishMessage(message);
	}

	@Override
	public void processMessage(MessageWaypoints message) {
		message.setTopic(Preferences.getPubTopicWaypoints());
		message.setQos(Preferences.getPubQosWaypoints());
		message.setRetained(Preferences.getPubRetainWaypoints());

		publishMessage(message);
	}

boolean firstStart = true;
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
			this.mqttClient.publish(message.getTopic(), m);

			// At this point, delivery is technically not completed. However, if the mqttClient didn't throw any errors, the message likely makes it to the broker
			// We don't save any references between delivery tokens and messages to singal completion when deliveryComplete is called
			this.messageSender.onMessageDelivered(message);

			if(this.mqttClient.getPendingDeliveryTokens().length >= MAX_INFLIGHT_MESSAGES) {
				Log.v(TAG, "pausing pubPool due to back preassure. Outstanding tokens: " + this.mqttClient.getPendingDeliveryTokens().length);
				this.pubPool.pause();
			}
		} catch (MqttException e) {
			Log.e(TAG, "processMessage: MqttException. " + e.getCause() + " " + e.getReasonCode() + " " + e.getMessage());
			e.printStackTrace();
		} catch (Parser.EncryptionException e) {
			e.printStackTrace();
		} catch (IOException e) {
			Log.e(TAG, "processMessage: JsonProcessingException");
			e.printStackTrace();
		}

	}




	private static ServiceMessage.EndpointState state = ServiceMessage.EndpointState.INITIAL;

	private CustomMqttClient mqttClient;
	private Thread workerThread;
	private static Exception error;
    private MqttClientPersistence persistenceStore;
	private BroadcastReceiver netConnReceiver;
    private WakeLock networkWakelock;
    private WakeLock connectionWakelock;
    private ReconnectHandler reconnectHandler;
	private PingHandler pingHandler;
	private boolean connectedWithCleanSession;

	private MqttConnectOptions lastConnectionOptions;
	private String lastConnectionId = null;





	@Override
	public void onCreate(ServiceProxy p) {
		Log.v(TAG, "loaded MQTT backend");
		this.context = p;
		this.workerThread = null;
		initPausedPubPool();
        this.persistenceStore = new CustomMemoryPersistence();
        this.reconnectHandler = new ReconnectHandler(context);
        changeState(EndpointState.INITIAL);
        doStart();


		// Testing
		/*MessageLocation m = new MessageLocation();
		m.setLat(52.4251861);
		m.setLon(9.7330908);
		m.setAcc(100);
		m.setTst(TimeUnit.MILLISECONDS.toSeconds(System.currentTimeMillis()));
		m.setT("m");
		m.setTopic("owntracks/binarybucks/e");
		m.setTid("e");

		ServiceProxy.getServiceParser().processMessage(m);*/
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
        if (intent == null)
			return;

		if(ServiceMessageMqtt.RECEIVER_ACTION_RECONNECT.equals(intent.getAction()) && !isConnected()) {
			Log.v(TAG,	 "onStartCommand ServiceMessageMqtt.RECEIVER_ACTION_RECONNECT");
			if(reconnectHandler != null)
				doStart();

		} else if(ServiceMessageMqtt.RECEIVER_ACTION_PING.equals(intent.getAction())) {
			Log.v(TAG,	 "onStartCommand ServiceMessageMqtt.RECEIVER_ACTION_PING");

			if(pingHandler != null)
				pingHandler.ping(intent);
			else
				doStart();
		}
        return;
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

		Log.v(TAG, "handleStart: force == " + force);
        if(!Preferences.canConnect()) {
			changeState(EndpointState.DISCONNECTED_CONFIGINCOMPLETE);
			return;
        }

		// Respect user's wish to stay disconnected. Overwrite with force = true
		// to reconnect manually afterwards
		if ((state == EndpointState.DISCONNECTED_USERDISCONNECT) && !force) {
			return;
		}

		if (isConnecting()) {
			Log.d(TAG, "handleStart: isConnecting == true");
			return;
		}

		// Check if there is a data connection. If not, try again in some time.
		if (!isOnline()) {
			Log.e(TAG, "handleStart: isBackgroundDataEnabled == false");
			changeState(EndpointState.DISCONNECTED_DATADISABLED);
			reconnectHandler.start(); // we will try again to connect after some time
			return;
		}

		if (isDisconnected()) {
				Log.v(TAG, "handleStart: isOnline() == true");

				if (connect())
					onConnect();
				else
					reconnectHandler.start();

		} else {
			Log.d(TAG, "handleStart: isDisconnected() == false");
		}
	}

	private boolean isDisconnected() {

		return (state == EndpointState.INITIAL)
				|| (state == EndpointState.DISCONNECTED)
				|| (state == EndpointState.DISCONNECTED_USERDISCONNECT)
				|| (state == EndpointState.DISCONNECTED_DATADISABLED)
				|| (state == EndpointState.DISCONNECTED_ERROR)

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
			this.mqttClient.setCallback(this);

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

			this.mqttClient.connect(lastConnectionOptions);
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
        } catch(JSONException e) {}

	}

	private String getConnectionId() {
		return mqttClient.getCurrentServerURI()+"/"+lastConnectionOptions.getUserName();
	}

	private void onConnect() {

		// Check if we're connecting to the same broker that we were already connected to
		String connectionId = getConnectionId();
		if(lastConnectionId != null && !connectionId.equals(lastConnectionId)) {
			EventBus.getDefault().post(new Events.BrokerChanged());
			lastConnectionId = connectionId;
			Log.v(TAG, "lastConnectionId changed to: " + lastConnectionId);
		}

		reconnectHandler.stop();

		// Establish observer to monitor wifi and radio connectivity
		if (this.netConnReceiver == null) {
			this.netConnReceiver = new NetworkConnectionIntentReceiver();
			this.context.registerReceiver(this.netConnReceiver, new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION));
		}

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

		if(subTopicBase.endsWith("#")) { // wildcard sub will match everything anyway
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


    private void subscribe(String topic)  {
        subscribe(new String[]{topic});
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

			this.mqttClient.subscribe(topics);

		} catch (Exception e) {
			e.printStackTrace();
		}
    }


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

	public void disconnect(boolean fromUser) {
		Log.v(TAG, "disconnect. from user: " + fromUser);

		if (isConnecting()) {
            return;
        }

		try {
			if (this.netConnReceiver != null) {
				this.context.unregisterReceiver(this.netConnReceiver);
				this.netConnReceiver = null;
			}

		} catch (Exception eee) {
			Log.e(TAG, "Unregistering netConnReceiver failed", eee);
		}

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

	@Override
	public void connectionLost(Throwable t) {
		Log.e(TAG, "connectionLost: " + t.toString());
        t.printStackTrace();
		pubPool.pause();
		// we protect against the phone switching off while we're doing this
		// by requesting a wake lock - we request the minimum possible wake
		// lock - just enough to keep the CPU running until we've finished



        if(connectionWakelock == null )
            connectionWakelock = ((PowerManager) this.context.getSystemService(Context.POWER_SERVICE)).newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, ServiceProxy.WAKELOCK_TAG_BROKER_CONNECTIONLOST);

        if (!connectionWakelock.isHeld())
            connectionWakelock.acquire();


        if (!isOnline()) {
			changeState(EndpointState.DISCONNECTED_DATADISABLED);
        } else {
			changeState(EndpointState.DISCONNECTED);
        }

        reconnectHandler.start();

        if(connectionWakelock.isHeld())
            connectionWakelock.release();
	}



	public void reconnect() {
		disconnect(false);
		doStart(true);
	}

	private void changeState(Exception e) {
		error = e;
		changeState(EndpointState.DISCONNECTED_ERROR, e);
	}

	private void changeState(EndpointState newState) {
		changeState(newState, null);
	}

	private void changeState(EndpointState newState, Exception e) {
		//Log.d(TAG, "ServiceMessageMqtt state changed to: " + newState);
		state = newState;
		EventBus.getDefault().postSticky(new Events.EndpointStateChanged(newState, e));
	}

	private boolean isOnline() {
		ConnectivityManager cm = (ConnectivityManager) this.context.getSystemService(Context.CONNECTIVITY_SERVICE);
		NetworkInfo netInfo = cm.getActiveNetworkInfo();

        if(netInfo != null && netInfo.isAvailable() && netInfo.isConnected()) {
            return true;
        } else {
            Log.e(TAG, "isONline == true. activeNetworkInfo: "+ (netInfo != null) +", available=" + (netInfo != null && netInfo.isAvailable()) + ", connected: " + (netInfo != null && netInfo.isConnected()));
            return false;
        }
	}

	public boolean isConnected() {
		return this.mqttClient != null && this.mqttClient.isConnected(  );
	}

	public static boolean isErrorState(EndpointState state) {
		return state == EndpointState.DISCONNECTED_ERROR;
	}

	public static boolean hasError() {
		return error != null;
	}

	public boolean isConnecting() {
		return (this.mqttClient != null)
				&& (state == EndpointState.CONNECTING);
	}

	@Override
	public void onDestroy() {
		// disconnect immediately
		disconnect(false);

		changeState(EndpointState.DISCONNECTED);
	}

	public static EndpointState getState() {
		return state;
	}

	public static String getErrorMessage() {
		if (hasError() && (error.getCause() != null))
			return "Error: " + error.getCause().getLocalizedMessage();
		else
			return "Error: " + ServiceProxy.getInstance().getString(R.string.na);

	}

	@Override
	public String getStateAsString() {
        int id;
        switch (getState()) {
            case CONNECTED:
                id = R.string.connectivityConnected;
                break;
            case CONNECTING:
                id = R.string.connectivityConnecting;
                break;
            case DISCONNECTING:
                id = R.string.connectivityDisconnecting;
                break;
            case DISCONNECTED_USERDISCONNECT:
                id = R.string.connectivityDisconnectedUserDisconnect;
                break;
            case DISCONNECTED_DATADISABLED:
                id = R.string.connectivityDisconnectedDataDisabled;
                break;
            case DISCONNECTED_ERROR:
                id = R.string.error;
                break;
			case DISCONNECTED_CONFIGINCOMPLETE:
				id = R.string.connectivityDisconnectedConfigIncomplete;
				break;
            default:
                id = R.string.connectivityDisconnected;

        }
        return context.getString(id);
    }





    public Exception getError() {
        return error;
    }



	@Override
	public void messageArrived(String topic, MqttMessage message) throws Exception {

		try {
			MessageBase m = Parser.deserializeSync(message.getPayload());
			if(!m.isValidMessage()) {
				Log.e(TAG, "message failed validation: " + message.getPayload());
				return;
			}

			m.setTopic(getBaseTopic(m, topic));
			m.setRetained(message.isRetained());
			m.setQos(message.getQos());
			this.messageReceiver.onMessageReceived(m);
			if(m instanceof MessageUnknown) {
				Log.v(TAG, "unknown message topic: " + topic +" payload: " + new String(message.getPayload()));
			}

		} catch (Exception e) {

			Log.e(TAG, "JSON parser exception for message: " + new String(message.getPayload()));
			Log.e(TAG, e.getMessage() +" " + e.getCause());

			e.printStackTrace();
		}
	}


	private String getBaseTopic(MessageBase message, String topic){

		if (message.getBaseTopicSuffix() != null && topic.endsWith(message.getBaseTopicSuffix())) {
			return topic.substring(0, (topic.length() - message.getBaseTopicSuffix().length()));
		} else {
			return topic;
		}
	}



	@Override
	public void deliveryComplete(IMqttDeliveryToken messageToken) {
		StatisticsProvider.setInt(StatisticsProvider.SERVICE_BROKER_QUEUE_LENGTH, pubPool.getQueueLength());

		if(this.pubPool.isPaused() && this.mqttClient.getPendingDeliveryTokens().length <= MAX_INFLIGHT_MESSAGES) {
			Log.v(TAG, "resuming pubPool that was paused due to backPreassure. Currently outstanding tokens: " + this.mqttClient.getPendingDeliveryTokens().length);
			this.pubPool.resume();
		}
    }

	private class NetworkConnectionIntentReceiver extends BroadcastReceiver {
		private static final String TAG = "NetworkConnectionIntent";

		@Override
		public void onReceive(Context ctx, Intent intent) {
			Log.v(TAG, "onReceive");
            if(networkWakelock == null )
                networkWakelock = ((PowerManager) ServiceMessageMqtt.this.context.getSystemService(Context.POWER_SERVICE)).newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, ServiceProxy.WAKELOCK_TAG_BROKER_NETWORK);

            if (!networkWakelock.isHeld())
                networkWakelock.acquire();

			//if (isOnline() && !isConnected() && !isConnecting()) {
			if (!isConnected() && !isConnecting()) {

			//Log.v(TAG, "NetworkConnectionIntentReceiver: triggering doStart");
                doStart();
            }

            if(networkWakelock.isHeld())
                networkWakelock.release();
        }
	}



	public void onEvent(Events.Dummy e) {
	}


	public void clearQueues() {
		initPausedPubPool();
		StatisticsProvider.setInt(StatisticsProvider.SERVICE_BROKER_QUEUE_LENGTH, 0);
    }

	@SuppressWarnings("unused")
	public void onEvent(Events.ModeChanged e) {
		Log.v(TAG, "ModeChanged. Disconnecting and draining message queue");
        disconnect(false);
        clearQueues();
    }

	@SuppressWarnings("unused")
	public void onEvent(Events.BrokerChanged e) {
        clearQueues();
    }



    // Custom blocking MqttClient that allows to specify a MqttPingSender
    private static final class CustomMqttClient extends MqttClient {
        public CustomMqttClient(String serverURI, String clientId, MqttClientPersistence persistence, MqttPingSender pingSender) throws MqttException {
            super(serverURI, clientId, persistence);// Have to call do the AsyncClient init twice as there is no other way to setup a client with a ping sender (thanks Paho)
            aClient = new MqttAsyncClient(serverURI, clientId, persistence, pingSender);
        }
    }

    class PingHandler implements MqttPingSender {
        static final String TAG = "PingHandler";

        private ClientComms comms;
        private Context context;
		private WakeLock wakelock;

        public void ping(Intent intent) {
			Log.v(TAG, "sending");

			if (wakelock == null) {
				PowerManager pm = (PowerManager) context.getSystemService(ServiceProxy.POWER_SERVICE);
				wakelock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, ServiceProxy.WAKELOCK_TAG_BROKER_PING);
			}

			if(!wakelock.isHeld())
				wakelock.acquire();

			if(comms == null) {
				doStart();
				return;
			}


			IMqttToken token = comms.checkForActivity(new IMqttActionListener() {

				@Override
				public void onSuccess(IMqttToken asyncActionToken) {
					Log.d(TAG, "Success. Release lock(" + ServiceProxy.WAKELOCK_TAG_BROKER_PING + "):" + System.currentTimeMillis());
					if(wakelock != null && wakelock.isHeld()){
						wakelock.release();
					}
				}

				@Override
				public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
					Log.d(TAG, "Failure. Release lock(" + ServiceProxy.WAKELOCK_TAG_BROKER_PING + "):" + System.currentTimeMillis());

					//Release wakelock when it is done.
					if(wakelock != null && wakelock.isHeld()){
						wakelock.release();
					}
				}
			});

			if (token == null) {
				wakelock.release();
			}
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
            alarmManager.cancel(ServiceProxy.getBroadcastIntentForService(context, ServiceProxy.SERVICE_MESSAGE_MQTT, ServiceMessageMqtt.RECEIVER_ACTION_PING, null));
        }

		// Schedules a BroadcastIntent that will trigger a ping message when received.
		// It will be received by ServiceMessageMqtt.onStartCommand which recreates the service in case it has been stopped
		// onStartCommand will then deliver the intent to the ping(...) method if the service was alive or it will trigger a new connection attempt
        @Override
        public void schedule(long delayInMilliseconds) {


            AlarmManager alarmManager = (AlarmManager) context.getSystemService(ServiceProxy.ALARM_SERVICE);
			PendingIntent p = ServiceProxy.getBroadcastIntentForService(context, ServiceProxy.SERVICE_MESSAGE_MQTT, ServiceMessageMqtt.RECEIVER_ACTION_PING, null);
			if (Build.VERSION.SDK_INT >= 19) {
				alarmManager.setExact(AlarmManager.RTC_WAKEUP, System.currentTimeMillis() + delayInMilliseconds, p);
			} else {
				alarmManager.set(AlarmManager.RTC_WAKEUP, System.currentTimeMillis() + delayInMilliseconds, p);
			}

        }
    }

    class ReconnectHandler {
		private static final String TAG = "ReconnectHandler";
		private static final int BACKOFF_INTERVAL_MAX = 6; // Will try to reconnect after 1, 2, 4, 8, 16, 32, 64 minutes
		private int backoff = 0;

		private final Context context;
        private boolean hasStarted;


        public ReconnectHandler(Context context) {
            this.context = context;
        }

        public void start() {
            if(hasStarted)
                return;

            Log.v(TAG, "start");

			schedule();
            hasStarted = true;
        }

        public void stop() {
            Log.v(TAG, "stoping reocnnect handler");
			backoff = 0;
            AlarmManager alarmManager = (AlarmManager) context.getSystemService(ServiceProxy.ALARM_SERVICE);
            alarmManager.cancel(ServiceProxy.getBroadcastIntentForService(this.context, ServiceProxy.SERVICE_MESSAGE_MQTT, RECEIVER_ACTION_RECONNECT, null));

            if (hasStarted) {
                hasStarted = false;
            }
        }

        private void schedule() {
			Log.v(TAG, "scheduling reconnect handler");
			AlarmManager alarmManager = (AlarmManager) context.getSystemService(ServiceProxy.ALARM_SERVICE);
			long delayInMilliseconds = (long)Math.pow(2, backoff) * TimeUnit.MINUTES.toMillis(1);
			PendingIntent p = ServiceProxy.getBroadcastIntentForService(this.context, ServiceProxy.SERVICE_MESSAGE_MQTT, RECEIVER_ACTION_RECONNECT, null);
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

        private Integer getSize(){
            return data.size();
        }

        @Override
        public void close() throws MqttPersistenceException {

        }

        @Override
        public void put(String key, MqttPersistable persistable) throws MqttPersistenceException {
            Log.v(TAG, "put key " + key);

            data.put(key, persistable);
        }

        @Override
        public MqttPersistable get(String key) throws MqttPersistenceException {
            Log.v(TAG, "get key " + key);
            return (MqttPersistable)data.get(key);
        }

        @Override
        public void remove(String key) throws MqttPersistenceException {
            Log.v(TAG, "removing key " + key);
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
