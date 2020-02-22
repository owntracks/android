package org.owntracks.android.services;

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
import org.greenrobot.eventbus.Subscribe;
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
import java.util.concurrent.BlockingDeque;
import java.util.concurrent.TimeUnit;

import timber.log.Timber;

public class MessageProcessorEndpointMqtt extends MessageProcessorEndpoint implements StatefulServiceMessageProcessor {
    public static final int MODE_ID = 0;

    private CustomMqttClient mqttClient;

    private String lastConnectionId;
    private static EndpointState state;

    private MessageProcessor messageProcessor;
    private final BlockingDeque<MessageBase> outgoingQueue;
    private Parser parser;
    private Preferences preferences;
    private Scheduler scheduler;
    private EventBus eventBus;

    MessageProcessorEndpointMqtt(MessageProcessor messageProcessor, Parser parser, Preferences preferences, Scheduler scheduler, EventBus eventBus, BlockingDeque<MessageBase> outgoingQueue) {
        super(messageProcessor);
        this.parser = parser;
        this.preferences = preferences;
        this.scheduler = scheduler;
        this.eventBus = eventBus;
        this.messageProcessor = messageProcessor;
        this.outgoingQueue = outgoingQueue;
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

    private synchronized void sendMessage(MessageBase m) throws ConfigurationIncompleteException, MqttConnectionException, MqttException, IOException {
        m.addMqttPreferences(preferences); // TODO send it twice if it's a MessageClear
        long messageId = m.getMessageId();
        sendMessageConnectPressure++;
        try {
            connectToBroker();
        } catch (ConfigurationIncompleteException | MqttConnectionException e) {
            Timber.v("failed connection attempts :%s", sendMessageConnectPressure);
            messageProcessor.onMessageDeliveryFailed(messageId);
            throw e;
        }

        try {
            IMqttDeliveryToken pubToken = this.mqttClient.publish(m.getTopic(), parser.toJsonBytes(m), m.getQos(), m.getRetained());
            pubToken.waitForCompletion(TimeUnit.SECONDS.toMillis(30));
            Timber.v("message sent: %s", messageId);
            messageProcessor.onMessageDelivered(messageId);
        } catch (MqttException e) {
            Timber.e(e,"MQTT Exception delivering message");
            messageProcessor.onMessageDeliveryFailed(messageId);
            throw e;
        } catch (IOException e) {
            // Message will not contain BUNDLE_KEY_ACTION and will be dropped by scheduler
            Timber.e(e, "JSON serialization failed for message %s. Message will be dropped", m.getMessageId());
            messageProcessor.onMessageDeliveryFailedFinal(messageId);
            throw e;
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
            changeState(EndpointState.DISCONNECTED.withError(cause));
        }

        @Override
        public void messageArrived(String topic, MqttMessage message) {
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
            } catch (Parser.EncryptionException e) {
                Timber.e("%s payload:%s ", e.getMessage(), new String(message.getPayload()));
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

    private CustomMqttClient buildMqttClient() throws URISyntaxException, MqttException {
        Timber.v("Initializing new mqttClient");

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
        Timber.v("mode: %s", preferences.getModeId());
        Timber.v("client id: %s", cid);
        Timber.v("connect string: %s", connectString);

        CustomMqttClient mqttClient = new CustomMqttClient(connectString, cid, new MqttClientMemoryPersistence());
        mqttClient.setCallback(iCallbackClient);
        return mqttClient;
    }

    private int sendMessageConnectPressure = 0;

    @WorkerThread
    private synchronized void connectToBroker() throws MqttConnectionException, ConfigurationIncompleteException {
        sendMessageConnectPressure++;
        boolean isUiThread = Build.VERSION.SDK_INT >= Build.VERSION_CODES.M ? Looper.getMainLooper().isCurrentThread()
                : Thread.currentThread() == Looper.getMainLooper().getThread();

        if (isConnected()) {
            Timber.v("already connected");
            changeState(getState()); // Background service might be restarted and not get the connection state
            return;
        }

        if (isConnecting()) {
            Timber.v("already connecting");
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
            Timber.v("Connecting on non-ui worker thread: %s", Thread.currentThread());
        }
        changeState(EndpointState.CONNECTING);
        if (this.mqttClient == null) {
            try {
                this.mqttClient = buildMqttClient();
            } catch (URISyntaxException | MqttException e) {
                Timber.e(e, "Error creating MQTT client");
                changeState(EndpointState.ERROR.withError(e));
                throw new MqttConnectionException(e);
            }
        }
        MqttConnectOptions mqttConnectOptions = getMqttConnectOptions();

        try {
            Timber.v("MQTT connecting synchronously");
            this.mqttClient.connect(mqttConnectOptions).waitForCompletion();
        } catch (MqttException e) {
            changeState(EndpointState.ERROR.withError(e));
            throw new MqttConnectionException(e);
        }
        Timber.v("MQTT Connected success");
        Timber.v("Queue depth: %s", outgoingQueue.size());
        scheduler.scheduleMqttPing(mqttConnectOptions.getKeepAliveInterval());
        changeState(EndpointState.CONNECTED);

        sendMessageConnectPressure = 0; // allow new connection attempts from queueMessageForSending
    }

    private MqttConnectOptions getMqttConnectOptions() throws MqttConnectionException {
        MqttConnectOptions  connectOptions = new MqttConnectOptions();
        if (preferences.getAuth()) {
            if (preferences.getUsePassword()) {
                connectOptions.setPassword(preferences.getPassword().toCharArray());
            }
            connectOptions.setUserName(preferences.getUsername());
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
        if (preferences.getUsername().trim().isEmpty()) {
            throw new ConfigurationIncompleteException("Username missing");
        }
        if (preferences.getAuth() && (preferences.getPassword().trim().isEmpty() && !preferences.getUsePassword())) {
            throw new ConfigurationIncompleteException("Authentication configured but password missing");
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


    @SuppressWarnings("UnusedParameters")
    @Subscribe
    public void onEvent(Events.EndpointChanged e) {
        reconnect();
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

    private void sendAvailableMessages() {
        Timber.v("Starting outbound message loop");
        MessageBase lastFailedMessageToBeRetried = null;
        while (true) {
            try {
                MessageBase message;
                if (lastFailedMessageToBeRetried == null) {
                    message = outgoingQueue.take();
                } else {
                    message = lastFailedMessageToBeRetried
                }
                try {
                    sendMessage(message);
                } catch (MqttException | MqttConnectionException | ConfigurationIncompleteException e) {
                    Timber.w(("Error sending message. Re-queueing"));
                    lastFailedMessageToBeRetried = message;
                } catch (IOException e) {
                    // Deserialization failure, drop and move on
                }
                if (lastFailedMessageToBeRetried != null) {
                    Thread.sleep(15000);
                }
            } catch (InterruptedException e) {
                Timber.i(e, "Outgoing message loop interrupted");
                break;
            }
        }
        Timber.w("Exiting outgoingmessage loop");
    }

    @Override
    public Runnable getBackgroundOutgoingRunnable() {
        return this::sendAvailableMessages;
    }

    @Override
    protected MessageBase onFinalizeMessage(MessageBase message) {
        // Not relevant for MQTT mode
        return message;
    }


}

class MqttConnectionException extends Throwable {
    MqttConnectionException(Exception e) {
        super(e);
    }
}