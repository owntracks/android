package st.alr.mqttitude.services;

import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.util.Calendar;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.concurrent.TimeUnit;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;

import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.MqttPersistenceException;
import org.eclipse.paho.client.mqttv3.MqttTopic;
import org.eclipse.paho.client.mqttv3.internal.ClientComms;

import st.alr.mqttitude.R;
import st.alr.mqttitude.messages.LocationMessage;
import st.alr.mqttitude.support.Defaults;
import st.alr.mqttitude.support.Defaults.State;
import st.alr.mqttitude.support.Events;
import st.alr.mqttitude.support.ServiceMqttCallbacks;
import st.alr.mqttitude.support.Preferences;
import android.annotation.SuppressLint;
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
import de.greenrobot.event.EventBus;
import st.alr.mqttitude.support.StringifiedJSONObject;

public class ServiceBroker implements MqttCallback, ProxyableService {

	private static State.ServiceBroker state = State.ServiceBroker.INITIAL;

	private short keepAliveSeconds;
	private MqttClient mqttClient;
	private static ServiceBroker instance;
	private Thread workerThread;
	private LinkedList<DeferredPublishable> deferredPublishables;
	private Exception error;
	private HandlerThread pubThread;
	private Handler pubHandler;

	private BroadcastReceiver netConnReceiver;
	private BroadcastReceiver pingSender;
	private ServiceProxy context;
    private LinkedList<String> subscribtions;
    private WakeLock networkWakelock;
    private WakeLock connectionWakelock;

    @Override
	public void onCreate(ServiceProxy p) {
		this.context = p;
		this.workerThread = null;
		this.error = null;
        this.subscribtions = new LinkedList<String>();
		this.keepAliveSeconds = 15 * 60;
		this.deferredPublishables = new LinkedList<DeferredPublishable>();
		this.pubThread = new HandlerThread("MQTTPUBTHREAD");
		this.pubThread.start();
		this.pubHandler = new Handler(this.pubThread.getLooper());
        changeState(Defaults.State.ServiceBroker.INITIAL);
        doStart();
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		// doStart();
		return 0;
	}

	private void doStart() {
		doStart(false);
	}

	private void doStart(final boolean force) {


		Thread thread1 = new Thread() {
			@Override
			public void run() {
				handleStart(force);
				if (this == ServiceBroker.this.workerThread) // Clean up worker
																// thread
					ServiceBroker.this.workerThread = null;
			}

			@Override
			public void interrupt() {
				if (this == ServiceBroker.this.workerThread) // Clean up worker
																// thread
					ServiceBroker.this.workerThread = null;
				super.interrupt();
			}
		};
		thread1.start();
	}

	void handleStart(boolean force) {
		Log.v(this.toString(), "handleStart. force: " + force);
        if(!Preferences.canConnect()) {
            Log.v(this.toString(), "canConnect() == false. Prerequisites not met.");
            return;
        } else {
            Log.v(this.toString(), "Prerequisites to connect met");
        }
		// Respect user's wish to stay disconnected. Overwrite with force = true
		// to reconnect manually afterwards
		if ((state == Defaults.State.ServiceBroker.DISCONNECTED_USERDISCONNECT)
				&& !force) {
			Log.d(this.toString(), "handleStart: respecting user disconnect ");

			return;
		}

		if (isConnecting()) {
			Log.d(this.toString(), "handleStart: already connecting");
			return;
		}

		// Respect user's wish to not use data
		if (!isBackgroundDataEnabled()) {
			Log.e(this.toString(), "handleStart: !isBackgroundDataEnabled");
			changeState(Defaults.State.ServiceBroker.DISCONNECTED_DATADISABLED);
			return;
		}

		// Don't do anything unless we're disconnected

		if (isDisconnected()) {
			Log.v(this.toString(), "handleStart: !isConnected");
			// Check if there is a data connection
			if (isOnline()) {
				Log.v(this.toString(), "handleStart: isOnline");

				if (connect()) {
					Log.v(this.toString(), "handleStart: connect sucessfull");
					onConnect();
				}
			} else {
				Log.e(this.toString(), "handleStart: !isOnline");
				changeState(Defaults.State.ServiceBroker.DISCONNECTED_DATADISABLED);
			}
		} else {
			Log.d(this.toString(), "handleStart: isConnected");

		}
	}

	private boolean isDisconnected() {

		Log.v(this.toString(), "disconnect check: " + state
				+ ", mqttClient.isConnected() == " + isConnected());
		return (state == Defaults.State.ServiceBroker.INITIAL)
				|| (state == Defaults.State.ServiceBroker.DISCONNECTED)
				|| (state == Defaults.State.ServiceBroker.DISCONNECTED_USERDISCONNECT)
				|| (state == Defaults.State.ServiceBroker.DISCONNECTED_DATADISABLED)
				|| (state == Defaults.State.ServiceBroker.DISCONNECTED_ERROR)

				// In some cases the internal state may diverge from the mqtt
				// client state.
				|| !isConnected();
	}

	/**
	 * @category CONNECTION HANDLING
	 */
	private void init() {
		if (this.mqttClient != null) {
			return;
		}

		try {
			String prefix = Preferences.getTls() == Preferences.getIntResource(R.integer.valTlsNone) ? "tcp" : "ssl";
			String cid = Preferences.getDeviceId(true);

            //if(Preferences.getZeroLenghClientId())
            //    Log.v(this.toString(), "Using zero-lengh (MQTT v 3.1.1) client-id");
            //else


            this.mqttClient = new MqttClient(prefix + "://" + Preferences.getHost() + ":" + Preferences.getPort(), cid, null);
			this.mqttClient.setCallback(this);

		} catch (MqttException e) {
			// something went wrong!
			this.mqttClient = null;
			changeState(Defaults.State.ServiceBroker.DISCONNECTED);
		}
	}

	//
	private javax.net.ssl.SSLSocketFactory getSSLSocketFactory()
			throws CertificateException, KeyStoreException,
			NoSuchAlgorithmException, IOException, KeyManagementException {
		CertificateFactory cf = CertificateFactory.getInstance("X.509");
		InputStream caInput = new BufferedInputStream(new FileInputStream(
				Preferences.getTlsCrtPath()));
		java.security.cert.Certificate ca;
		try {
			ca = cf.generateCertificate(caInput);
		} finally {
			caInput.close();
		}

		// Create a KeyStore containing our trusted CAs
		String keyStoreType = KeyStore.getDefaultType();
		KeyStore keyStore = KeyStore.getInstance(keyStoreType);
		keyStore.load(null, null);
		keyStore.setCertificateEntry("ca", ca);

		// Create a TrustManager that trusts the CAs in our KeyStore
		String tmfAlgorithm = TrustManagerFactory.getDefaultAlgorithm();
		TrustManagerFactory tmf = TrustManagerFactory.getInstance(tmfAlgorithm);
		tmf.init(keyStore);

		// Create an SSLContext that uses our TrustManager
		SSLContext context = SSLContext.getInstance("TLS");
		context.init(null, tmf.getTrustManagers(), null);

		return context.getSocketFactory();
	}

	private boolean connect() {
		this.workerThread = Thread.currentThread(); // We connect, so we're the
													// worker thread
		Log.v(this.toString(), "connect");
		this.error = null; // clear previous error on connect
		init();

		try {
			changeState(Defaults.State.ServiceBroker.CONNECTING);
			MqttConnectOptions options = new MqttConnectOptions();
			setWill(options);

			if (Preferences.getAuth()) {
				options.setPassword(Preferences.getPassword().toCharArray());
				options.setUserName(Preferences.getUsername());
			}

			if (Preferences.getTls() == Preferences.getIntResource(R.integer.valTlsCustom))
				options.setSocketFactory(this.getSSLSocketFactory());

			// setWill(options);
			options.setKeepAliveInterval(this.keepAliveSeconds);
			options.setConnectionTimeout(10);
			options.setCleanSession(false);

			this.mqttClient.connect(options);

			Log.d(this.toString(), "No error during connect");
			changeState(Defaults.State.ServiceBroker.CONNECTED);

			return true;

		} catch (Exception e) { // Catch paho and socket factory exceptions
			Log.e(this.toString(), e.toString());
            e.printStackTrace();
			changeState(e);
			return false;
		}
	}

	private void setWill(MqttConnectOptions m) {
		StringBuffer payload = new StringBuffer();
		payload.append("{");
		payload.append("\"_type\": ").append("\"").append("lwt").append("\"");
		payload.append(", \"tst\": ")
				.append("\"")
				.append((int) (TimeUnit.MILLISECONDS.toSeconds(System
						.currentTimeMillis()))).append("\"");
		payload.append("}");

		m.setWill(this.mqttClient.getTopic(Preferences.getBaseTopic()),
				payload.toString().getBytes(), 0, false);

	}

	private void onConnect() {

		if (!isConnected())
			Log.e(this.toString(), "onConnect: !isConnected");

		// Establish observer to monitor wifi and radio connectivity
		if (this.netConnReceiver == null) {
			this.netConnReceiver = new NetworkConnectionIntentReceiver();
			this.context.registerReceiver(this.netConnReceiver,
					new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION));
		}

		// Establish ping sender
		if (this.pingSender == null) {
			this.pingSender = new PingSender();
			this.context.registerReceiver(this.pingSender, new IntentFilter(
					Defaults.INTENT_ACTION_PUBLISH_PING));
		}

		scheduleNextPing();
        resubscribe();


	}

    public void resubscribe(){
        Log.v(this.toString(), "Resubscribing");

        try {
            unsubscribe();

            subscribe(Preferences.getBaseTopic());

            if (Preferences.getSub())
                subscribe(Preferences.getSubTopic(true));


        } catch (MqttException e) {
            e.printStackTrace();
        }
    }

    private void subscribe(String topic) throws MqttException{
        if(!isConnected())
            return;

        this.mqttClient.subscribe(topic);
        subscribtions.push(topic);
    }
    private void unsubscribe() throws MqttException{
        if(!isConnected())
            return;

        mqttClient.unsubscribe(subscribtions.toArray(new String[subscribtions.size()]));
        subscribtions.clear();
    }

	public void disconnect(boolean fromUser) {
		Log.v(this.toString(), "disconnect. from user: " + fromUser);

		if (isConnecting()) // throws
							// MqttException.REASON_CODE_CONNECT_IN_PROGRESS
							// when disconnecting while connect is in progress.
			return;

		try {
			if (this.netConnReceiver != null) {
				this.context.unregisterReceiver(this.netConnReceiver);
				this.netConnReceiver = null;
			}

			if (this.pingSender != null) {
				this.context.unregisterReceiver(this.pingSender);
				this.pingSender = null;
			}
		} catch (Exception eee) {
			Log.e(this.toString(), "Unregister failed", eee);
		}

		try {
			if (isConnected()) {
				Log.v(this.toString(), "Disconnecting");
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
				changeState(Defaults.State.ServiceBroker.DISCONNECTED_USERDISCONNECT);
			else
				changeState(Defaults.State.ServiceBroker.DISCONNECTED);
		}
	}

	@Override
	public void connectionLost(Throwable t) {
		Log.e(this.toString(), "error: " + t.toString());
        t.printStackTrace();
		// we protect against the phone switching off while we're doing this
		// by requesting a wake lock - we request the minimum possible wake
		// lock - just enough to keep the CPU running until we've finished

        if(connectionWakelock == null )
            connectionWakelock = ((PowerManager) this.context.getSystemService(Context.POWER_SERVICE)).newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "Owntracks-ServiceBroker-ConnectionLost");

        if (!connectionWakelock.isHeld())
            connectionWakelock.acquire();


        if (!isOnline()) {
			changeState(Defaults.State.ServiceBroker.DISCONNECTED_DATADISABLED);
        } else {
			changeState(Defaults.State.ServiceBroker.DISCONNECTED);
			scheduleNextPing();
        }

        if(connectionWakelock.isHeld())
            connectionWakelock.release();
	}

	public void reconnect() {
		disconnect(false);
		doStart(true);
	}

	public void onEvent(Events.StateChanged.ServiceBroker event) {
		if (event.getState() == Defaults.State.ServiceBroker.CONNECTED)
			publishDeferrables();
	}

	private void changeState(Exception e) {
		this.error = e;
		changeState(Defaults.State.ServiceBroker.DISCONNECTED_ERROR, e);
	}

	private void changeState(Defaults.State.ServiceBroker newState) {
		changeState(newState, null);
	}

	private void changeState(Defaults.State.ServiceBroker newState, Exception e) {
		Log.d(this.toString(), "ServiceMqtt state changed to: " + newState);
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
            Log.e(this.toString(), "isONline == true. activeNetworkInfo: "+ (netInfo != null) +", available=" + (netInfo != null ? netInfo.isAvailable() : false) + ", connected: " + (netInfo != null ? netInfo.isConnected() : false));
            return false;
        }
	}

	public boolean isConnected() {
		return this.mqttClient != null && this.mqttClient.isConnected(  );
	}

	public static boolean isErrorState(Defaults.State.ServiceBroker state) {
		return state == Defaults.State.ServiceBroker.DISCONNECTED_ERROR;
	}

	public boolean hasError() {
		return this.error != null;
	}

	public boolean isConnecting() {
		return (this.mqttClient != null)
				&& (state == Defaults.State.ServiceBroker.CONNECTING);
	}

	private boolean isBackgroundDataEnabled() {
		return isOnline();
	}

	/**
	 * @category MISC
	 */
	public static ServiceBroker getInstance() {
		return instance;
	}

	@Override
	public void onDestroy() {
		// disconnect immediately
		disconnect(false);

		changeState(Defaults.State.ServiceBroker.DISCONNECTED);
	}

	public static Defaults.State.ServiceBroker getState() {
		return state;
	}

	public static String getErrorMessage() {
		Exception e = getInstance().error;

		if ((getInstance() != null) && getInstance().hasError()
				&& (e.getCause() != null))
			return "Error: " + e.getCause().getLocalizedMessage();
		else
			return "Error: " + getInstance().context.getString(R.string.na);

	}

	public static String getStateAsString(Context c) {
		return Defaults.State.toString(state, c);
	}

	public static String stateAsString(Defaults.State.ServiceLocator state,
			Context c) {
		return Defaults.State.toString(state, c);
	}

	private void deferPublish(final DeferredPublishable p) {
		p.wait(this.deferredPublishables, new Runnable() {

			@Override
			public void run() {
				if ((ServiceBroker.this.deferredPublishables != null)
						&& ServiceBroker.this.deferredPublishables.contains(p))
					ServiceBroker.this.deferredPublishables.remove(p);
				if (!p.isPublishing())// might happen that the publish is in
										// progress while the timeout occurs.
					p.publishFailed();
			}
		});
	}


	public void publish(String topic, String payload) {
		publish(topic, payload, false, 0, 0, null, null);
	}

	public void publish(String topic, String payload, boolean retained) {
		publish(topic, payload, retained, 0, 0, null, null);
	}

	public void publish(final String topic, final String payload,
			final boolean retained, final int qos, final int timeout,
			final ServiceMqttCallbacks callback, final Object extra) {

		publish(new DeferredPublishable(topic, payload, retained, qos, timeout,
				callback, extra));

	}

	private void publish(final DeferredPublishable p) {

		this.pubHandler.post(new Runnable() {

			@Override
			public void run() {

				if (Looper.getMainLooper().getThread() == Thread
						.currentThread()) {
					Log.e("ServiceBroker", "PUB ON MAIN THREAD");
				}

				if (!isOnline() || !isConnected()) {
					Log.d("ServiceBroker", "pub deferred");

					deferPublish(p);
					doStart();
					return;
				}

				try {
					p.publishing();
					ServiceBroker.this.mqttClient.getTopic(p.getTopic())
							.publish(p);
					p.publishSuccessfull();
				} catch (MqttException e) {
					Log.e("ServiceBroker", e.getMessage());
					e.printStackTrace();
					p.cancelWait();
					p.publishFailed();
				}
			}
		});

	}

	private void publishDeferrables() {
		for (Iterator<DeferredPublishable> iter = this.deferredPublishables
				.iterator(); iter.hasNext();) {
			DeferredPublishable p = iter.next();
			iter.remove();
			publish(p);
		}
	}

    public short getKeepaliveSeconds() {
        return keepAliveSeconds;
    }

    public Exception getError() {
        return error;
    }

    public Integer getDeferredPublishablesCound() {
        return deferredPublishables != null ? deferredPublishables.size() : -1;
    }

    private class DeferredPublishable extends MqttMessage {
		private Handler timeoutHandler;
		private ServiceMqttCallbacks callback;
		private String topic;
		private int timeout = 0;
		private boolean isPublishing;
		private Object extra;

		public DeferredPublishable(String topic, String payload,
				boolean retained, int qos, int timeout, ServiceMqttCallbacks callback,
				Object extra) {

			super(payload.getBytes());
			this.setQos(qos);
			this.setRetained(retained);
			this.extra = extra;
			this.callback = callback;
			this.topic = topic;
			this.timeout = timeout;
		}

		public void publishFailed() {
			if (this.callback != null)
				this.callback.publishFailed(this.extra);
		}

		public void publishSuccessfull() {
			if (this.callback != null)
				this.callback.publishSuccessfull(this.extra);
			cancelWait();

		}

		public void publishing() {
			this.isPublishing = true;
			if (this.callback != null)
				this.callback.publishing(this.extra);
		}

		public boolean isPublishing() {
			return this.isPublishing;
		}

		public String getTopic() {
			return this.topic;
		}

		public void cancelWait() {
			if (this.timeoutHandler != null)
				this.timeoutHandler.removeCallbacksAndMessages(this);
		}

		public void wait(LinkedList<DeferredPublishable> queue,
				Runnable onRemove) {
			if (this.timeoutHandler != null) {
				Log.d(this.toString(),
						"This DeferredPublishable already has a timeout set");
				return;
			}

			// No need signal waiting for timeouts of 0. The command will be
			// failed right away
			if ((this.callback != null) && (this.timeout > 0))
				this.callback.publishWaiting(this.extra);

			queue.addLast(this);
			this.timeoutHandler = new Handler();
			this.timeoutHandler.postDelayed(onRemove, this.timeout * 1000);
		}
	}

	@Override
	public void messageArrived(String topic, MqttMessage message)
			throws Exception {
		scheduleNextPing();
		Log.v(this.toString(), "Received message: " + topic + " : "
				+ message.getPayload());

		String msg = new String(message.getPayload());
		String type;
		StringifiedJSONObject json;

		try {
			json = new StringifiedJSONObject(msg);
			type = json.getString("_type");
		} catch (Exception e) {
			Log.e(this.toString(), "Received invalid message: " + msg);
			return;
		}

		if (type.equals("location")) {
            // GeocodableLocation l = GeocodableLocation.fromJsonObject(json);
            LocationMessage lm = LocationMessage.fromJsonObject(json);
            EventBus.getDefault().postSticky(new Events.LocationMessageReceived(lm, topic));
        } else if(type.equals("cmd") && topic.equals(Preferences.getBaseTopic())) {
            String action = "";
            try {
                action = json.getString("action");
            } catch (Exception e) {
                Log.v(this.toString(), "Received cmd message without action");
                return;
            }

            if (action.equals("dump")) {
                Log.v(this.toString(), "Received dump cmd message");
                if(!Preferences.getRemoteCommandDump()) {
                    Log.i(this.toString(), "Command is disabled");
                    return;
                }
                ServiceProxy.getServiceApplication().dump();
            } else if (action.equals("reportLocation")){
                Log.v(this.toString(), "Received reportLocation cmd message");

                if(!Preferences.getRemoteCommandReportLocation()) {
                    Log.i(this.toString(), "Command is disabled");
                    return;
                }
                ServiceProxy.getServiceLocator().publishLocationMessage();

            } else {
                Log.v(this.toString(), "Received cmd message with unsupported action (" + action + ")");
            }

		} else {
			Log.d(this.toString(), "Ignoring message (" + type + ") received on topic " + topic);
			return;
		}
	}

	@Override
	public void deliveryComplete(IMqttDeliveryToken token) {
	}

	private class NetworkConnectionIntentReceiver extends BroadcastReceiver {

		@Override
		public void onReceive(Context ctx, Intent intent) {
			Log.v(this.toString(), "NetworkConnectionIntentReceiver, onReceive");

            if(networkWakelock == null )
                networkWakelock = ((PowerManager) ServiceBroker.this.context.getSystemService(Context.POWER_SERVICE)).newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "Owntracks-ServiceBroker-ConnectionLost");

            if (!networkWakelock.isHeld())
                networkWakelock.acquire();

			if (isOnline() && !isConnected() && !isConnecting()) {
				Log.v(this.toString(), "NetworkConnectionIntentReceiver: triggering doStart");
                doStart();
            }

            if(networkWakelock.isHeld())
                networkWakelock.release();
        }
	}

	public class PingSender extends BroadcastReceiver {
		@Override
		public void onReceive(Context context, Intent intent) {

			if (isOnline() && !isConnected() && !isConnecting()) {
				Log.v(this.toString(), "ping: isOnline()=" + isOnline()
						+ ", isConnected()=" + isConnected());
				doStart();
			} else if (!isOnline()) {
				Log.d(this.toString(),
						"ping: Waiting for network to come online again");
			} else {
				try {
					ping();
				} catch (MqttException e) {
					// if something goes wrong, it should result in
					// connectionLost
					// being called, so we will handle it there
					Log.e(this.toString(), "ping failed - MQTT exception", e);

					// assume the client connection is broken - trash it
					try {
						ServiceBroker.this.mqttClient.disconnect();
					} catch (MqttPersistenceException e1) {
						Log.e(this.toString(),
								"disconnect failed - persistence exception", e1);
					} catch (MqttException e2) {
						Log.e(this.toString(),
								"disconnect failed - mqtt exception", e2);
					}

					// reconnect
					Log.w(this.toString(), "onReceive: MqttException=" + e);
					doStart();
				}
			}
			scheduleNextPing();
		}
	}

	private void scheduleNextPing() {
		PendingIntent pendingIntent = PendingIntent.getBroadcast(this.context,
				0, new Intent(Defaults.INTENT_ACTION_PUBLISH_PING),
				PendingIntent.FLAG_UPDATE_CURRENT);

		Calendar wakeUpTime = Calendar.getInstance();
		wakeUpTime.add(Calendar.SECOND, this.keepAliveSeconds);

		AlarmManager aMgr = (AlarmManager) this.context
				.getSystemService(Context.ALARM_SERVICE);
		aMgr.set(AlarmManager.RTC_WAKEUP, wakeUpTime.getTimeInMillis(),
				pendingIntent);
	}

	private void ping() throws MqttException {
		MqttTopic topic = this.mqttClient.getTopic("$SYS/keepalive");

		MqttMessage message = new MqttMessage();
		message.setRetained(false);
		message.setQos(1);
		message.setPayload(new byte[] { 0 });

		try {
            Log.v(this.toString(), "Sending PING");
			topic.publish(message);
		} catch (org.eclipse.paho.client.mqttv3.MqttPersistenceException e) {
			e.printStackTrace();
		} catch (org.eclipse.paho.client.mqttv3.MqttException e) {
			throw new MqttException(e);
		}
	}

	public void onEvent(Events.Dummy e) {
	}

}
