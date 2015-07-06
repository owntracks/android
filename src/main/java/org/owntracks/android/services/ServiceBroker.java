package org.owntracks.android.services;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.util.Log;

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
import org.owntracks.android.messages.LocationMessage;
import org.owntracks.android.messages.Message;
import org.owntracks.android.support.Events;
import org.owntracks.android.support.MessageLifecycleCallbacks;
import org.owntracks.android.support.Preferences;
import org.owntracks.android.support.SocketFactory;
import org.owntracks.android.support.Statistics;

import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import ch.hsr.geohash.GeoHash;
import de.greenrobot.event.EventBus;

public class ServiceBroker implements MqttCallback, ProxyableService {
    public static final String RECEIVER_ACTION_RECONNECT = "org.owntracks.android.RECEIVER_ACTION_RECONNECT";
    public static final String RECEIVER_ACTION_PING = "org.owntracks.android.RECEIVER_ACTION_PING";
    private static final String TAG = "ServiceBroker";


    private static final int MAX_INFLIGHT_MESSAGES = 10;

    public enum State {
        INITIAL, CONNECTING, CONNECTED, DISCONNECTING, DISCONNECTED, DISCONNECTED_USERDISCONNECT, DISCONNECTED_DATADISABLED, DISCONNECTED_ERROR
    }


	private static State state = State.INITIAL;

	private CustomMqttClient mqttClient;
	private Thread workerThread;
	private static Exception error;
	private HandlerThread pubThread;
	private Handler pubHandler;
    private MqttClientPersistence persistenceStore;
	private BroadcastReceiver netConnReceiver;
	private ServiceProxy context;
    private LinkedList<String> subscribtions;
    private WakeLock networkWakelock;
    private WakeLock connectionWakelock;
    private ReconnectHandler reconnectHandler;
	private PingHandler pingHandler;
	private boolean connectedWithCleanSession;

    private  Map<IMqttDeliveryToken, Message> inflightMessages = new HashMap<>(); // keeps track of sent messages for delivery callbacks
    private final Object inflightMessagesLock = new Object();

    private LinkedList<Message> backlog = new LinkedList<>(); // keeps track of messages that failed to send
    private final Object backlogLock = new Object();

	private static final int MIN_GEOHASH_PRECISION = 3 ;
	private static final int MAX_GEOHASH_PRECISION = 6;

	private static final int GEOHASH_SLOTS = MAX_GEOHASH_PRECISION-MIN_GEOHASH_PRECISION+1;
	String[] activeGeohashTopics = new String[GEOHASH_SLOTS];
	private static final String[] NO_GEOHASHES = new String[GEOHASH_SLOTS];
	String pendingGeohash;

	@Override
	public void onCreate(ServiceProxy p) {
		this.context = p;
		this.workerThread = null;
		this.error = null;
        this.subscribtions = new LinkedList<>();
		this.pubThread = new HandlerThread("org.owntracks.android.services.ServiceBroker.pubThread");
		this.pubThread.start();
		this.pubHandler = new Handler(this.pubThread.getLooper());
        this.persistenceStore = new CustomMemoryPersistence();
        this.reconnectHandler = new ReconnectHandler(context);
        changeState(State.INITIAL);
        doStart();
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent == null)
			return 0;

		if(ServiceBroker.RECEIVER_ACTION_RECONNECT.equals(intent.getAction()) && !isConnected()) {
			Log.v(TAG,	 "onStartCommand ServiceBroker.RECEIVER_ACTION_RECONNECT");
			if(reconnectHandler != null)
				doStart();

		} else if(ServiceBroker.RECEIVER_ACTION_PING.equals(intent.getAction())) {
			if(pingHandler != null)
				pingHandler.ping(intent);
			else
				doStart();
		}
        return 0;
	}

	private void doStart() {
		doStart(false);
	}

	private void doStart(final boolean force) {
		Log.v(TAG, "doStart " + force);

		Thread thread1 = new Thread() {
			@Override
			public void run() {
				Log.v(TAG, "running thread");
				handleStart(force);
				if (this == ServiceBroker.this.workerThread) // Clean up worker
					ServiceBroker.this.workerThread = null;
			}

			@Override
			public void interrupt() {
				Log.v(TAG, "worker thread interrupt");
				if (this == ServiceBroker.this.workerThread) // Clean up worker
																// thread
					ServiceBroker.this.workerThread = null;
				super.interrupt();
			}
		};
		thread1.start();
	}

	void handleStart(boolean force) {

		Log.v(TAG, "handleStart: force == " + force);
        if(!Preferences.canConnect()) {
            //Log.v(TAG, "handleStart: canConnect() == false");
            return;
        }
		// Respect user's wish to stay disconnected. Overwrite with force = true
		// to reconnect manually afterwards
		if ((state == State.DISCONNECTED_USERDISCONNECT)
				&& !force) {
			//Log.d(TAG, "handleStart: userdisconnect==true");
			return;
		}

		if (isConnecting()) {
			Log.d(TAG, "handleStart: isConnecting == true");
			return;
		}

		// Respect user's wish to not use data
		if (!isBackgroundDataEnabled()) {
			Log.e(TAG, "handleStart: isBackgroundDataEnabled == false");
			changeState(State.DISCONNECTED_DATADISABLED);
			reconnectHandler.schedule(); // we will try again to connect after some time
			return;
		}

		// Don't do anything unless we're disconnected

		if (isDisconnected()) {
			//Log.v(TAG, "handleStart: isDisconnected() == true");
			// Check if there is a data connection
			if (isOnline()) {
				Log.v(TAG, "handleStart: isOnline() == true");

				if (connect())
					onConnect();

			} else {
				Log.e(TAG, "handleStart: isDisconnected() == false");
				changeState(State.DISCONNECTED_DATADISABLED);
				reconnectHandler.schedule(); // we will try again to connect after some time
			}
		} else {
			Log.d(TAG, "handleStart: isDisconnected() == false");

		}
	}

	private boolean isDisconnected() {

		return (state == State.INITIAL)
				|| (state == State.DISCONNECTED)
				|| (state == State.DISCONNECTED_USERDISCONNECT)
				|| (state == State.DISCONNECTED_DATADISABLED)
				|| (state == State.DISCONNECTED_ERROR)

				// In some cases the internal state may diverge from the mqtt
				// client state.
				|| !isConnected();
	}

	private boolean init() {
		if (this.mqttClient != null) {
			return true;
		}

		try {
			String prefix = Preferences.getTls() ? "ssl" : "tcp";
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
        changeState(State.CONNECTING);
		Log.v(TAG, "connect() mode: " + Preferences.getModeId());

		error = null; // clear previous error on connect
		if(!init()) {
            return false;
        }

		try {
			MqttConnectOptions options = new MqttConnectOptions();

            if(Preferences.isModeHosted()) {
                options.setPassword(Preferences.getPassword().toCharArray());
                options.setUserName(String.format("%s|%s", Preferences.getUsername(), Preferences.getDeviceId(false)));
                options.setSocketFactory(new SocketFactory(false, false));
            } else {
                if (Preferences.getAuth()) {
                    options.setPassword(Preferences.getPassword().toCharArray());
                    options.setUserName(Preferences.getUsername());
                }

                if (Preferences.getTls()) {
                    options.setSocketFactory(new SocketFactory(Preferences.getTlsCaCrtPath().length() > 0, Preferences.getTlsClientCrtPath().length() > 0));
                }
            }

            setWill(options);
			options.setKeepAliveInterval(Preferences.getKeepalive());
			options.setConnectionTimeout(30);
			connectedWithCleanSession = Preferences.getCleanSession();
			options.setCleanSession(connectedWithCleanSession);

			this.mqttClient.connect(options);
			changeState(State.CONNECTED);

			activeGeohashTopics = NO_GEOHASHES;
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

            m.setWill(Preferences.getDeviceTopic(true), lwt.toString().getBytes(), 0, false);
        } catch(JSONException e) {}

	}

	private void onConnect() {
		Statistics.incrementCounter(context, Statistics.SERVICE_BROKER_CONNECTS);


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
	}


	private void onCleanSessionConnect() {
		Log.v(TAG, "onCleanSessionConnect()");
	}

	private void onUncleanSessionConnect() {
		Log.v(TAG, "onUncleanSessionConnect()");

		activeGeohashTopics  = hashToSubPrecisionTopics(Preferences.getAndClearPersistentGeohash());
	}

	private void onSessionConnect() {
		subscribToInitialTopics();

		if (Preferences.getLocationBasedServicesEnabled() && pendingGeohash != null) {
				updateGeohashSubscriptions();
		}

		deliverBacklog();
	}

	public void onEvent(Events.CurrentLocationUpdated e) {
		Log.v(TAG, "onEvent() - CurrentLocationUpdated ");

		if(Preferences.getLocationBasedServicesEnabled()) {
			pendingGeohash = GeoHash.geoHashStringWithCharacterPrecision(e.getGeocodableLocation().getLatitude(), e.getGeocodableLocation().getLongitude(), MAX_GEOHASH_PRECISION);
			if(isConnected())
				updateGeohashSubscriptions();
		}
	}

	private String[] hashToSubPrecisionTopics(String maxPrecision) {
		if("".equals(maxPrecision))
			return NO_GEOHASHES;

		String[] r = new String[GEOHASH_SLOTS];
		String format = Preferences.getGeohashMsgTopic();
		for(int i = MIN_GEOHASH_PRECISION; i <= MAX_GEOHASH_PRECISION; i++) {
			int j = i - MIN_GEOHASH_PRECISION;
			r[j]= String.format(format, maxPrecision.substring(0, i-1));
		}
		return r;
	}

	public void updateGeohashSubscriptions() {
		Log.v(TAG, "updateGeohashSubscriptions() with hash " + pendingGeohash);
		String[] newGeohashTopics = hashToSubPrecisionTopics(pendingGeohash);

		if(activeGeohashTopics[0] == null) {
			activeGeohashTopics = newGeohashTopics;
			subscribe(activeGeohashTopics);
			handleGeohashForUnCleanSession();
			return;
		}

		int newGeohashIndex = -1;
		int activeGeohashIndex;
		for(int i = MIN_GEOHASH_PRECISION; i <= MAX_GEOHASH_PRECISION; i++) {
			activeGeohashIndex = i - MIN_GEOHASH_PRECISION; // index in geohash topic storage
			Log.v(TAG, "updateGeohashSubscriptions() - comparing activeGeohashIndex: " + activeGeohashIndex+ ". New: " + newGeohashTopics[activeGeohashIndex] + ", active: " + activeGeohashTopics[activeGeohashIndex]);
			if(!newGeohashTopics[activeGeohashIndex].equals(activeGeohashTopics[activeGeohashIndex])) {
				newGeohashIndex = activeGeohashIndex;
				Log.v(TAG, "updateGeohashSubscriptions()  - compare difference at index " + newGeohashIndex);
				break;
			}
		}
		Log.v(TAG, "updateGeohashSubscriptions() - newGeohashIndex == " + newGeohashIndex);


		if(newGeohashIndex >= 0) {
			Log.v(TAG, "updateGeohashSubscriptions() - newGeohashIndex > 0");
			unsubscribe(Arrays.copyOfRange(activeGeohashTopics, newGeohashIndex, activeGeohashTopics.length - 1)); // unsubscribe from match to end
			subscribe(Arrays.copyOfRange(newGeohashTopics, newGeohashIndex, newGeohashTopics.length - 1));
			activeGeohashTopics = newGeohashTopics;
			handleGeohashForUnCleanSession();
		}
	}

	private void handleGeohashForUnCleanSession() {
		if(!connectedWithCleanSession)
			Preferences.setPersistentGeohash(pendingGeohash);
		else
			Preferences.getAndClearPersistentGeohash();
	}


    public void resubscribe(){
		//unsubscribe();
		//subscribToInitialTopics();
    }


	public void subscribToInitialTopics() {
		List<String> topics =new ArrayList<String>();
		String baseTopic = Preferences.getBaseTopic();

		if(baseTopic.endsWith("#")) { // wildcard sub will match everything anyway
			topics.add(baseTopic);
		} else {

			topics.add(baseTopic);
			topics.add(baseTopic + "/info");

			if (Preferences.getRemoteConfiguration() && !Preferences.isModePublic())
				topics.add(Preferences.getDeviceTopic(true) + "/cmd");

			if (Preferences.getDirectMessageEnable() && !Preferences.isModePublic())
				topics.add(baseTopic + "/msg");

			if (!Preferences.isModePublic()) {
				topics.add(baseTopic + "/event");
				topics.add(baseTopic + "/waypoint");
			}


		}

		if(Preferences.getBroadcastMessageEnabled())
			topics.add(Preferences.getBroadcastMessageTopic());


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
			for (String topic : topics) {
				subscribtions.push(topic);
			}
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
				changeState(State.DISCONNECTED_USERDISCONNECT);
			else
				changeState(State.DISCONNECTED);
		}
	}

	@Override
	public void connectionLost(Throwable t) {
		Log.e(TAG, "connectionLost: " + t.toString());
        t.printStackTrace();
		// we protect against the phone switching off while we're doing this
		// by requesting a wake lock - we request the minimum possible wake
		// lock - just enough to keep the CPU running until we've finished



        if(connectionWakelock == null )
            connectionWakelock = ((PowerManager) this.context.getSystemService(Context.POWER_SERVICE)).newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, ServiceProxy.WAKELOCK_TAG_BROKER_CONNECTIONLOST);

        if (!connectionWakelock.isHeld())
            connectionWakelock.acquire();


        if (!isOnline()) {
			changeState(State.DISCONNECTED_DATADISABLED);
        } else {
			changeState(State.DISCONNECTED);
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
		this.error = e;
		changeState(State.DISCONNECTED_ERROR, e);
	}

	private void changeState(State newState) {
		changeState(newState, null);
	}

	private void changeState(State newState, Exception e) {
		//Log.d(TAG, "ServiceBroker state changed to: " + newState);
		state = newState;
		EventBus.getDefault().postSticky(
                new Events.StateChanged.ServiceBroker(newState, e));
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

	public static boolean isErrorState(State state) {
		return state == State.DISCONNECTED_ERROR;
	}

	public static boolean hasError() {
		return error != null;
	}

	public boolean isConnecting() {
		return (this.mqttClient != null)
				&& (state == State.CONNECTING);
	}

	private boolean isBackgroundDataEnabled() {
		return isOnline();
	}


	@Override
	public void onDestroy() {
		// disconnect immediately
		disconnect(false);

		changeState(State.DISCONNECTED);
	}

	public static State getState() {
		return state;
	}

	public static String getErrorMessage() {
		if (hasError() && (error.getCause() != null))
			return "Error: " + error.getCause().getLocalizedMessage();
		else
			return "Error: " + ServiceProxy.getInstance().getString(R.string.na);

	}

	public static String getStateAsString(Context c)
    {
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
            default:
                id = R.string.connectivityDisconnected;

        }
        return c.getString(id);
    }

    public void publish(String message, String topic, int qos, boolean retained, MessageLifecycleCallbacks callback, Object extra) {
        publish(new Message(topic, message, qos, retained, callback, extra));
    }

    public void publish(Message message, String topic, int qos, boolean retained, MessageLifecycleCallbacks callback, Object extra){
        message.setCallback(callback);
        message.setExtra(extra);
        publish(message, topic, qos, retained);
    }

    public void publish(Message message, String topic, int qos, boolean retained){
        message.setTopic(topic);
        message.setRetained(retained);
        message.setQos(qos);
        publish(message);
    }

    public void publish(final Message message) {
		this.pubHandler.post(new Runnable() {

            @Override
            public void run() {
                Log.v(toString(), "Init publish of " + message + " to " + message.getTopic());
				if(message instanceof LocationMessage)
					Statistics.incrementCounter(context, Statistics.SERVICE_BROKER_LOCATION_PUBLISH_INIT);

				// This should never happen
                if (Looper.getMainLooper().getThread() == Thread.currentThread()) {
                    Log.e(TAG, "PUB ON MAIN THREAD");
                }

                // Check if we can publish
                //if (!isOnline() || !isConnected()) {
                //    Log.d("ServiceBroker", "publish deferred");
                //    doStart();
                //    return;
                //}

                message.setPayload(message.toString().getBytes(Charset.forName("UTF-8")));

                try {

                    if (message.getTopic() == null) {
                        throw new Exception("message without topic. class:" + message.getClass() + ", msg: " + message.toString());
                    }

                    //IMqttDeliveryToken t = ;
                    message.publishing();
                    synchronized (inflightMessagesLock) {
                        // either works if client is connected or throws Exception if not.
                        // If Client is initialized but not connected, it throws a paho exception and we have to remove the message in the catch
                        // if client is not initialized and NullpointerException is thrown
                        Log.v(TAG, "queueing message for delivery");
                        inflightMessages.put(ServiceBroker.this.mqttClient.getTopic(message.getTopic()).publish(message), message); // if we reach this point, the previous publish did not throw an exception and the message went out
                    }
                    Log.v(TAG, "queued message for delivery on thread: " +Thread.currentThread().getId());
                } catch (Exception e) {
                    synchronized (inflightMessagesLock) {
                        inflightMessages.remove(message);
                    }
                    // Handle TTL for message to discard it after message.ttl publish attempts or discard right away if message qos is 0
                    if (message.getQos() != 0 && message.decrementTTL() >= 1) {
                        Log.v(TAG, "failed qos 1|2 message added to backlog");
                        synchronized (backlogLock) {
                            backlog.add(message);
                        }
						if(message instanceof LocationMessage)
							Statistics.incrementCounter(context, Statistics.SERVICE_BROKER_LOCATION_PUBLISH_INIT_QOS12_QUEUE);

						Statistics.incrementCounter(context, Statistics.SERVICE_BROKER_QUEUE_LENGTH);

						message.publishQueued();
                    } else {
						if(message instanceof LocationMessage)
							Statistics.incrementCounter(context, Statistics.SERVICE_BROKER_LOCATION_PUBLISH_INIT_QOS0_DROP);

						Log.v(TAG, "failed qos 0 message dumped");

                        message.publishFailed();
                    }

                    Log.e(TAG, "cought delivery exception. backlog size is: " + backlog.size());
                    e.printStackTrace();
                } finally {
                }
            }
        });

	}


    public Exception getError() {
        return error;
    }


    // After reconnecting, we try to deliver messages for which the publish previously threw an error
    // Messages are removed from the backlock, a publish is attempted and if the publish the message is added at the end of the backlog again until ttl reaches zero
    private void deliverBacklog() {
        Log.v(TAG, "delivering backlog");
        Iterator<Message> i = backlog.iterator();
        while (i.hasNext() && mqttClient.getPendingDeliveryTokens().length <= MAX_INFLIGHT_MESSAGES) {
            Message m;
            synchronized (backlogLock) {
                m = i.next();
                i.remove();
            }

            publish(m);
        }
		Statistics.setInt(context, Statistics.SERVICE_BROKER_QUEUE_LENGTH, backlog.size());

	}

	@Override
	public void messageArrived(String topic, MqttMessage message) throws Exception {
        // Received messages are forwarded to ServiceApplication
        ServiceProxy.getServiceApplication().messageArrived(topic, message);
	}



    @Override
	public void deliveryComplete(IMqttDeliveryToken messageToken) {
        Log.v(TAG, "delivery complete thread " + Thread.currentThread().getId());

        Message message;
        synchronized (inflightMessagesLock) {
            message = inflightMessages.remove(messageToken);
            Log.v(TAG, "Delivery complete of " + messageToken.getMessageId());

            if(message != null) {
                Log.v(TAG, "Message received from inflight messages");
                message.publishSuccessful();
				if(message instanceof LocationMessage)
					Statistics.incrementCounter(context, Statistics.SERVICE_BROKER_LOCATION_PUBLISH_SUCCESS);

			}
        }
    }

	private class NetworkConnectionIntentReceiver extends BroadcastReceiver {
		private static final String TAG = "NetworkConnectionIntent";

		@Override
		public void onReceive(Context ctx, Intent intent) {
			Log.v(TAG, "onReceive");
            if(networkWakelock == null )
                networkWakelock = ((PowerManager) ServiceBroker.this.context.getSystemService(Context.POWER_SERVICE)).newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, ServiceProxy.WAKELOCK_TAG_BROKER_NETWORK);

            if (!networkWakelock.isHeld())
                networkWakelock.acquire();

			if (isOnline() && !isConnected() && !isConnecting()) {
				//Log.v(TAG, "NetworkConnectionIntentReceiver: triggering doStart");
                doStart();
            }

            if(networkWakelock.isHeld());
                networkWakelock.release();
        }
	}



	public void onEvent(Events.Dummy e) {
	}

    public void clearQueues() {
        synchronized (inflightMessagesLock) {
            inflightMessages.clear();
        }

        synchronized (backlogLock) {
            backlog.clear();
        }
    }

    public void onEvent(Events.ModeChanged e) {
        disconnect(false);
        clearQueues();
    }

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
			IMqttToken token = comms.checkForActivity();

			// No ping has been sent.
			if (token == null) {
				return;
			}

			if (wakelock == null) {
				PowerManager pm = (PowerManager) context.getSystemService(ServiceProxy.POWER_SERVICE);
				wakelock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, ServiceProxy.WAKELOCK_TAG_BROKER_PING);
			}

			if(!wakelock.isHeld())
				wakelock.acquire();

			token.setActionCallback(new IMqttActionListener() {

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
        }

        public PingHandler(Context c) {
            if (c == null) {
                throw new IllegalArgumentException( "Neither service nor client can be null.");
            }
            this.context = c;
        }

        @Override
        public void init(ClientComms comms) {
            this.comms = comms;
        }

        @Override
        public void start() {
            Log.v(TAG, "start");
            schedule(comms.getKeepAlive());
        }

        @Override
        public void stop() {
            Log.v(TAG, "stop");
            AlarmManager alarmManager = (AlarmManager) context.getSystemService(ServiceProxy.ALARM_SERVICE);
            alarmManager.cancel(ServiceProxy.getBroadcastIntentForService(context, ServiceProxy.SERVICE_BROKER, ServiceBroker.RECEIVER_ACTION_PING, null));
        }

		// Schedules a BroadcastIntent that will trigger a ping message when received.
		// It will be received by ServiceBroker.onStartCommand which recreates the service in case it has been stopped
		// onStartCommand will then deliver the intent to the ping(...) method if the service was alive or it will trigger a new connection attempt
        @Override
        public void schedule(long delayInMilliseconds) {
            AlarmManager alarmManager = (AlarmManager) context.getSystemService(ServiceProxy.ALARM_SERVICE);
			alarmManager.set(AlarmManager.RTC_WAKEUP, System.currentTimeMillis() + delayInMilliseconds, ServiceProxy.getBroadcastIntentForService(context, ServiceProxy.SERVICE_BROKER, ServiceBroker.RECEIVER_ACTION_PING, null));
        }
    }

    class ReconnectHandler {
		private static final String TAG = "ReconnectHandler";
		private static final int BACKOFF_INTERVAL_MAX = 64; // Will try to reconnect after 1, 2, 4, 8, 16, 32, 64 minutes
		private int backoff = 0;

		private Context context;
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
            Log.v(TAG, "stop");
			backoff = 0;
            AlarmManager alarmManager = (AlarmManager) context.getSystemService(ServiceProxy.ALARM_SERVICE);
            alarmManager.cancel(ServiceProxy.getBroadcastIntentForService(this.context, ServiceProxy.SERVICE_BROKER, RECEIVER_ACTION_RECONNECT, null));

            if (hasStarted) {
                hasStarted = false;
            }
        }

        private void schedule() {

            AlarmManager alarmManager = (AlarmManager) this.context.getSystemService(Context.ALARM_SERVICE);
			 long next = (long)Math.pow(2, backoff) * TimeUnit.MINUTES.toMillis(1);
			Log.v(TAG, "scheduling next reconnect in " +  next);
            alarmManager.set(AlarmManager.RTC_WAKEUP, System.currentTimeMillis() +  next, ServiceProxy.getBroadcastIntentForService(this.context, ServiceProxy.SERVICE_BROKER, RECEIVER_ACTION_RECONNECT, null));
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
            Log.v(TAG, "clearing store");

            data.clear();
        }

        @Override
        public boolean containsKey(String key) throws MqttPersistenceException {
            return data.containsKey(key);
        }
    }

}
