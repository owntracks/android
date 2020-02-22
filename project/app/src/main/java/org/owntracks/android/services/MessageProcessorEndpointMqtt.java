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
import org.owntracks.android.support.interfaces.StatefulServiceMessageProcessor;

import java.io.FileNotFoundException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.List;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.TimeUnit;

import timber.log.Timber;

public class MessageProcessorEndpointMqtt extends MessageProcessorEndpoint implements StatefulServiceMessageProcessor {
    public static final int MODE_ID = 0;

    private CustomMqttClient mqttClient;
    private MqttConnectOptions connectOptions;
    private String lastConnectionId;
    private static EndpointState state;

    private MessageProcessor messageProcessor;
    private final LinkedBlockingDeque<MessageBase> outgoingQueue;
    private Parser parser;
    private Preferences preferences;
    private Scheduler scheduler;
    private EventBus eventBus;

    MessageProcessorEndpointMqtt(MessageProcessor messageProcessor, Parser parser, Preferences preferences, Scheduler scheduler, EventBus eventBus, LinkedBlockingDeque<MessageBase> outgoingQueue) {
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

    private synchronized boolean sendMessage(MessageBase m) {
        m.addMqttPreferences(preferences); // TODO send it twice if it's a MessageClear
        long messageId = m.getMessageId();
        sendMessageConnectPressure++;
        if (!connectToBroker()) {
            Timber.v("failed connection attempts :%s", sendMessageConnectPressure);
            messageProcessor.onMessageDeliveryFailed(messageId);
            return false;
        }

        try {
            IMqttDeliveryToken pubToken = this.mqttClient.publish(m.getTopic(), parser.toJsonBytes(m), m.getQos(), m.getRetained());
            pubToken.waitForCompletion(TimeUnit.SECONDS.toMillis(30));
            Timber.v("message sent: %s", messageId);
            messageProcessor.onMessageDelivered(messageId);
        } catch (MqttException e) {
            Timber.e(e,"MQTT Exception delivering message");
            messageProcessor.onMessageDeliveryFailed(messageId);
            return false;
        } catch (Exception e) {
            // Message will not contain BUNDLE_KEY_ACTION and will be dropped by scheduler
            Timber.e(e, "JSON serialization failed for message %s. Message will be dropped", m.getMessageId());
            messageProcessor.onMessageDeliveryFailedFinal(messageId);
            return false;
        }
        return true;
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
    private synchronized boolean connectToBroker() {
        sendMessageConnectPressure++;
        boolean isUiThread = Build.VERSION.SDK_INT >= Build.VERSION_CODES.M ? Looper.getMainLooper().isCurrentThread()
                : Thread.currentThread() == Looper.getMainLooper().getThread();

        if (isConnected()) {
            Timber.v("already connected");
            changeState(getState()); // Background service might be restarted and not get the connection state
            return true;
        }

        if (isConnecting()) {
            Timber.v("already connecting");
            return false;
        }

        if (!isConfigurationComplete()) {
            changeState(EndpointState.ERROR_CONFIGURATION);
            return false;
        }

        if (isUiThread) {
            try {
                throw new Exception("BLOCKING CONNECT ON MAIN THREAD");
            } catch (Exception e) {
                Timber.e(e);
                e.printStackTrace();
            }
        } else {
            Timber.v("Connecting on non-ui worker thread: %s", Thread.currentThread());
        }
        Timber.v("connecting on thread %s", Thread.currentThread().getId());
        changeState(EndpointState.CONNECTING);
        if (this.mqttClient==null) {
            try {
                this.mqttClient = buildMqttClient();
            } catch (URISyntaxException | MqttException e) {
                Timber.e(e, "Error creating MQTT client");
                changeState(EndpointState.ERROR.withError(e));
                return false;
            }
        }
        connectOptions = new MqttConnectOptions();
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
        } catch (Exception e) {
            changeState(EndpointState.ERROR.withError(e).withMessage("TLS setup failed"));
            return false;
        }

        if (!setWill(connectOptions)) {
            return false;
        }

        connectOptions.setKeepAliveInterval(preferences.getKeepalive());
        connectOptions.setConnectionTimeout(30);
        connectOptions.setCleanSession(preferences.getCleanSession());

        try {
            Timber.v("MQTT connecting synchronously");
            this.mqttClient.connect(connectOptions).waitForCompletion();
        } catch (Exception e) {
            changeState(EndpointState.ERROR.withError(e));
            return false;
        }
        Timber.v("MQTT Connected success");
        Timber.v("Queue depth: %s", outgoingQueue.size());
        scheduler.scheduleMqttPing(connectOptions.getKeepAliveInterval());
        changeState(EndpointState.CONNECTED);

        sendMessageConnectPressure = 0; // allow new connection attempts from queueMessageForSending
        return true;
    }

    private boolean setWill(MqttConnectOptions m) {
        try {
            JSONObject lwt = new JSONObject();
            lwt.put("_type", "lwt");
            lwt.put("tst", (int) TimeUnit.MILLISECONDS.toSeconds(System.currentTimeMillis()));

            m.setWill(preferences.getPubTopicBase(), lwt.toString().getBytes(), 0, false);
        } catch (JSONException ignored) {
        } catch (IllegalArgumentException e) {
            changeState(EndpointState.ERROR_CONFIGURATION.withError(e).withMessage("Invalid pubTopic specified"));
            return false;
        }
        return true;
    }

    private String getConnectionId() {
        return mqttClient.getCurrentServerURI() + "/" + connectOptions.getUserName();
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
        connectToBroker();
    }

    @Override
    public void disconnect() {
        disconnect(true);
    }

    @Override
    public boolean isConfigurationComplete() {
        // Required to connect: host, username (only send when auth is enabled)
        // When auth is enabled, password (unless usePassword is set to false which only sends username)
        return !preferences.getHost().trim().isEmpty() && !preferences.getUsername().trim().isEmpty() && (!preferences.getAuth() || (!preferences.getPassword().trim().isEmpty() || !preferences.getUsePassword()));

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
        if (!isConfigurationComplete()) {
            changeState(EndpointState.ERROR_CONFIGURATION);
        } else {
            scheduler.scheduleMqttReconnect();
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
        while (true) {
            try {
                MessageBase messageBase = outgoingQueue.takeFirst();
                if (!sendMessage(messageBase)) {
                    Timber.w(("Error sending message. Re-queueing"));
                    outgoingQueue.putFirst(messageBase);
                    Thread.sleep(15000);
                }
            } catch (InterruptedException e) {
                Timber.i("Outgoing message loop interrupted");
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
