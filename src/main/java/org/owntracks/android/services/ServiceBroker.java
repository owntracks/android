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
import org.owntracks.android.R;
import org.owntracks.android.messages.ConfigurationMessage;
import org.owntracks.android.messages.LocationMessage;
import org.owntracks.android.messages.Message;
import org.owntracks.android.support.Events;
import org.owntracks.android.support.MessageCallbacks;
import org.owntracks.android.support.Preferences;
import org.owntracks.android.support.StringifiedJSONObject;

import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.nio.charset.Charset;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.TrustManagerFactory;

import de.greenrobot.event.EventBus;

public class ServiceBroker implements MqttCallback, ProxyableService {


    public static enum State {
        INITIAL, CONNECTING, CONNECTED, DISCONNECTING, DISCONNECTED, DISCONNECTED_USERDISCONNECT, DISCONNECTED_DATADISABLED, DISCONNECTED_ERROR
    }


	private static State state = State.INITIAL;

	private CustomMqttClient mqttClient;
	private static ServiceBroker instance;
	private Thread workerThread;
	private LinkedList<Message> deferredPublishables;
	private Exception error;
	private HandlerThread pubThread;
	private Handler pubHandler;
    private MqttClientPersistence persistenceStore;
	private BroadcastReceiver netConnReceiver;
	private BroadcastReceiver pingSender;
	private ServiceProxy context;
    private LinkedList<String> subscribtions;
    private WakeLock networkWakelock;
    private WakeLock connectionWakelock;
    private String TAG_WAKELOG = "org.owntracks.android.wakelog.broker";

    private Map<IMqttDeliveryToken, Message> sendMessages = new HashMap<IMqttDeliveryToken, Message>();
    private List<Message> backlog = new LinkedList<Message>();

    @Override
	public void onCreate(ServiceProxy p) {
		this.context = p;
		this.workerThread = null;
		this.error = null;
        this.subscribtions = new LinkedList<String>();
		this.deferredPublishables = new LinkedList<Message>();
		this.pubThread = new HandlerThread("MQTTPUBTHREAD");
		this.pubThread.start();
		this.pubHandler = new Handler(this.pubThread.getLooper());
        this.persistenceStore = new CustomMemoryPersistence();
        changeState(State.INITIAL);
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
		//Log.v(this.toString(), "handleStart: force == " + force);
        if(!Preferences.canConnect()) {
            //Log.v(this.toString(), "handleStart: canConnect() == false");
            return;
        } else {
            //Log.v(this.toString(), "handleStart: canConnect() == true");
        }
		// Respect user's wish to stay disconnected. Overwrite with force = true
		// to reconnect manually afterwards
		if ((state == State.DISCONNECTED_USERDISCONNECT)
				&& !force) {
			//Log.d(this.toString(), "handleStart: userdisconnect==true");
			return;
		}

		if (isConnecting()) {
			//Log.d(this.toString(), "handleStart: isConnecting == true");
			return;
		}

		// Respect user's wish to not use data
		if (!isBackgroundDataEnabled()) {
			//Log.e(this.toString(), "handleStart: isBackgroundDataEnabled == false");
			changeState(State.DISCONNECTED_DATADISABLED);
			return;
		}

		// Don't do anything unless we're disconnected

		if (isDisconnected()) {
			//Log.v(this.toString(), "handleStart: isDisconnected() == true");
			// Check if there is a data connection
			if (isOnline()) {
				//Log.v(this.toString(), "handleStart: isOnline() == true");

				if (connect())
					onConnect();

			} else {
				//Log.e(this.toString(), "handleStart: isDisconnected() == false");
				changeState(State.DISCONNECTED_DATADISABLED);
			}
		} else {
			//Log.d(this.toString(), "handleStart: isDisconnected() == false");

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

	private void init() {
		if (this.mqttClient != null) {
			return;
		}

		try {
			String prefix = Preferences.getTls() == Preferences.getIntResource(R.integer.valTlsNone) ? "tcp" : "ssl";
			String cid = Preferences.getClientId(true);
            Log.v(this.toString(), "Using client id: " + cid);

            this.mqttClient = new CustomMqttClient(prefix + "://" + Preferences.getHost() + ":" + Preferences.getPort(), cid, persistenceStore, new AlarmPingSender(context));
			this.mqttClient.setCallback(this);

		} catch (MqttException e) {
			// something went wrong!
			this.mqttClient = null;
			changeState(State.DISCONNECTED);
		}
	}

	private static class CustomSocketFactory extends javax.net.ssl.SSLSocketFactory{
        private javax.net.ssl.SSLSocketFactory factory;

        public CustomSocketFactory(boolean sideloadCa) throws CertificateException, KeyStoreException, NoSuchAlgorithmException, IOException, KeyManagementException {
            Log.v(this.toString(), "initializing CustomSocketFactory");

            String tmfAlgorithm = TrustManagerFactory.getDefaultAlgorithm();
            TrustManagerFactory tmf = TrustManagerFactory.getInstance(tmfAlgorithm);


            if(sideloadCa) {
                Log.v(this.toString(), "CA sideload: true");

                KeyStore keyStore = KeyStore.getInstance(KeyStore.getDefaultType());
                keyStore.load(null, null);

                CertificateFactory cf = CertificateFactory.getInstance("X.509");
                InputStream caInput = new BufferedInputStream(new FileInputStream(Preferences.getTlsCrtPath()));
                Log.v(this.toString(), "Using custom tls cert from : " + Preferences.getTlsCrtPath());
                java.security.cert.Certificate ca;
                try {
                    ca = cf.generateCertificate(caInput);
                    keyStore.setCertificateEntry("owntracks-custom-tls-root", ca);

                } catch (Exception e) {
                    Log.e(this.toString(), e.toString());
                } finally {
                    caInput.close();
                }


                Log.v(this.toString(), "Keystore content: ");
                Enumeration<String> aliases = keyStore.aliases();

                for (; aliases.hasMoreElements();) {
                    String o = aliases.nextElement();
                    Log.v(this.toString(), "Alias: " + o);
                }

                tmf.init(keyStore);

            } else {
                Log.v(this.toString(), "CA sideload: false, using system keystore");
                // Use system KeyStore. This is some kind of magic.
                // On devices with hardware backed keystore, one does not get a an instance of the
                // system keystore when using KeyStore.getInstance("AndroidKeystore"). Instead,
                // an empty keystore is returned. However, when passing null to the tmf.init method
                // the system keystore is used
                tmf.init((KeyStore) null);

            }

            // Create an SSLContext that uses our TrustManager
            SSLContext context = SSLContext.getInstance("TLSv1.2");
            context.init(null, tmf.getTrustManagers(), null);
            this.factory= context.getSocketFactory();

        }

        @Override
        public String[] getDefaultCipherSuites() {
            return this.factory.getDefaultCipherSuites();
        }

        @Override
        public String[] getSupportedCipherSuites() {
            return this.factory.getSupportedCipherSuites();
        }

        @Override
        public Socket createSocket() throws IOException{
            SSLSocket r = (SSLSocket)this.factory.createSocket();
            r.setEnabledProtocols(new String[] {"SSLv3", "TLSv1", "TLSv1.1", "TLSv1.2"});
            return r;
        }

        @Override
        public Socket createSocket(Socket s, String host, int port, boolean autoClose) throws IOException {
            SSLSocket r = (SSLSocket)this.factory.createSocket(s, host, port, autoClose);
            r.setEnabledProtocols(new String[] {"SSLv3", "TLSv1", "TLSv1.1", "TLSv1.2"});
            return r;
        }

        @Override
        public Socket createSocket(String host, int port) throws IOException, UnknownHostException {

            SSLSocket r = (SSLSocket)this.factory.createSocket(host, port);
            r.setEnabledProtocols(new String[] {"SSLv3", "TLSv1", "TLSv1.1", "TLSv1.2"});
            return r;
        }

        @Override
        public Socket createSocket(String host, int port, InetAddress localHost, int localPort) throws IOException, UnknownHostException {
            SSLSocket r = (SSLSocket)this.factory.createSocket(host, port, localHost, localPort);
            r.setEnabledProtocols(new String[] {"SSLv3", "TLSv1", "TLSv1.1", "TLSv1.2"});
            return r;
        }

        @Override
        public Socket createSocket(InetAddress host, int port) throws IOException {
            SSLSocket r = (SSLSocket)this.factory.createSocket(host, port);
            r.setEnabledProtocols(new String[] {"SSLv3", "TLSv1", "TLSv1.1", "TLSv1.2"});
            return r;
        }

        @Override
        public Socket createSocket(InetAddress address, int port, InetAddress localAddress, int localPort) throws IOException {
            SSLSocket r = (SSLSocket)this.factory.createSocket(address, port, localAddress,localPort);
            r.setEnabledProtocols(new String[] {"SSLv3", "TLSv1", "TLSv1.1", "TLSv1.2"});
            return r;
        }
    }

	private boolean connect() {
		this.workerThread = Thread.currentThread(); // We connect, so we're the
													// worker thread
		this.error = null; // clear previous error on connect
		init();

		try {
			changeState(State.CONNECTING);
			MqttConnectOptions options = new MqttConnectOptions();
			setWill(options);

			if (Preferences.getAuth()) {
				options.setPassword(Preferences.getPassword().toCharArray());
				options.setUserName(Preferences.getUsername());
			}

			if (Preferences.getTls() != Preferences.getIntResource(R.integer.valTlsNone)) {
                options.setSocketFactory(new CustomSocketFactory(Preferences.getTls() == Preferences.getIntResource(R.integer.valTlsCustom)));
            }

			// setWill(options);
			options.setKeepAliveInterval(Preferences.getKeepalive());
			options.setConnectionTimeout(30);
			options.setCleanSession(Preferences.getCleanSession());

			this.mqttClient.connect(options);
			changeState(State.CONNECTED);

			return true;

		} catch (Exception e) { // Catch paho and socket factory exceptions
			Log.e(this.toString(), e.toString());
            e.printStackTrace();
			changeState(e);
			return false;
		}
	}

	private void setWill(MqttConnectOptions m) {
		StringBuilder payload = new StringBuilder();
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

		//if (!isConnected())
		//	Log.e(this.toString(), "onConnect: !isConnected");

		// Establish observer to monitor wifi and radio connectivity
		if (this.netConnReceiver == null) {
			this.netConnReceiver = new NetworkConnectionIntentReceiver();
			this.context.registerReceiver(this.netConnReceiver,
					new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION));
		}

		// Establish ping sender
		//if (this.pingSender == null) {
		//	this.pingSender = new PingSender();
		//	this.context.registerReceiver(this.pingSender, new IntentFilter(Defaults.INTENT_ACTION_PUBLISH_PING));
		//}

		//scheduleNextPing();
        resubscribe();
        deliverBacklog();
	}

    public void resubscribe(){
        //Log.v(this.toString(), "Resubscribing");

        try {
            unsubscribe();

            if (Preferences.getSub())
                subscribe(new String[]{Preferences.getSubTopic(true), Preferences.getBaseTopic()});
            else
                subscribe(new String[]{Preferences.getBaseTopic()});

        } catch (MqttException e) {
            e.printStackTrace();
        }
    }

    private void subscribe(String topic) throws MqttException {
        subscribe(new String[]{topic});
    }

    private void subscribe(String[] topics) throws MqttException{
        if(!isConnected())
            return;

        Log.v(this.toString(), "Subscribing to: " + topics);
        this.mqttClient.subscribe(topics);
        for (String topic : topics) {
            subscribtions.push(topic);
        }
    }
    private void unsubscribe() throws MqttException{
        if(!isConnected())
            return;

        mqttClient.unsubscribe(subscribtions.toArray(new String[subscribtions.size()]));
        subscribtions.clear();
    }

	public void disconnect(boolean fromUser) {
		//Log.v(this.toString(), "disconnect. from user: " + fromUser);

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
				//Log.v(this.toString(), "Disconnecting");
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
			changeState(State.DISCONNECTED_DATADISABLED);
        } else {
			changeState(State.DISCONNECTED);
			//scheduleNextPing();
        }

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
		//Log.d(this.toString(), "ServiceBroker state changed to: " + newState);
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
            Log.e(this.toString(), "isONline == true. activeNetworkInfo: "+ (netInfo != null) +", available=" + (netInfo != null && netInfo.isAvailable()) + ", connected: " + (netInfo != null && netInfo.isConnected()));
            return false;
        }
	}

	public boolean isConnected() {
		return this.mqttClient != null && this.mqttClient.isConnected(  );
	}

	public static boolean isErrorState(State state) {
		return state == State.DISCONNECTED_ERROR;
	}

	public boolean hasError() {
		return this.error != null;
	}

	public boolean isConnecting() {
		return (this.mqttClient != null)
				&& (state == State.CONNECTING);
	}

	private boolean isBackgroundDataEnabled() {
		return isOnline();
	}

	public static ServiceBroker getInstance() {
		return instance;
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
		Exception e = getInstance().error;

		if ((getInstance() != null) && getInstance().hasError()
				&& (e.getCause() != null))
			return "Error: " + e.getCause().getLocalizedMessage();
		else
			return "Error: " + getInstance().context.getString(R.string.na);

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

    public void publish(String message, String topic, int qos, boolean retained, MessageCallbacks callback, Object extra) {
        publish(new Message(topic, message, qos, retained, callback, extra));
    }

        public void publish(Message message, String topic, int qos, boolean retained, MessageCallbacks callback, Object extra){
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

                if (Looper.getMainLooper().getThread() == Thread.currentThread()) {
                    Log.e("ServiceBroker", "PUB ON MAIN THREAD");
                }

                if (mqttClient == null) {
                    Log.d("ServiceBroker", "no mqttClient initialized");
                    return;
                }

                // Handle TTL for message to discard it after message.ttl publish attempts
                if (message.decrementTTL() >= 1) {
                    if (!backlog.contains(message)) {
                        backlog.add(message);
                    }
                } else if (backlog.contains(message)) {
                    backlog.remove(message);
                }

                message.setPayload(message.toString().getBytes(Charset.forName("UTF-8")));

                try {
                    IMqttDeliveryToken t = ServiceBroker.this.mqttClient.getTopic(message.getTopic()).publish(message);
                    message.publishing();
                    sendMessages.put(t, message); // if we reach this point, the previous publish did not throw an exception and the message went out

                    Log.v(this.toString(), "queued message for delivery: " + t.getMessageId());
                } catch (MqttException e) {
                    message.publishFailed();
                    Log.e("ServiceBroker", e.getMessage());
                } finally {
                }
            }
        });

	}


    public Exception getError() {
        return error;
    }

    public Integer getDeferredPublishablesCound() {
        return deferredPublishables != null ? deferredPublishables.size() : -1;
    }



    private void deliverBacklog() {
        Iterator<Message> i = backlog.iterator();
        while (i.hasNext()) {
            Message m = i.next();
            publish(m);
        }
    }

	@Override
	public void messageArrived(String topic, MqttMessage message)
			throws Exception {
		//scheduleNextPing();

		String msg = new String(message.getPayload());
        Log.v(this.toString(), "Received message: " + topic + " : " + msg);

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
            LocationMessage lm = new LocationMessage(json);
            EventBus.getDefault().postSticky(new Events.LocationMessageReceived(lm, topic));
        } else if(type.equals("cmd") && topic.equals(Preferences.getBaseTopic())) {
            String action;
            try {
                action = json.getString("action");
            } catch (Exception e) {
                return;
            }

            switch (action) {
                case "dump":
                    if (!Preferences.getRemoteCommandDump()) {
                        Log.i(this.toString(), "Dump remote command is disabled");
                        return;
                    }
                    ServiceProxy.getServiceApplication().dump();
                    break;
                case "dumpLog":
                    if (!Preferences.getRemoteCommandDump()) {
                        Log.i(this.toString(), "Dump remote command is disabled");
                        return;
                    }
                    ServiceProxy.getServiceApplication().dumpLog();
                    break;
                case "reportLocation":
                    if (!Preferences.getRemoteCommandReportLocation()) {
                        Log.i(this.toString(), "ReportLocation remote command is disabled");
                        return;
                    }
                    ServiceProxy.getServiceLocator().publishResponseLocationMessage();

                    break;
                default:
                    Log.v(this.toString(), "Received cmd message with unsupported action (" + action + ")");
                    break;
            }

        } else if (type.equals("configuration") ) {
            // read configuration message and post event only if Remote Configuration is enabled
            if (!Preferences.getRemoteConfiguration()) {
                Log.i(this.toString(), "Remote Configuration is disabled");
                return;
            }
            ConfigurationMessage cm = new ConfigurationMessage(json);
            EventBus.getDefault().post(new Events.ConfigurationMessageReceived(cm, topic));

        } else {
			Log.d(this.toString(), "Ignoring message (" + type + ") received on topic " + topic);
		}
	}



    @Override
	public void deliveryComplete(IMqttDeliveryToken messageToken) {
        Log.v(this.toString(), "Delivery complete of " + messageToken.getMessageId());
        Message message = sendMessages.remove(messageToken);
        backlog.remove(message);
        message.publishSuccessful();
    }

	private class NetworkConnectionIntentReceiver extends BroadcastReceiver {

		@Override
		public void onReceive(Context ctx, Intent intent) {

            if(networkWakelock == null )
                networkWakelock = ((PowerManager) ServiceBroker.this.context.getSystemService(Context.POWER_SERVICE)).newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "Owntracks-ServiceBroker-ConnectionLost");

            if (!networkWakelock.isHeld())
                networkWakelock.acquire();

			if (isOnline() && !isConnected() && !isConnecting()) {
				//Log.v(this.toString(), "NetworkConnectionIntentReceiver: triggering doStart");
                doStart();
            }

            if(networkWakelock.isHeld())
                networkWakelock.release();
        }
	}



	public void onEvent(Events.Dummy e) {
	}



    // Custom blocking MqttClient that allows to specify a MqttPingSender
    private static final class CustomMqttClient extends MqttClient {
        public CustomMqttClient(String serverURI, String clientId, MqttClientPersistence persistence, MqttPingSender pingSender) throws MqttException {
            super(serverURI, clientId, persistence);// Have to call do the AsyncClient init twice as there is no other way to setup a client with a ping sender (thanks Paho)
            aClient = new MqttAsyncClient(serverURI, clientId, persistence, pingSender);
        }
    }

    class AlarmPingSender implements MqttPingSender {
        // Identifier for Intents, log messages, etc..
        static final String TAG = "AlarmPingSender";

        private ClientComms comms;
        private Context context;
        private BroadcastReceiver alarmReceiver;
        private AlarmPingSender that;
        private PendingIntent pendingIntent;
        private volatile boolean hasStarted = false;

        public AlarmPingSender(Context c ) {
            Log.v(this.toString(), "AlarmPingSender instantiated");

            if (c == null) {
                throw new IllegalArgumentException( "Neither service nor client can be null.");
            }
            this.context = c;
            that = this;
        }

        @Override
        public void init(ClientComms comms) {
            Log.v(this.toString(), "AlarmPingSender init");
            this.comms = comms;
            this.alarmReceiver = new AlarmReceiver();
        }

        @Override
        public void start() {
            Log.v(this.toString(), "AlarmPingSender start");
            context.registerReceiver(alarmReceiver, new IntentFilter(ServiceProxy.INTENT_ACTION_PUBLISH_PING));

            pendingIntent = PendingIntent.getBroadcast(context, 0, new Intent(ServiceProxy.INTENT_ACTION_PUBLISH_PING), PendingIntent.FLAG_UPDATE_CURRENT);

            schedule(comms.getKeepAlive());
            hasStarted = true;
        }

        @Override
        public void stop() {
            Log.v(this.toString(), "AlarmPingSender stop");

            // Cancel Alarm.
            AlarmManager alarmManager = (AlarmManager) context.getSystemService(ServiceProxy.ALARM_SERVICE);
            alarmManager.cancel(pendingIntent);

            Log.d(TAG, "Unregister alarmreceiver to MqttService"+comms.getClient().getClientId());
            if(hasStarted){
                hasStarted = false;
                try{
                    context.unregisterReceiver(alarmReceiver);
                }catch(IllegalArgumentException e){
                    //Ignore unregister errors.
                }
            }
        }

        @Override
        public void schedule(long delayInMilliseconds) {
            long nextAlarmInMilliseconds = System.currentTimeMillis()
                    + delayInMilliseconds;
            Log.d(TAG, "Schedule next alarm at " + nextAlarmInMilliseconds);
            AlarmManager alarmManager = (AlarmManager) context.getSystemService(ServiceProxy.ALARM_SERVICE);
            alarmManager.set(AlarmManager.RTC_WAKEUP, nextAlarmInMilliseconds, pendingIntent);
        }

        class AlarmReceiver extends BroadcastReceiver {
            private WakeLock wakelock;

            @Override
            public void onReceive(Context context, Intent intent) {
                // According to the docs, "Alarm Manager holds a CPU wake lock as
                // long as the alarm receiver's onReceive() method is executing.
                // This guarantees that the phone will not sleep until you have
                // finished handling the broadcast.", but this class still get
                // a wake lock to wait for ping finished.
                int count = intent.getIntExtra(Intent.EXTRA_ALARM_COUNT, -1);
                Log.d(TAG, "Ping " + count + " times.");

                Log.d(TAG, "Check time :" + System.currentTimeMillis());
                IMqttToken token = comms.checkForActivity();

                // No ping has been sent.
                if (token == null) {
                    return;
                }

                if (wakelock == null) {
                    PowerManager pm = (PowerManager) context.getSystemService(ServiceProxy.POWER_SERVICE);
                    wakelock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, TAG_WAKELOG);
                }
                wakelock.acquire();
                token.setActionCallback(new IMqttActionListener() {

                    @Override
                    public void onSuccess(IMqttToken asyncActionToken) {
                        Log.d(TAG, "Success. Release lock(" + TAG_WAKELOG + "):" + System.currentTimeMillis());
                        if(wakelock != null && wakelock.isHeld()){
                            wakelock.release();
                        }
                    }

                    @Override
                    public void onFailure(IMqttToken asyncActionToken,
                                          Throwable exception) {
                        Log.d(TAG, "Failure. Release lock(" + TAG_WAKELOG + "):"
                                + System.currentTimeMillis());
                        //Release wakelock when it is done.
                        if(wakelock != null && wakelock.isHeld()){
                            wakelock.release();
                        }
                    }
                });
            }
        }
    }

    private static final class CustomMemoryPersistence implements MqttClientPersistence {
        private static Hashtable data;

        public CustomMemoryPersistence(){

        }

        @Override
        public void open(String s, String s2) throws MqttPersistenceException {
            if(data == null) {
                this.data = new Hashtable();
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
            Log.v(this.toString(), "put key " + key);

            data.put(key, persistable);
        }

        @Override
        public MqttPersistable get(String key) throws MqttPersistenceException {
            Log.v(this.toString(), "get key " + key);
            return (MqttPersistable)data.get(key);
        }

        @Override
        public void remove(String key) throws MqttPersistenceException {
            Log.v(this.toString(), "removing key " + key);
            data.remove(key);
        }

        @Override
        public Enumeration keys() throws MqttPersistenceException {
            return data.keys();
        }

        @Override
        public void clear() throws MqttPersistenceException {
            Log.v(this.toString(), "clearing store");

            data.clear();
        }

        @Override
        public boolean containsKey(String key) throws MqttPersistenceException {
            return data.containsKey(key);
        }
    }

}
