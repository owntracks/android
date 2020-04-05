package org.owntracks.android.services;

import android.os.Looper;

import androidx.annotation.WorkerThread;

import com.hivemq.client.mqtt.MqttClient;
import com.hivemq.client.mqtt.datatypes.MqttQos;
import com.hivemq.client.mqtt.lifecycle.MqttClientConnectedContext;
import com.hivemq.client.mqtt.lifecycle.MqttClientConnectedListener;
import com.hivemq.client.mqtt.lifecycle.MqttClientDisconnectedContext;
import com.hivemq.client.mqtt.lifecycle.MqttClientDisconnectedListener;
import com.hivemq.client.mqtt.mqtt3.Mqtt3AsyncClient;
import com.hivemq.client.mqtt.mqtt3.Mqtt3BlockingClient;
import com.hivemq.client.mqtt.mqtt3.Mqtt3ClientBuilder;
import com.hivemq.client.mqtt.mqtt3.message.connect.connack.Mqtt3ConnAck;
import com.hivemq.client.mqtt.mqtt3.message.publish.Mqtt3Publish;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.jetbrains.annotations.NotNull;
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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.BlockingDeque;
import java.util.function.Consumer;

import timber.log.Timber;

public class MessageProcessorEndpointMqtt extends MessageProcessorEndpoint implements StatefulServiceMessageProcessor, MqttClientConnectedListener, MqttClientDisconnectedListener {
    public static final int MODE_ID = 0;

    private Mqtt3BlockingClient mqttClient;

    private String lastConnectionId;
    private static EndpointState state;

    private MessageProcessor messageProcessor;

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
        this.outgoingMessageQueue = outgoingQueue;
    }

    synchronized boolean sendKeepalive() {
        Timber.d("NOOP mqttClient.ping()");
        return true;
    }

    synchronized void sendMessage(MessageBase m) throws ConfigurationIncompleteException, OutgoingMessageSendingException, IOException {
        m.addMqttPreferences(preferences); // TODO send it twice if it's a MessageClear
        long messageId = m.getMessageId();
        try {
            connectToBroker();
        } catch (MqttConnectionException e) {
            Timber.w("failed connection attempts :%s", sendMessageConnectPressure);
            messageProcessor.onMessageDeliveryFailed(messageId);
            throw new OutgoingMessageSendingException(e);
        } catch (ConfigurationIncompleteException e) {
            Timber.w("failed connection attempts :%s", sendMessageConnectPressure);
            messageProcessor.onMessageDeliveryFailed(messageId);
            throw e;
        }

        try {
            this.mqttClient.publishWith().topic(m.getTopic()).qos(MqttQos.fromCode(m.getQos())).payload(parser.toJsonBytes(m)).retain(m.getRetained()).send();

            //IMqttDeliveryToken pubToken = this.mqttClient.publish(m.getTopic(), parser.toJsonBytes(m), m.getQos(), m.getRetained());
            //pubToken.waitForCompletion(TimeUnit.SECONDS.toMillis(30));
            Timber.d("message sent: %s", messageId);
            messageProcessor.onMessageDelivered(m);
            //} catch (MqttException e) {
            //    Timber.e(e,"MQTT Exception delivering message");
            //messageProcessor.onMessageDeliveryFailed(messageId);
            //throw new OutgoingMessageSendingException(e);
        } catch (IOException e) {
            // Message will not contain BUNDLE_KEY_ACTION and will be dropped by scheduler
            Timber.e(e, "JSON serialization failed for message %s. Message will be dropped", m.getMessageId());
            messageProcessor.onMessageDeliveryFailedFinal(messageId);
            throw e;
        }
    }

    private Consumer<Mqtt3Publish> messageConsumer = new Consumer<Mqtt3Publish>() {
        @Override
        public void accept(Mqtt3Publish mqtt3Publish) {
            Timber.tag("Consumer<Mqtt3Publish>").v("message received");

            if (mqtt3Publish.getPayload().isPresent()) {
                try {
                    MessageBase m = parser.fromJson(mqtt3Publish.getPayloadAsBytes());
                    if (!m.isValidMessage()) {
                        Timber.tag("Consumer<Mqtt3Publish>").e("message failed validation");
                        return;
                    }
                    m.setTopic(mqtt3Publish.getTopic().toString());
                    m.setRetained(mqtt3Publish.isRetain());
                    m.setQos(mqtt3Publish.getQos().getCode());
                    onMessageReceived(m);
                } catch (Parser.EncryptionException | IOException e) {
                    Timber.tag("Consumer<Mqtt3Publish>").e(e, "Decryption failure payload:%s ", new String(mqtt3Publish.getPayloadAsBytes()));
                }
            } else {
                MessageClear m = new MessageClear();
                m.setTopic(mqtt3Publish.getTopic().toString().replace(MessageCard.BASETOPIC_SUFFIX, ""));
                onMessageReceived(m);
            }
        }
    };




    private Mqtt3BlockingClient buildMqttClient() {
        Timber.d("Initializing new mqttClient");

        //TODO: Add TLS support
        @NotNull Mqtt3ClientBuilder b = MqttClient.builder()
                .identifier(preferences.getClientId())
                .serverHost(preferences.getHost())
                .serverPort(preferences.getPort())
                .addConnectedListener(this)
                .addDisconnectedListener(this)
                .useMqttVersion3();


        if (preferences.getTls()) {
            String tlsCaCrt = preferences.getTlsCaCrtName();
            String tlsClientCrt = preferences.getTlsClientCrtName();

            if (tlsCaCrt.length() > 0 || tlsClientCrt.length() > 0) {

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

                SocketFactory factory = null;
                try {
                    factory = new SocketFactory(socketFactoryOptions);
                    b.sslConfig().trustManagerFactory(factory.getTrustManagerFactory()).keyManagerFactory(factory.getKeyManagerFactory()).applySslConfig();

                } catch (Exception e) {
                    e.printStackTrace();
                }
            } else {
                b.sslWithDefaultConfig();
            }
        }


        if(preferences.getWs()) {
            b.webSocketWithDefaultConfig();
        }

        return b.buildBlocking();
    }

    private int sendMessageConnectPressure = 0;

    @WorkerThread
    private synchronized void connectToBroker() throws MqttConnectionException, ConfigurationIncompleteException {
        sendMessageConnectPressure++;
        boolean isUiThread = Looper.getMainLooper().isCurrentThread();

        if (isConnected()) {
            Timber.d("already connected");
            changeState(getState()); // Background service might be restarted and not get the connection state
            return;
        }

        if (isConnecting()) {
            Timber.d("already connecting");
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
            Timber.d("Connecting on non-ui worker thread: %s", Thread.currentThread());
        }
        changeState(EndpointState.CONNECTING);
        if (this.mqttClient == null) {
            this.mqttClient = buildMqttClient();
        }
        //TODO: schedule ping if possible
        //TODO: send will
        //TODO: send PW optional
        //TODO: TLS
        //try {
        Timber.v("MQTT connecting synchronously");
        Mqtt3ConnAck ack = this.mqttClient.connectWith().simpleAuth().username(preferences.getUsername()).password(preferences.getPassword().getBytes()).applySimpleAuth().keepAlive(preferences.getKeepalive()).cleanSession(preferences.getCleanSession()).send();
        if(ack.getReturnCode().isError()) {
            Timber.e("connect error: %s", ack.getReturnCode());
            changeState(EndpointState.ERROR.withMessage(ack.getReturnCode().toString()));

        }
        //} catch (MqttException e) {
        //    changeState(EndpointState.ERROR.withError(e));
        //    throw new MqttConnectionException(e);
        //}
        //Timber.d("MQTT Connected success. Queue depth: %s", outgoingMessageQueue.size());
        //scheduler.scheduleMqttPing(this.mqttClient.);

        sendMessageConnectPressure = 0; // allow new connection attempts from queueMessageForSending
    }



    private String getConnectionId() {
        return mqttClient.getConfig().getServerHost()+"/"+mqttClient.getConfig().getServerPort();
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

    @Override
    public void onConnected(@NotNull MqttClientConnectedContext context) {
        Timber.v("-----connected------");
        changeState(EndpointState.CONNECTED);
        scheduler.cancelMqttReconnect();
        scheduler.scheduleMqttKeepalive(preferences.getKeepalive());
        onConnect();
    }

    @Override
    public void onDisconnected(@NotNull MqttClientDisconnectedContext context) {
        Timber.v("-----disconnected ------");
        changeState(EndpointState.DISCONNECTED.withError(context.getCause()));
        scheduler.scheduleMqttReconnect();
        scheduler.cancelMqttKeepalive();
    }

    private void subscribe(String[] topics) {
        if (!isConnected()) {
            Timber.e("subscribe when not connected");
            return;
        }

        //TODO: improve to send all in one sub
        //TODO: handle sub failuer
        int[] qos = getSubTopicsQos(topics);
        Mqtt3AsyncClient.Mqtt3SubscribeAndCallbackBuilder.@NotNull Start subscriber = this.mqttClient.toAsync().subscribeWith();

        for (String t: topics) {
            Timber.v("sub to: %s", t);
            subscriber.topicFilter(t).callback(messageConsumer).send();
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

        //TODO: improve to send all in one sub
        int[] qos = getSubTopicsQos(topics);
        for (String t : topics) {
            this.mqttClient.unsubscribeWith().topicFilter("t").send();
        }
    }

    private void disconnect(boolean fromUser) {
        Timber.v("disconnect. user:%s", fromUser);
        if (isConnecting()) {
            return;
        }

        if (isConnected()) {
            Timber.v("Disconnecting");
            this.mqttClient.disconnect();
        }

        this.mqttClient = null;

        if (fromUser)
            changeState(EndpointState.DISCONNECTED_USERDISCONNECT);
        else
            changeState(EndpointState.DISCONNECTED);
        scheduler.cancelMqttKeepalive();
        scheduler.cancelMqttReconnect();
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
            if(isConnected()) {
                return true;
            } else {

                try {
                    connectToBroker();
                } catch (Exception e) {

                }
                return false;
            }
    }

    private void changeState(EndpointState newState) {
        if (state == newState)
            return;

        state = newState;
        messageProcessor.onEndpointStateChanged(newState);
    }

    private boolean isConnected() {
        return (this.mqttClient != null) && (state == EndpointState.CONNECTED);
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