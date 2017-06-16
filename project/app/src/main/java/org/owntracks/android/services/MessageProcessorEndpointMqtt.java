package org.owntracks.android.services;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.util.Log;

import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttAsyncClient;
import org.eclipse.paho.client.mqttv3.MqttCallbackExtended;
import org.eclipse.paho.client.mqttv3.MqttClientPersistence;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.MqttPersistable;
import org.eclipse.paho.client.mqttv3.MqttPersistenceException;
import org.greenrobot.eventbus.Subscribe;
import org.json.JSONException;
import org.json.JSONObject;
import org.owntracks.android.App;
import org.owntracks.android.messages.MessageBase;
import org.owntracks.android.messages.MessageCard;
import org.owntracks.android.messages.MessageClear;
import org.owntracks.android.messages.MessageCmd;
import org.owntracks.android.messages.MessageEvent;
import org.owntracks.android.messages.MessageLocation;
import org.owntracks.android.messages.MessageTransition;
import org.owntracks.android.messages.MessageWaypoint;
import org.owntracks.android.messages.MessageWaypoints;
import org.owntracks.android.services.MessageProcessor.EndpointState;
import org.owntracks.android.support.Events;
import org.owntracks.android.support.interfaces.OutgoingMessageProcessor;
import org.owntracks.android.support.Preferences;
import org.owntracks.android.support.SocketFactory;
import org.owntracks.android.support.interfaces.StatefulServiceMessageProcessor;

import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.List;
import java.util.concurrent.TimeUnit;

import timber.log.Timber;

public class MessageProcessorEndpointMqtt implements OutgoingMessageProcessor, StatefulServiceMessageProcessor {
	private static final String TAG = "ServiceMessageMqtt";

	private static final String MQTT_BUNDLE_KEY_MESSAGE_PAYLOAD = "MQTT_BUNDLE_KEY_MESSAGE_PAYLOAD";
	private static final String MQTT_BUNDLE_KEY_MESSAGE_TOPIC = "MQTT_BUNDLE_KEY_MESSAGE_TOPIC";
	private static final String MQTT_BUNDLE_KEY_MESSAGE_RETAINED = "MQTT_BUNDLE_KEY_MESSAGE_RETAINED";
	private static final String MQTT_BUNDLE_KEY_MESSAGE_QOS = "MQTT_BUNDLE_KEY_MESSAGE_QOS";

	private CustomMqttClient mqttClient;
	private MqttConnectOptions connectOptions;
	private String lastConnectionId;
	private static EndpointState state;

	synchronized boolean sendPing() {
		// Connects if not connected or sends a ping message if aleady connected
		if(checkConnection() && mqttClient!=null) {
			mqttClient.ping();
			return true;
		} else {
			return false;
		}
	}

	synchronized boolean sendMessage(Bundle b) {
		long messageId = b.getLong(Scheduler.BUNDLE_KEY_MESSAGE_ID);
		if(!connect()) {
			Timber.e("not connected and connect failed");
			return false;
		}

		try {
			IMqttDeliveryToken pubToken = this.mqttClient.publish(b.getString(MQTT_BUNDLE_KEY_MESSAGE_TOPIC), mqttMessageFromBundle(b));
			pubToken.waitForCompletion(TimeUnit.SECONDS.toMillis(30));
			App.getMessageProcessor().onMessageDelivered(messageId);
			Timber.v("message sent: %s", b.getLong(Scheduler.BUNDLE_KEY_MESSAGE_ID));
			return true;
		} catch (MqttException e) {
			App.getMessageProcessor().onMessageDeliveryFailed(messageId);
			e.printStackTrace();
			return false;
		}
	}

	@SuppressWarnings("ConstantConditions")
	private MqttMessage mqttMessageFromBundle(Bundle b) {
		MqttMessage  m = new MqttMessage();
		m.setPayload(b.getByteArray(MQTT_BUNDLE_KEY_MESSAGE_PAYLOAD));
		m.setQos(b.getInt(MQTT_BUNDLE_KEY_MESSAGE_QOS));
		m.setRetained(b.getBoolean(MQTT_BUNDLE_KEY_MESSAGE_RETAINED));
		return m;
	}

	@NonNull
	private Bundle mqttMessageToBundle(MessageBase m)  {
		Bundle b = new Bundle();
		b.putLong(Scheduler.BUNDLE_KEY_MESSAGE_ID, m.getMessageId());
		b.putString(Scheduler.BUNDLE_KEY_ACTION, Scheduler.ONEOFF_TASK_SEND_MESSAGE_MQTT);

		try {
			// Message properties
			b.putByteArray(MQTT_BUNDLE_KEY_MESSAGE_PAYLOAD, App.getParser().toJsonBytes(m));
			b.putString(MQTT_BUNDLE_KEY_MESSAGE_TOPIC, m.getTopic());
			b.putInt(MQTT_BUNDLE_KEY_MESSAGE_QOS, m.getQos());
			b.putBoolean(MQTT_BUNDLE_KEY_MESSAGE_RETAINED, m.getRetained());
		} catch (Exception e) {
			// Message will not contain BUNDLE_KEY_ACTION and will be dropped by scheduler
			Timber.e(e, "JSON serialization failed for message %m. Message will be dropped" ,m.getMessageId());
			return b;
		}
		return b;
	}

	@NonNull
	private Bundle mqttMessageToBundle(@NonNull MessageClear m) {
		Bundle b = mqttMessageToBundle(MessageBase.class.cast(m));
		b.putByteArray(MQTT_BUNDLE_KEY_MESSAGE_PAYLOAD, new byte[0]);
		b.putBoolean(MQTT_BUNDLE_KEY_MESSAGE_RETAINED, true);
		return b;
	}



	private static MessageProcessorEndpointMqtt instance;
	public static MessageProcessorEndpointMqtt getInstance() {
		if(instance == null)
			instance = new MessageProcessorEndpointMqtt();
		return instance;
	}

	private MqttCallbackExtended iCallbackClient = new MqttCallbackExtended() {
		@Override
		public void connectComplete(boolean reconnect, String serverURI) {
			Timber.v("%s, serverUri:%s", reconnect, serverURI);
			onConnect();
		}

		@Override
		public void deliveryComplete(IMqttDeliveryToken token) {

		}

		@Override
		public void connectionLost(Throwable cause) {
			Timber.e(cause, "connectionLost error");
			App.getScheduler().cancelMqttPing();
            App.getScheduler().scheduleMqttReconnect();
			changeState(EndpointState.DISCONNECTED, new Exception(cause));
		}

		@Override
		public void messageArrived(String topic, MqttMessage message) throws Exception {
			try {
				MessageBase m = App.getParser().fromJson(message.getPayload());
				if (!m.isValidMessage()) {
					Timber.e("message failed validation");
					return;
				}

				m.setTopic(topic);
				m.setRetained(message.isRetained());
				m.setQos(message.getQos());
				App.getMessageProcessor().onMessageReceived(m);
			} catch (Exception e) {
				if (message.getPayload().length == 0) {
					Timber.v("clear message received: %s", topic);
					MessageClear m = new MessageClear();
					m.setTopic(topic.replace(MessageCard.BASETOPIC_SUFFIX, ""));
					App.getMessageProcessor().onMessageReceived(m);
				} else {
					Timber.e(e, "payload:%s ", new String(message.getPayload()));
				}
			}
		}
	};


	private boolean initClient() {
		if (this.mqttClient != null) {
			return true;
		}

		Timber.v("initializing new mqttClient");
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

			String cid = Preferences.getClientId();
            String connectString = prefix + "://" + Preferences.getHost() + ":" + Preferences.getPort();
			Timber.v("mode: " + App.getPreferences().getModeId());
			Timber.v("client id: " + cid);
			Timber.v("connect string: " + connectString);

			this.mqttClient = new CustomMqttClient(connectString, cid, new MqttClientMemoryPersistence());
			this.mqttClient.setCallback(iCallbackClient);
		} catch (Exception e) {
			Timber.e(e, "init failed");
			this.mqttClient = null;
			changeState(e);
            return false;
		}
        return true;
	}

	private boolean connect() {

		if(isConnected()) {
			Timber.v("already connected");
			changeState(getState()); // Background service might be restarted and not get the connection state
			return true;
		}

		if(isConnecting()) {
			Timber.v("already connecting");
			return false;
		}

		if(!Preferences.canConnect()) {
			changeState(EndpointState.ERROR_CONFIGURATION);
			return false;
		}

		// Check if there is a data connection.
		if (!isOnline()) {
			changeState(EndpointState.ERROR_DATADISABLED);
			return false;
		}

		Timber.v("connecting on thread %s",  Thread.currentThread().getId());

        changeState(EndpointState.CONNECTING);

		if(!initClient()) {
            return false;
        }

		try {
			Timber.v("setting up connect options");
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
			connectOptions.setCleanSession(Preferences.getCleanSession());

			Timber.v("connecting sync");
			this.mqttClient.connect(connectOptions).waitForCompletion();
			App.getScheduler().scheduleMqttPing(connectOptions.getKeepAliveInterval());
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
		App.getScheduler().cancelMqttReconnect();
		// Check if we're connecting to the same broker that we were already connected to
		String connectionId = getConnectionId();
		if(lastConnectionId != null && !connectionId.equals(lastConnectionId)) {
			App.getEventBus().post(new Events.BrokerChanged());
			lastConnectionId = connectionId;
			Log.v(TAG, "lastConnectionId changed to: " + lastConnectionId);
		}

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
			App.getScheduler().cancelMqttPing();
			App.getScheduler().cancelMqttReconnect();

		}
	}



	public void reconnect() {
		disconnect(false);
		connect();
	}

	@Override
	public void disconnect() {
		disconnect(true);
	}

	@Override
	public void onEnterForeground() {
		checkConnection();
	}

	boolean checkConnection() {
		if(isConnected()) {
			return true;
		} else {
			connect();
			return false;
		}
	}

	private void changeState(Exception e) {
		changeState(EndpointState.ERROR, e);
	}

	private void changeState(EndpointState newState) {
		state = newState;
		getMessageProcessor().onEndpointStateChanged(newState);
	}

	private void changeState(EndpointState newState, Exception e) {
		state = newState;
		getMessageProcessor().onEndpointStateChanged(newState.setError(e));
	}

	private MessageProcessor getMessageProcessor() {
		return App.getMessageProcessor();
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
		return this.mqttClient != null && this.mqttClient.isConnected();
	}

	private boolean isConnecting() {
		return (this.mqttClient != null) && (state == EndpointState.CONNECTING);
	}

	public static EndpointState getState() {
		return state;
	}


	@SuppressWarnings("UnusedParameters")
	@Subscribe
	public void onEvent(Events.BrokerChanged e) {

	}

	public void processOutgoingMessage(MessageBase message) {
		message.setTopic(Preferences.getPubTopicBase());
		scheduleMessage(mqttMessageToBundle(message));
	}

	@Override
	public void processOutgoingMessage(MessageCmd message) {
		message.setTopic(App.getPreferences().getPubTopicCommands());
		scheduleMessage(mqttMessageToBundle(message));
	}

	@Override
	public void processOutgoingMessage(MessageEvent message) {
		scheduleMessage(mqttMessageToBundle(message));
	}

	@Override
	public void processOutgoingMessage(MessageLocation message) {
		message.setTopic(Preferences.getPubTopicLocations());
		message.setQos(Preferences.getPubQosLocations());
		message.setRetained(Preferences.getPubRetainLocations());
		scheduleMessage(mqttMessageToBundle(message));
	}

	@Override
	public void processOutgoingMessage(MessageTransition message) {
		message.setTopic(Preferences.getPubTopicEvents());
		message.setQos(Preferences.getPubQosEvents());
		message.setRetained(Preferences.getPubRetainEvents());
		scheduleMessage(mqttMessageToBundle(message));
	}

	@Override
	public void processOutgoingMessage(MessageWaypoint message) {
		message.setTopic(Preferences.getPubTopicWaypoints());
		message.setQos(Preferences.getPubQosWaypoints());
		message.setRetained(Preferences.getPubRetainWaypoints());
		scheduleMessage(mqttMessageToBundle(message));
	}

	@Override
	public void processOutgoingMessage(MessageWaypoints message) {
		message.setTopic(Preferences.getPubTopicWaypoints());
		message.setQos(Preferences.getPubQosWaypoints());
		message.setRetained(Preferences.getPubRetainWaypoints());
		scheduleMessage(mqttMessageToBundle(message));
	}

	@Override
	public void processOutgoingMessage(MessageClear message) {
		message.setRetained(true);
		scheduleMessage(mqttMessageToBundle(message));

		message.setTopic(message.getTopic()+MessageCard.BASETOPIC_SUFFIX);
		scheduleMessage(mqttMessageToBundle(message));
	}

	@Override
	public void onDestroy() {
		disconnect(false);
	}

	@Override
	public void onCreateFromProcessor() {
		connect();
	}



	private void scheduleMessage(Bundle b) {
			if(App.isInForeground())
				sendMessage(b);
			else
				App.getScheduler().scheduleMessage(b);
	}


	private static final class MqttClientMemoryPersistence implements MqttClientPersistence {
		private static Hashtable<String, MqttPersistable> data;

		@Override
		public void open(String s, String s2) throws MqttPersistenceException {
			if(data == null) {
				data = new Hashtable<>();
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
			return data.get(key);
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

	private static final class CustomMqttClient extends MqttAsyncClient {

		CustomMqttClient(String serverURI, String clientId, MqttClientPersistence persistence) throws MqttException {
			super(serverURI, clientId, persistence);
		}

		void ping() {
			if(comms != null)
				comms.checkForActivity();
		}
	}
}

