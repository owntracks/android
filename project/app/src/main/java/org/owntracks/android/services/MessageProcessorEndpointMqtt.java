package org.owntracks.android.services;

import android.content.SharedPreferences;
import android.os.Build;
import android.os.Looper;

import androidx.annotation.WorkerThread;

import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttAsyncClient;
import org.eclipse.paho.client.mqttv3.MqttCallbackExtended;
import org.eclipse.paho.client.mqttv3.MqttClientPersistence;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.MqttPersistable;
import org.greenrobot.eventbus.EventBus;
import org.json.JSONException;
import org.json.JSONObject;
import org.owntracks.android.App;
import org.owntracks.android.messages.MessageBase;
import org.owntracks.android.messages.MessageCard;
import org.owntracks.android.messages.MessageClear;
import org.owntracks.android.services.MessageProcessor.EndpointState;
import org.owntracks.android.services.worker.Scheduler;
import org.owntracks.android.support.Events;
import org.owntracks.android.support.Parser;
import org.owntracks.android.support.Preferences;
import org.owntracks.android.support.RunThingsOnOtherThreads;
import org.owntracks.android.support.SocketFactory;
import org.owntracks.android.support.interfaces.ConfigurationIncompleteException;
import org.owntracks.android.support.interfaces.StatefulServiceMessageProcessor;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.List;
import java.util.concurrent.TimeUnit;

import timber.log.Timber;

import static org.owntracks.android.support.RunThingsOnOtherThreads.NETWORK_HANDLER_THREAD_NAME;

public class MessageProcessorEndpointMqtt extends MessageProcessorEndpoint implements StatefulServiceMessageProcessor, Preferences.OnPreferenceChangedListener {
    public static final int MODE_ID = 0;

    private CustomMqttClient mqttClient;

    private String lastConnectionId;
    private static EndpointState state;

    private MessageProcessor messageProcessor;
    private RunThingsOnOtherThreads runThingsOnOtherThreads;

    private Parser parser;
    private Preferences preferences;
    private Scheduler scheduler;
    private EventBus eventBus;

    MessageProcessorEndpointMqtt(MessageProcessor messageProcessor, Parser parser, Preferences preferences, Scheduler scheduler, EventBus eventBus, RunThingsOnOtherThreads runThingsOnOtherThreads) {
        super(messageProcessor);
        this.parser = parser;
        this.preferences = preferences;
        this.scheduler = scheduler;
        this.eventBus = eventBus;
        this.messageProcessor = messageProcessor;
        this.runThingsOnOtherThreads = runThingsOnOtherThreads;
        preferences.registerOnPreferenceChangedListener(this);
    }

    synchronized boolean sendKeepalive() {
        // Connects if not connected or sends a ping message if aleady connected
        if (checkConnection() && mqttClient != null) {
            mqttClient.ping();
            return true;
        } else {
            return false;
        }
    }

    synchronized void sendMessage(MessageBase m) throws ConfigurationIncompleteException, OutgoingMessageSendingException, IOException {
        m.addMqttPreferences(preferences); // TODO send it twice if it's a MessageClear
        long messageId = m.getMessageId();
        try {
            connectToBroker();
        } catch (MqttConnectionException e) {
            Timber.tag("outgoing").w("failed connection attempts :%s", sendMessageConnectPressure);
            messageProcessor.onMessageDeliveryFailed(messageId);
            throw new OutgoingMessageSendingException(e);
        } catch (ConfigurationIncompleteException e) {
            Timber.tag("outgoing").w("failed connection attempts :%s", sendMessageConnectPressure);
            messageProcessor.onMessageDeliveryFailed(messageId);
            throw e;
        }

        try {
            IMqttDeliveryToken pubToken = this.mqttClient.publish(m.getTopic(), parser.toJsonBytes(m), m.getQos(), m.getRetained());
            long startTime = System.nanoTime();
            pubToken.waitForCompletion(TimeUnit.SECONDS.toMillis(30));
            long endTime = System.nanoTime();
            long duration = (endTime - startTime);
            Timber.tag("outgoing").d("message id %s sent in %dms", messageId, TimeUnit.NANOSECONDS.toMillis(duration));
            messageProcessor.onMessageDelivered(m);
        } catch (MqttException e) {
            Timber.tag("outgoing").e(e, "MQTT Exception delivering message");
            messageProcessor.onMessageDeliveryFailed(messageId);
            throw new OutgoingMessageSendingException(e);
        } catch (IOException e) {
            // Message will not contain BUNDLE_KEY_ACTION and will be dropped by scheduler
            Timber.tag("outgoing").e(e, "JSON serialization failed for message %s. Message will be dropped", m.getMessageId());
            messageProcessor.onMessageDeliveryFailedFinal(messageId);
            throw e;
        }
    }

    private final MqttCallbackExtended iCallbackClient = new MqttCallbackExtended() {
        @Override
        public void connectComplete(boolean reconnect, String serverURI) {
            Timber.tag("mqttcallback").d("Connect Complete. Reconnected: %s, serverUri:%s", reconnect, serverURI);
            onConnect();
        }

        @Override
        public void deliveryComplete(IMqttDeliveryToken token) {

        }

        @Override
        public void connectionLost(Throwable cause) {
            Timber.tag("mqttcallback").e(cause, "connectionLost error");
            scheduler.cancelMqttPing();
            scheduler.scheduleMqttReconnect();
            changeState(EndpointState.DISCONNECTED.withError(cause));
        }

        @Override
        public void messageArrived(String topic, MqttMessage message) {
            try {
                MessageBase m = parser.fromJson(message.getPayload());
                if (!m.isValidMessage()) {
                    Timber.tag("mqttcallback").e("message failed validation");
                    return;
                }
                m.setTopic(topic);
                m.setRetained(message.isRetained());
                m.setQos(message.getQos());
                onMessageReceived(m);
            } catch (Parser.EncryptionException e) {
                Timber.tag("mqttcallback").e(e, "Decryption failure payload:%s ", new String(message.getPayload()));
            } catch (IOException e) {
                if (message.getPayload().length == 0) {
                    Timber.tag("mqttcallback").d("clear message received: %s", topic);
                    MessageClear m = new MessageClear();
                    m.setTopic(topic.replace(MessageCard.BASETOPIC_SUFFIX, ""));
                    onMessageReceived(m);
                } else {
                    Timber.tag("mqttcallback").e(e, "payload: %s ", new String(message.getPayload()));
                }
            }
        }
    };

    private CustomMqttClient buildMqttClient() throws URISyntaxException, MqttException {
        Timber.tag("outgoing").d("Initializing new mqttClient");

        String scheme = "tcp";
        if (preferences.getTls()) {
            if (preferences.getWs()) {
                scheme = "wss";
            } else
                scheme = "ssl";
        } else {
            if (preferences.getWs())
                scheme = "ws";
        }

        String cid = preferences.getClientId();

        String connectString = new URI(scheme, null, preferences.getHost(), preferences.getPort(), null, null, null).toString();
        Timber.d("client id :%s, connect string: %s", cid, connectString);

        CustomMqttClient mqttClient = new CustomMqttClient(connectString, cid, new MqttClientMemoryPersistence());
        mqttClient.setCallback(iCallbackClient);
        return mqttClient;
    }

    private int sendMessageConnectPressure = 0;

    @WorkerThread
    private synchronized void connectToBroker() throws MqttConnectionException, ConfigurationIncompleteException {
        Timber.tag("outgoing").d("Connecting to broker. ThreadId: %s", Thread.currentThread());
        sendMessageConnectPressure++;
        boolean isUiThread = Build.VERSION.SDK_INT >= Build.VERSION_CODES.M ? Looper.getMainLooper().isCurrentThread()
                : Thread.currentThread() == Looper.getMainLooper().getThread();

        if (isConnected()) {
            Timber.tag("outgoing").d("already connected");
            changeState(getState()); // Background service might be restarted and not get the connection state
            return;
        }

        if (isConnecting()) {
            Timber.tag("outgoing").d("already connecting");
            return;
        }

        try {
            checkConfigurationComplete();
        } catch (ConfigurationIncompleteException e) {
            changeState(EndpointState.ERROR_CONFIGURATION.withError(e));
            throw e;
        }

        if (isUiThread) {
            throw new RuntimeException("BLOCKING CONNECT ON MAIN THREAD");
        } else {
            Timber.tag("outgoing").d("Connecting on non-ui worker thread: %s", Thread.currentThread());
        }
        changeState(EndpointState.CONNECTING);

        try {
            this.mqttClient = buildMqttClient();
        } catch (URISyntaxException | MqttException e) {
            Timber.tag("outgoing").e(e, "Error creating MQTT client");
            changeState(EndpointState.ERROR.withError(e));
            throw new MqttConnectionException(e);
        }

        MqttConnectOptions mqttConnectOptions = getMqttConnectOptions();

        try {
            Timber.tag("outgoing").v("MQTT connecting synchronously");
            this.mqttClient.connect(mqttConnectOptions).waitForCompletion();
        } catch (MqttException e) {
            changeState(EndpointState.ERROR.withError(e));
            throw new MqttConnectionException(e);
        }
        Timber.tag("outgoing").d("MQTT Connected success.");
        scheduler.scheduleMqttPing(mqttConnectOptions.getKeepAliveInterval());
        changeState(EndpointState.CONNECTED);

        sendMessageConnectPressure = 0; // allow new connection attempts from queueMessageForSending
    }

    private MqttConnectOptions getMqttConnectOptions() throws MqttConnectionException {
        MqttConnectOptions connectOptions = new MqttConnectOptions();
        /* Even though the MQTT spec supports various different combinations of setting the username
        & password flags to allow and differentiate between no usernames, empty usernames etc. there's
        too many permutations of these to usefully expose to the end user. So, to simplify: if the
        username is not the empty string, send the username and password. If it is empty, then don't
        send either.

        This might change depending on what users want.
         */
        if (!preferences.getUsername().trim().equals("")) {
            connectOptions.setUserName(preferences.getUsername());
            connectOptions.setPassword(preferences.getPassword().toCharArray());
        }
        connectOptions.setMqttVersion(preferences.getMqttProtocolLevel());
        try {
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

        } catch (CertificateException | NoSuchAlgorithmException | UnrecoverableKeyException | KeyStoreException | KeyManagementException | IOException e) {
            changeState(EndpointState.ERROR.withError(e).withMessage("TLS setup failed"));
            throw new MqttConnectionException(e);
        }

        setWill(connectOptions);

        connectOptions.setKeepAliveInterval(preferences.getKeepalive());
        connectOptions.setConnectionTimeout(30);
        connectOptions.setCleanSession(preferences.getCleanSession());
        return connectOptions;
    }

    private void setWill(MqttConnectOptions m) {
        try {
            JSONObject lwt = new JSONObject();
            lwt.put("_type", "lwt");
            lwt.put("tst", (int) TimeUnit.MILLISECONDS.toSeconds(System.currentTimeMillis()));

            m.setWill(preferences.getPubTopicBase(), lwt.toString().getBytes(), 0, false);
        } catch (JSONException ignored) {
        } catch (IllegalArgumentException e) {
            changeState(EndpointState.ERROR_CONFIGURATION.withError(e).withMessage("Invalid pubTopic specified"));
            throw e;
        }
    }

    private String getConnectionId() {
        return String.format("%s/%s", mqttClient.getCurrentServerURI(), mqttClient.getClientId());
    }

    private void onConnect() {
        scheduler.cancelMqttReconnect();
        // Check if we're connecting to the same broker that we were already connected to
        String connectionId = getConnectionId();
        if (lastConnectionId != null && !connectionId.equals(lastConnectionId)) {
            eventBus.post(new Events.EndpointChanged());
            lastConnectionId = connectionId;
            Timber.v("lastConnectionId changed to: %s", lastConnectionId);
        }

        List<String> topics = new ArrayList<>();
        String subTopicBase = preferences.getSubTopic();

        if (!preferences.getSub()) // Don't subscribe if base topic is invalid
            return;
        else if (subTopicBase.endsWith("#")) { // wildcard sub will match everything anyway
            topics.add(subTopicBase);
        } else {
            topics.add(subTopicBase);
            if (preferences.getInfo())
                topics.add(subTopicBase + preferences.getPubTopicInfoPart());

            topics.add(preferences.getPubTopicBase() + preferences.getPubTopicCommandsPart());
            topics.add(subTopicBase + preferences.getPubTopicEventsPart());
            topics.add(subTopicBase + preferences.getPubTopicWaypointsPart());
        }
        subscribe(topics.toArray(new String[0]));
    }

    private void subscribe(String[] topics) {
        if (!isConnected()) {
            Timber.e("subscribe when not connected");
            return;
        }
        for (String s : topics) {
            Timber.v("subscribe() - Will subscribe to: %s", s);
        }
        try {
            int[] qos = getSubTopicsQos(topics);
            this.mqttClient.subscribe(topics, qos);
        } catch (MqttException e) {
            changeState(EndpointState.ERROR.withError(e).withMessage("Subscribe failed"));
        }
    }

    private int[] getSubTopicsQos(String[] topics) {
        int[] qos = new int[topics.length];
        Arrays.fill(qos, preferences.getSubQos());
        return qos;
    }

    @SuppressWarnings("unused")
    private void unsubscribe(String[] topics) {
        if (!isConnected()) {
            Timber.e("subscribe when not connected");
            return;
        }

        for (String s : topics) {
            Timber.v("unsubscribe() - Will unsubscribe from: %s", s);
        }

        try {
            mqttClient.unsubscribe(topics);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void disconnect(boolean fromUser) {
        Timber.tag("outgoing").v("disconnect. Manually triggered? %s. ThreadID: %s", fromUser, Thread.currentThread());
        if (isConnecting()) {
            return;
        }

        try {
            if (isConnected()) {
                Timber.v("Disconnecting");
                this.mqttClient.disconnect(0);
            }

        } catch (MqttException e) {
            Timber.e(e, "Error disconnecting from broker");
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
        if (!Thread.currentThread().getName().equals(NETWORK_HANDLER_THREAD_NAME)) {
            runThingsOnOtherThreads.postOnNetworkHandlerDelayed(this::reconnect, 0);
            return;
        }
        disconnect(false);
        try {
            connectToBroker();
        } catch (MqttConnectionException | ConfigurationIncompleteException e) {
            Timber.e(e, "Failed to reconnect to MQTT broker");
        }
    }

    @Override
    public void disconnect() {
        disconnect(true);
    }

    @Override
    public void checkConfigurationComplete() throws ConfigurationIncompleteException {
        // Required to connect: host, username (only send when auth is enabled)
        // When auth is enabled, password (unless usePassword is set to false which only sends username)
        if (preferences.getHost().trim().isEmpty()) {
            throw new ConfigurationIncompleteException("Host missing");
        }
    }

    @WorkerThread
    @Override
    public boolean checkConnection() {
        return isConnected();
    }

    private void changeState(EndpointState newState) {
        if (state == newState)
            return;

        state = newState;
        messageProcessor.onEndpointStateChanged(newState);
    }

    private boolean isConnected() {
        return this.mqttClient != null && this.mqttClient.isConnected();
    }

    private boolean isConnecting() {
        return (this.mqttClient != null) && (state == EndpointState.CONNECTING);
    }

    private static EndpointState getState() {
        return state;
    }

    @Override
    public void onDestroy() {
        disconnect(false);
        scheduler.cancelMqttTasks();
    }

    @Override
    public void onCreateFromProcessor() {
        try {
            checkConfigurationComplete();
            scheduler.scheduleMqttReconnect();
        } catch (ConfigurationIncompleteException e) {
            changeState(EndpointState.ERROR_CONFIGURATION.withError(e));
        }
    }

    @Override
    public void onAttachAfterModeChanged() {
        //NOOP
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        if (preferences.getModeId() != MessageProcessorEndpointMqtt.MODE_ID) {
            return;
        }
        if (Preferences.Keys.MQTT_PROTOCOL_LEVEL.equals(key) ||
                Preferences.Keys.HOST.equals(key) ||
                Preferences.Keys.PASSWORD.equals(key) ||
                Preferences.Keys.PORT.equals(key) ||
                Preferences.Keys.CLIENT_ID.equals(key) ||
                Preferences.Keys.TLS.equals(key) ||
                Preferences.Keys.TLS_CA_CRT.equals(key) ||
                Preferences.Keys.TLS_CLIENT_CRT.equals(key) ||
                Preferences.Keys.TLS_CLIENT_CRT_PASSWORD.equals(key) ||
                Preferences.Keys.WS.equals(key) ||
                Preferences.Keys.DEVICE_ID.equals(key)

        ) {
            Timber.d("MQTT preferences changed. Reconnecting to broker. ThreadId: %s", Thread.currentThread());
            reconnect();
        }
    }

    private static final class MqttClientMemoryPersistence implements MqttClientPersistence {
        private static Hashtable<String, MqttPersistable> data;

        @Override
        public void open(String s, String s2) {
            if (data == null) {
                data = new Hashtable<>();
            }
        }

        @SuppressWarnings("unused")
        private Integer getSize() {
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
        public MqttPersistable get(String key) {
            return data.get(key);
        }

        @Override
        public void remove(String key) {
            data.remove(key);
        }

        @Override
        public Enumeration keys() {
            return data.keys();
        }

        @Override
        public void clear() {
            data.clear();
        }

        @Override
        public boolean containsKey(String key) {
            return data.containsKey(key);
        }
    }

    private static final class CustomMqttClient extends MqttAsyncClient {

        CustomMqttClient(String serverURI, String clientId, MqttClientPersistence persistence) throws MqttException {
            super(serverURI, clientId, persistence);
        }

        void ping() {
            if (comms != null)
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

class MqttConnectionException extends Exception {
    MqttConnectionException(Exception e) {
        super(e);
    }
}