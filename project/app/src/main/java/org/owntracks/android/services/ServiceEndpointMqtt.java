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
import android.os.Bundle;
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
import org.greenrobot.eventbus.Subscribe;
import org.json.JSONException;
import org.json.JSONObject;
import org.owntracks.android.App;
import org.owntracks.android.messages.MessageBase;
import org.owntracks.android.messages.MessageCard;
import org.owntracks.android.messages.MessageClear;
import org.owntracks.android.services.ServiceMessage.EndpointState;
import org.owntracks.android.support.Events;
import org.owntracks.android.support.Parser;
import org.owntracks.android.support.Preferences;
import org.owntracks.android.support.SocketFactory;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.List;
import java.util.concurrent.TimeUnit;

import timber.log.Timber;

public class ServiceEndpointMqtt {
	private static final String TAG = "ServiceMessageMqtt";

	public static final String MQTT_BUNDLE_KEY_MESSAGE_PAYLOAD = "MQTT_BUNDLE_KEY_MESSAGE_PAYLOAD";
	public static final String MQTT_BUNDLE_KEY_MESSAGE_TOPIC = "MQTT_BUNDLE_KEY_MESSAGE_TOPIC";
	public static final String MQTT_BUNDLE_KEY_MESSAGE_RETAINED = "MQTT_BUNDLE_KEY_MESSAGE_RETAINED";
	public static final String MQTT_BUNDLE_KEY_MESSAGE_QOS = "MQTT_BUNDLE_KEY_MESSAGE_QOS";

	private MqttAsyncClient mqttClient;
	private Object error;
	private MqttConnectOptions connectOptions;
	private boolean cleanSession;
	private String lastConnectionId;
	private static EndpointState state;

	boolean sendMessage(Bundle b) {
		if(!isConnected() && !connect())
			return false;

		try {
			IMqttDeliveryToken pubToken = this.mqttClient.publish(b.getString(MQTT_BUNDLE_KEY_MESSAGE_TOPIC), mqttMessageFromBundle(b));
			pubToken.waitForCompletion(TimeUnit.SECONDS.toMillis(30));
			return true;
		} catch (MqttException e) {
			e.printStackTrace();
			return false;
		}

	}

	private MqttMessage mqttMessageFromBundle(Bundle b) {
		MqttMessage  m = new MqttMessage();
		m.setPayload(b.getString(MQTT_BUNDLE_KEY_MESSAGE_PAYLOAD).getBytes());
		m.setQos(b.getInt(MQTT_BUNDLE_KEY_MESSAGE_QOS));
		m.setRetained(b.getBoolean(MQTT_BUNDLE_KEY_MESSAGE_RETAINED));
		return m;
	}

	public static Bundle mqttMessageToBundle(MessageBase m) throws IOException, Parser.EncryptionException {
		Bundle b = new Bundle();
		b.putString(MQTT_BUNDLE_KEY_MESSAGE_PAYLOAD, Parser.toJson(m));
		b.putString(MQTT_BUNDLE_KEY_MESSAGE_TOPIC, m.getTopic());
		b.putInt(MQTT_BUNDLE_KEY_MESSAGE_QOS, m.getQos());
		b.putBoolean(MQTT_BUNDLE_KEY_MESSAGE_RETAINED, m.getRetained());
		return b;
	}


	private static ServiceEndpointMqtt instance;
	public static ServiceEndpointMqtt getInstance() {
		if(instance == null)
			instance = new ServiceEndpointMqtt();
		return instance;
	}

	private MqttCallbackExtended iCallbackClient = new MqttCallbackExtended() {
		@Override
		public void connectComplete(boolean reconnect, String serverURI) {
			Timber.v("%s, serverUri:%s", reconnect, serverURI);
		}

		@Override
		public void deliveryComplete(IMqttDeliveryToken token) {

		}

		@Override
		public void connectionLost(Throwable cause) {
			Timber.e(cause, "connectionLost error");
			changeState(EndpointState.DISCONNECTED, new Exception(cause));
			// TODO: schedule reconnect through dispatcher
		}

		@Override
		public void messageArrived(String topic, MqttMessage message) throws Exception {
			if(message.getPayload().length > 0) {
				try {
					MessageBase m = Parser.fromJson(message.getPayload());
					if (!m.isValidMessage()) {
						Timber.e("message failed validation");
						return;
					}

					m.setTopic(topic);
					m.setRetained(message.isRetained());
					m.setQos(message.getQos());
					//TODO: send to repo
					//service.onMessageReceived(m);
				} catch (Exception e) {
					Timber.e(e, "payload:%s ", new String(message.getPayload()));

				}
			} else {
				MessageClear m = new MessageClear();
				m.setTopic(topic.replace(MessageCard.BASETOPIC_SUFFIX, ""));
				Timber.v("clear message received: %s", m.getTopic());
				//TODO: send to repo
				//service.onMessageReceived(m);
			}
		}

	};


	private void handleStart() {
		Log.v(TAG, "handleStart");
        if(!Preferences.canConnect()) {
			changeState(EndpointState.ERROR_CONFIGURATION);
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
			return;
		}

		if (isDisconnected()) {
				Log.v(TAG, "handleStart: isDisconnected:true");
				changeState(EndpointState.DISCONNECTED);

				if (connect())
					onConnect();
		} else {
			Log.d(TAG, "handleStart: isDisconnected() == false");
		}
	}

	private boolean isDisconnected() {
		return this.mqttClient == null || !this.mqttClient.isConnected();
	}

	private boolean init() {
		if (this.mqttClient != null) {
			return true;
		}

		try {
			String prefix = "tcp";
			if (Preferences.getTls()) {
				if (Preferences.getWs()) {
					prefix = "wss";
				} else
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

			this.mqttClient = new MqttAsyncClient(connectString, cid);
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
        changeState(EndpointState.CONNECTING);

		error = null; // clear previous error on connect
		if(!init()) {
            return false;
        }

		try {
			 connectOptions = new MqttConnectOptions();
			if (Preferences.getAuth()) {
				connectOptions.setPassword(Preferences.getPassword().toCharArray());
				connectOptions.setUserName(Preferences.getUsername());
			}

			connectOptions.setMqttVersion(Preferences.getMqttProtocolLevel());

			if (Preferences.getTls()) {
				String tlsCaCrt = Preferences.getTlsCaCrtName();
				String tlsClientCrt = Preferences.getTlsClientCrtName();

				SocketFactory.SocketFactoryOptions socketFactoryOptions = new SocketFactory.SocketFactoryOptions();

				if (tlsCaCrt.length() > 0) {
					try {
						socketFactoryOptions.withCaInputStream(App.getContext().openFileInput(tlsCaCrt));
					} catch (FileNotFoundException e) {
						e.printStackTrace();
					}
				}

				if (tlsClientCrt.length() > 0) {
					try {
						socketFactoryOptions.withClientP12InputStream(App.getContext().openFileInput(tlsClientCrt)).withClientP12Password(Preferences.getTlsClientCrtPassword());
					} catch (FileNotFoundException e1) {
						e1.printStackTrace();
					}
				}



				connectOptions.setSocketFactory(new SocketFactory(socketFactoryOptions));
			}


            setWill(connectOptions);
			connectOptions.setKeepAliveInterval(Preferences.getKeepalive());
			connectOptions.setConnectionTimeout(30);
			cleanSession = Preferences.getCleanSession();
			connectOptions.setCleanSession(cleanSession);

			this.mqttClient.connect(connectOptions).waitForCompletion();
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

            m.setWill(Preferences.getPubTopicBase(), lwt.toString().getBytes(), 0, false);
        } catch(JSONException ignored) {}

	}

	private String getConnectionId() {
		return mqttClient.getCurrentServerURI()+"/"+ connectOptions.getUserName();
	}

	private void onConnect() {

		// Check if we're connecting to the same broker that we were already connected to
		String connectionId = getConnectionId();
		if(lastConnectionId != null && !connectionId.equals(lastConnectionId)) {
			App.getEventBus().post(new Events.BrokerChanged());
			lastConnectionId = connectionId;
			Log.v(TAG, "lastConnectionId changed to: " + lastConnectionId);
		}

		// Establish observer to monitor wifi and radio connectivity
		if (cleanSession)
			onCleanSessionConnect();
		else
			onUncleanSessionConnect();

		onSessionConnect();
	}


	private void onCleanSessionConnect() {
	}

	private void onUncleanSessionConnect() {
	}

	private void onSessionConnect() {
		subscribToInitialTopics();
	}

	private void subscribToInitialTopics() {
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
				topics.add(Preferences.getPubTopicBase() + Preferences.getPubTopicCommandsPart());

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
		Arrays.fill(qos, Preferences.getSubQos());
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

		Timber.v("disconnect. user:%s", fromUser);
		if (isConnecting()) {
            return;
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

			if (fromUser)
				changeState(EndpointState.DISCONNECTED_USERDISCONNECT);
			else
				changeState(EndpointState.DISCONNECTED);
		}
	}



	public void reconnect() {
		disconnect(false);
		handleStart();
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
		//if(service != null)
		//	service.onEndpointStateChanged(newState, e);
	}

	private boolean isOnline() {
		ConnectivityManager cm = (ConnectivityManager) App.getContext().getSystemService(Context.CONNECTIVITY_SERVICE);
		NetworkInfo netInfo = cm.getActiveNetworkInfo();

        if(netInfo != null && netInfo.isAvailable() && netInfo.isConnected()) {
            return true;
        } else {
            Log.e(TAG, "isOnline == false. activeNetworkInfo: "+ (netInfo != null) +", available:" + (netInfo != null && netInfo.isAvailable()) + ", connected:" + (netInfo != null && netInfo.isConnected()));
            return false;
        }
	}

	private boolean isConnected() {
		return this.mqttClient != null && this.mqttClient.isConnected(  );
	}

	private boolean isConnecting() {
		return (this.mqttClient != null) && (state == EndpointState.CONNECTING);
	}

	public static EndpointState getState() {
		return state;
	}


	@Subscribe
	public void onEvent(Events.Dummy e) {
	}




	@Subscribe
	public void onEvent(Events.BrokerChanged e) {
//        clearQueues();
    }
}

