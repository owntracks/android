package org.owntracks.android.services;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Build;
import android.os.Looper;
import android.support.annotation.WorkerThread;

import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttAsyncClient;
import org.eclipse.paho.client.mqttv3.MqttCallbackExtended;
import org.eclipse.paho.client.mqttv3.MqttClientPersistence;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.MqttPersistable;
import org.greenrobot.eventbus.EventBus;
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
import org.owntracks.android.services.worker.Scheduler;
import org.owntracks.android.support.Events;
import org.owntracks.android.support.Parser;
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

public class MessageProcessorEndpointMqtt extends MessageProcessorEndpoint implements StatefulServiceMessageProcessor {
	public static final int MODE_ID = 0;

	private CustomMqttClient mqttClient;
	private MqttConnectOptions connectOptions;
	private String lastConnectionId;
	private static EndpointState state;

	protected MessageProcessor messageProcessor;
	protected Parser parser;
	protected Preferences preferences;
	protected Scheduler scheduler;
	protected EventBus eventBus;

	public MessageProcessorEndpointMqtt(MessageProcessor messageProcessor, Parser parser, Preferences preferences, Scheduler scheduler, EventBus eventBus) {
		super(messageProcessor);
		this.parser = parser;
		this.preferences = preferences;
		this.scheduler = scheduler;
		this.eventBus = eventBus;
		this.messageProcessor = messageProcessor;
	}

	public synchronized boolean sendKeepalive() {
		// Connects if not connected or sends a ping message if aleady connected
		if(checkConnection() && mqttClient!=null) {
			mqttClient.ping();
			return true;
		} else {
			return false;
		}
	}

	private synchronized void sendMessage(MessageBase m) {
		long messageId = m.getMessageId();
		sendMessageConnectPressure++;
		if (!connect()) {
			Timber.v("failed connection attempts :%s", sendMessageConnectPressure);
			messageProcessor.onMessageDeliveryFailed(messageId);
			return;
		}

		try {
			IMqttDeliveryToken pubToken = this.mqttClient.publish(m.getTopic(), parser.toJsonBytes(m), m.getQos(), m.getRetained());
			pubToken.waitForCompletion(TimeUnit.SECONDS.toMillis(30));

			Timber.v("message sent: %s", messageId);
			messageProcessor.onMessageDelivered(messageId);
		} catch (MqttException e) {
			e.printStackTrace();
			messageProcessor.onMessageDeliveryFailed(messageId);
		} catch (Exception e) {
			// Message will not contain BUNDLE_KEY_ACTION and will be dropped by scheduler
			Timber.e(e, "JSON serialization failed for message %m. Message will be dropped", m.getMessageId());
			messageProcessor.onMessageDeliveryFailedFinal(messageId);
		}
	}

	private final MqttCallbackExtended iCallbackClient = new MqttCallbackExtended() {
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
			scheduler.cancelMqttPing();
			scheduler.scheduleMqttReconnect();
			changeState(EndpointState.DISCONNECTED, new Exception(cause));
		}

		@Override
		public void messageArrived(String topic, MqttMessage message)  {
			try {
				MessageBase m = parser.fromJson(message.getPayload());
				if (!m.isValidMessage()) {
					Timber.e("message failed validation");
					return;
				}

				m.setTopic(topic);
				m.setRetained(message.isRetained());
				m.setQos(message.getQos());
				onMessageReceived(m);
			} catch (Exception e) {
				if (message.getPayload().length == 0) {
					Timber.v("clear message received: %s", topic);
					MessageClear m = new MessageClear();
					m.setTopic(topic.replace(MessageCard.BASETOPIC_SUFFIX, ""));
					onMessageReceived(m);
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
			if (preferences.getTls()) {
				if (preferences.getWs()) {
					prefix = "wss";
				} else
					prefix = "ssl";
			} else {
				if (preferences.getWs())
					prefix = "ws";
			}

			String cid = preferences.getClientId();
			String connectString = prefix + "://" + preferences.getHost() + ":" + preferences.getPort();
			Timber.v("mode: %s", preferences.getModeId());
			Timber.v("client id: %s", cid);
			Timber.v("connect string: %s", connectString);

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

	private int sendMessageConnectPressure = 0;

	@WorkerThread
	private synchronized boolean connect() {
		sendMessageConnectPressure++;
		boolean isUiThread = Build.VERSION.SDK_INT >= Build.VERSION_CODES.M ? Looper.getMainLooper().isCurrentThread()
				: Thread.currentThread() == Looper.getMainLooper().getThread();

		if(isUiThread) {
			try {
				throw new Exception("BLOCKING CONNECT ON MAIN THREAD");
			} catch (Exception e) {
				Timber.e(e);
				e.printStackTrace();
			}
		} else {
			Timber.e("Thread: %s", Thread.currentThread());
		}

		if(isConnected()) {
			Timber.v("already connected");
			changeState(getState()); // Background service might be restarted and not get the connection state
			return true;
		}

		if(isConnecting()) {
			Timber.v("already connecting");
			return false;
		}

		if(!isConfigurationComplete()) {
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
			if (preferences.getAuth()) {
				connectOptions.setPassword(preferences.getPassword().toCharArray());
				connectOptions.setUserName(preferences.getUsername());
			}

			connectOptions.setMqttVersion(preferences.getMqttProtocolLevel());

			if (preferences.getTls()) {
				String tlsCaCrt = preferences.getTlsCaCrtName();
				String tlsClientCrt = preferences.getTlsClientCrtName();

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
						socketFactoryOptions.withClientP12InputStream(App.getContext().openFileInput(tlsClientCrt)).withClientP12Password(preferences.getTlsClientCrtPassword());
					} catch (FileNotFoundException e1) {
						e1.printStackTrace();
					}
				}

				connectOptions.setSocketFactory(new SocketFactory(socketFactoryOptions));
			}

			setWill(connectOptions);
			connectOptions.setKeepAliveInterval(preferences.getKeepalive());
			connectOptions.setConnectionTimeout(30);
			connectOptions.setCleanSession(preferences.getCleanSession());

			Timber.v("connecting sync");
			this.mqttClient.connect(connectOptions).waitForCompletion();
			scheduler.scheduleMqttPing(connectOptions.getKeepAliveInterval());
			changeState(EndpointState.CONNECTED);

			sendMessageConnectPressure =0; // allow new connection attempts from sendMessage
			return true;

		} catch (Exception e) { // Catch paho and socket factory exceptions
			Timber.e(e);
			changeState(e);
			return false;
		}
	}

	private void setWill(MqttConnectOptions m) {
		try {
			JSONObject lwt = new JSONObject();
			lwt.put("_type", "lwt");
			lwt.put("tst", (int) TimeUnit.MILLISECONDS.toSeconds(System.currentTimeMillis()));

			m.setWill(preferences.getPubTopicBase(), lwt.toString().getBytes(), 0, false);
		} catch(JSONException ignored) {}

	}

	private String getConnectionId() {
		return mqttClient.getCurrentServerURI()+"/"+ connectOptions.getUserName();
	}

	private void onConnect() {
		scheduler.cancelMqttReconnect();
		// Check if we're connecting to the same broker that we were already connected to
		String connectionId = getConnectionId();
		if(lastConnectionId != null && !connectionId.equals(lastConnectionId)) {
			eventBus.post(new Events.EndpointChanged());
			lastConnectionId = connectionId;
			Timber.v("lastConnectionId changed to: %s", lastConnectionId);
		}

		List<String> topics = new ArrayList<>();
		String subTopicBase = preferences.getSubTopic();

		if(!preferences.getSub()) // Don't subscribe if base topic is invalid
			return;
		else if(subTopicBase.endsWith("#")) { // wildcard sub will match everything anyway
			topics.add(subTopicBase);
		} else {

			topics.add(subTopicBase);
			if(preferences.getInfo())
				topics.add(subTopicBase + preferences.getPubTopicInfoPart());

			topics.add(preferences.getPubTopicBase() + preferences.getPubTopicCommandsPart());
			topics.add(subTopicBase + preferences.getPubTopicEventsPart());
			topics.add(subTopicBase + preferences.getPubTopicWaypointsPart());


		}

		subscribe(topics.toArray(new String[0]));
	}



	private void subscribe(String[] topics) {
		if(!isConnected()) {
			Timber.e("subscribe when not connected");
			return;
		}
		for(String s : topics) {
			Timber.v( "subscribe() - Will subscribe to: %s", s);
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
		Arrays.fill(qos, preferences.getSubQos());
		return qos;
	}

	@SuppressWarnings("unused")
	private void unsubscribe(String[] topics) {
		if(!isConnected()) {
			Timber.e("subscribe when not connected");
			return;
		}

		for(String s : topics) {
			Timber.v("unsubscribe() - Will unsubscribe from: %s", s);
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
				Timber.v("Disconnecting");
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
			scheduler.cancelMqttPing();
			scheduler.cancelMqttReconnect();

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
	public boolean isConfigurationComplete() {
		return !preferences.getHost().trim().equals("") && !preferences.getUsername().trim().equals("") && (!preferences.getAuth() || !preferences.getPassword().trim().equals(""));
	}

	@WorkerThread
	public boolean checkConnection() {
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
		//Reduce unnecessary work caused by state updates to the same state
		if(state == newState)
			return;

		state = newState;
		messageProcessor.onEndpointStateChanged(newState);
	}

	private void changeState(EndpointState newState, Exception e) {
		state = newState;
		messageProcessor.onEndpointStateChanged(newState.setError(e));
	}

	private boolean isOnline() {
		ConnectivityManager cm = (ConnectivityManager) App.getContext().getSystemService(Context.CONNECTIVITY_SERVICE);
		if(cm == null)
			return false;
		NetworkInfo netInfo = cm.getActiveNetworkInfo();

		if(netInfo != null && netInfo.isAvailable() && netInfo.isConnected()) {
			return true;
		} else {
			Timber.e("isOnline == false. available:%s, connected:%s", netInfo != null && netInfo.isAvailable(), netInfo != null && netInfo.isConnected());
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
	public void onEvent(Events.EndpointChanged e) {
		reconnect();
	}

	public void processOutgoingMessage(MessageBase message) {
		message.setTopic(preferences.getPubTopicBase());
		sendMessage(message);
	}

	@Override
	public void processOutgoingMessage(MessageCmd message) {
		message.setTopic(preferences.getPubTopicCommands());
		sendMessage(message);
	}

	@Override
	public void processOutgoingMessage(MessageEvent message) {
		message.setTopic(preferences.getPubTopicEvents());
		sendMessage(message);
	}

	@Override
	public void processOutgoingMessage(MessageLocation message) {
		message.setTopic(preferences.getPubTopicLocations());
		message.setQos(preferences.getPubQosLocations());
		message.setRetained(preferences.getPubRetainLocations());
		sendMessage(message);
	}

	@Override
	public void processOutgoingMessage(MessageTransition message) {
		message.setTopic(preferences.getPubTopicEvents());
		message.setQos(preferences.getPubQosEvents());
		message.setRetained(preferences.getPubRetainEvents());
		sendMessage(message);
	}

	@Override
	public void processOutgoingMessage(MessageWaypoint message) {
		message.setTopic(preferences.getPubTopicWaypoints());
		message.setQos(preferences.getPubQosWaypoints());
		message.setRetained(preferences.getPubRetainWaypoints());
		sendMessage(message);
	}

	@Override
	public void processOutgoingMessage(MessageWaypoints message) {
		message.setTopic(preferences.getPubTopicWaypoints());
		message.setQos(preferences.getPubQosWaypoints());
		message.setRetained(preferences.getPubRetainWaypoints());
		sendMessage(message);
	}

	@Override
	public void processOutgoingMessage(MessageClear message) {
		message.setRetained(true);
		sendMessage(message);
		message.setTopic(message.getTopic()+MessageCard.BASETOPIC_SUFFIX);
		sendMessage(message);
	}

	@Override
	public void onDestroy() {
		disconnect(false);
		scheduler.cancelMqttTasks();
	}

	@Override
	public void onCreateFromProcessor() {
		scheduler.scheduleMqttReconnect();
	}

	private static final class MqttClientMemoryPersistence implements MqttClientPersistence {
		private static Hashtable<String, MqttPersistable> data;

		@Override
		public void open(String s, String s2)  {
			if(data == null) {
				data = new Hashtable<>();
			}
		}

		@SuppressWarnings("unused")
		private Integer getSize(){
			return data.size();
		}

		@Override
		public void close() {

		}

		@Override
		public void put(String key, MqttPersistable persistable) {
			data.put(key, persistable);
		}

		@Override
		public MqttPersistable get(String key)  {
			return data.get(key);
		}

		@Override
		public void remove(String key)  {
			data.remove(key);
		}

		@Override
		public Enumeration keys()  {
			return data.keys();
		}

		@Override
		public void clear()  {
			data.clear();
		}

		@Override
		public boolean containsKey(String key)  {
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

	@Override
	int getModeId() {
		return MODE_ID;
	}

	@Override
	protected MessageBase onFinalizeMessage(MessageBase message) {
		// Not relevant for MQTT mode
		return message;
	}
}
