package org.owntracks.android.services;

import static org.eclipse.paho.client.mqttv3.MqttException.REASON_CODE_CLIENT_DISCONNECT_PROHIBITED;
import static org.owntracks.android.support.RunThingsOnOtherThreads.NETWORK_HANDLER_THREAD_NAME;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Looper;

import androidx.annotation.WorkerThread;

import org.eclipse.paho.client.mqttv3.IMqttAsyncClient;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.IMqttToken;
import org.eclipse.paho.client.mqttv3.MqttAsyncClient;
import org.eclipse.paho.client.mqttv3.MqttCallbackExtended;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.TimerPingSender;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;
import org.greenrobot.eventbus.EventBus;
import org.jetbrains.annotations.NotNull;
import org.json.JSONException;
import org.json.JSONObject;
import org.owntracks.android.R;
import org.owntracks.android.data.EndpointState;
import org.owntracks.android.model.messages.MessageBase;
import org.owntracks.android.model.messages.MessageCard;
import org.owntracks.android.model.messages.MessageClear;
import org.owntracks.android.services.worker.Scheduler;
import org.owntracks.android.support.Events;
import org.owntracks.android.support.Parser;
import org.owntracks.android.support.Preferences;
import org.owntracks.android.support.RunThingsOnOtherThreads;
import org.owntracks.android.support.SocketFactory;
import org.owntracks.android.support.interfaces.ConfigurationIncompleteException;
import org.owntracks.android.support.interfaces.StatefulServiceMessageProcessor;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.Arrays;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

import timber.log.Timber;

public class MessageProcessorEndpointMqtt extends MessageProcessorEndpoint implements StatefulServiceMessageProcessor, SharedPreferences.OnSharedPreferenceChangeListener {
    public static final int MODE_ID = 0;

    private IMqttAsyncClient mqttClient;

    private String lastConnectionId;
    private static EndpointState state;

    private final MessageProcessor messageProcessor;
    private final RunThingsOnOtherThreads runThingsOnOtherThreads;
    private final Context applicationContext;

    private final Parser parser;
    private final Preferences preferences;
    private final Scheduler scheduler;
    private final EventBus eventBus;

    private final Semaphore connectingLock = new Semaphore(1);

    MessageProcessorEndpointMqtt(MessageProcessor messageProcessor, Parser parser, Preferences preferences, Scheduler scheduler, EventBus eventBus, RunThingsOnOtherThreads runThingsOnOtherThreads, Context applicationContext) {
        super(messageProcessor);
        this.parser = parser;
        this.preferences = preferences;
        this.scheduler = scheduler;
        this.eventBus = eventBus;
        this.messageProcessor = messageProcessor;
        this.runThingsOnOtherThreads = runThingsOnOtherThreads;
        this.applicationContext = applicationContext;
        if (preferences != null) {
            preferences.registerOnPreferenceChangedListener(this);
        }
    }

    void reconnectAndSendKeepalive(Semaphore completionNotifier) {
        if (!Thread.currentThread().getName().equals(NETWORK_HANDLER_THREAD_NAME)) {
            runThingsOnOtherThreads.postOnNetworkHandlerDelayed(() -> reconnectAndSendKeepalive(completionNotifier), 0);
            return;
        }
        try {
            if (!checkConnection()) {
                reconnect();
            }
        } finally {
            if (completionNotifier != null) {
                completionNotifier.release();
            }
        }
    }

    synchronized void sendMessage(MessageBase m) throws ConfigurationIncompleteException, OutgoingMessageSendingException, IOException {
        Timber.d("Sending message %s. Thread: %s", m, Thread.currentThread());
        m.addMqttPreferences(preferences);
        String messageId = m.getMessageId();
        try {
            connectToBroker();
        } catch (MqttConnectionException | AlreadyConnectingToBrokerException e) {
            Timber.w("failed connection attempts: %s", sendMessageConnectPressure);
            messageProcessor.onMessageDeliveryFailed(messageId);
            throw new OutgoingMessageSendingException(e);
        } catch (ConfigurationIncompleteException e) {
            Timber.w("failed connection attempts :%s", sendMessageConnectPressure);
            messageProcessor.onMessageDeliveryFailed(messageId);
            throw e;
        }

        try {
            IMqttDeliveryToken pubToken = this.mqttClient.publish(m.getTopic(), m.toJsonBytes(parser), m.getQos(), m.getRetained());
            long startTime = System.nanoTime();
            pubToken.waitForCompletion(TimeUnit.SECONDS.toMillis(30));
            long endTime = System.nanoTime();
            long duration = (endTime - startTime);
            Timber.i("%s id=%s sent in %dms", m.getClass().getSimpleName(), messageId, TimeUnit.NANOSECONDS.toMillis(duration));
            messageProcessor.onMessageDelivered(m);
        } catch (MqttException e) {
            Timber.e(e, "MQTT Exception delivering message");
            messageProcessor.onMessageDeliveryFailed(messageId);
            throw new OutgoingMessageSendingException(e);
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
            Timber.d("Connect Complete. Reconnected: %s, serverUri:%s", reconnect, serverURI);
            onConnect();
        }

        @Override
        public void deliveryComplete(IMqttDeliveryToken token) {

        }

        @Override
        public void connectionLost(Throwable cause) {
            Timber.e(cause, "connectionLost error");
            scheduler.cancelMqttPing();
            changeState(EndpointState.DISCONNECTED.withError(cause));
            Timber.d("Releasing connectinglock");
            connectingLock.release();
            scheduler.scheduleMqttReconnect();
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
                Timber.e(e, "Decryption failure message: %s ", message);
            } catch (IOException e) {
                if (message.getPayload().length == 0) {
                    Timber.d("clear message received: %s", topic);
                    MessageClear m = new MessageClear();
                    m.setTopic(topic.replace(MessageCard.BASETOPIC_SUFFIX, ""));
                    onMessageReceived(m);
                } else {
                    Timber.e(e, "message: %s", message);
                }
            } catch (RuntimeException e) {
                if (e.getMessage() != null && e.getMessage().equals("Decryption failed. Ciphertext failed verification")) {
                    Timber.e(e);
                } else {
                    throw e;
                }
            }
        }
    };

    private IMqttAsyncClient buildMqttClient() throws URISyntaxException, MqttException {
        Timber.d("Initializing new mqttClient");

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
        try {
            IMqttAsyncClient mqttClient = new MqttAsyncClient(connectString, cid, new MemoryPersistence(), new TimerPingSender(), null, new AndroidHighResolutionTimer());
            mqttClient.setCallback(iCallbackClient);
            return mqttClient;
        } catch (IllegalArgumentException e) {
            throw new URISyntaxException(connectString, "Invalid URL");
        }
    }

    private int sendMessageConnectPressure = 0;

    @WorkerThread
    private synchronized void connectToBroker() throws MqttConnectionException, ConfigurationIncompleteException, AlreadyConnectingToBrokerException {
        boolean isUiThread = Build.VERSION.SDK_INT >= Build.VERSION_CODES.M ? Looper.getMainLooper().isCurrentThread()
                : Thread.currentThread() == Looper.getMainLooper().getThread();

        if (isConnected()) {
            Timber.d("already connected to broker");
            changeState(getState()); // Background service might be restarted and not get the connection state
            return;
        }

        Timber.d("Connecting to broker. ThreadId: %s", Thread.currentThread());
        sendMessageConnectPressure++;

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
        if (!connectingLock.tryAcquire()) {
            Timber.w("already connecting to broker");
            throw new AlreadyConnectingToBrokerException();
        }
        Timber.d("Acquired connecting lock");
        changeState(EndpointState.CONNECTING);

        try {
            if (this.mqttClient != null) {
                Timber.d("Disconnecting mqtt Client");
                try {
                    this.mqttClient.disconnect().waitForCompletion();
                } catch (MqttException e) {
                    Timber.d(e, "Error disconnecting from mqtt client.");
                    if (e.getReasonCode() == REASON_CODE_CLIENT_DISCONNECT_PROHIBITED) {
                        Timber.w("Disconnect existing mqtt client would deadlock, not continuing connect");
                        throw e;
                    }
                }
            }
            this.mqttClient = buildMqttClient();
        } catch (URISyntaxException | MqttException e) {
            Timber.e(e, "Error creating MQTT client");
            Timber.d("Releasing connectinglock");
            connectingLock.release();
            changeState(EndpointState.ERROR.withError(e));
            throw new MqttConnectionException(e);
        }

        try {
            MqttConnectOptions mqttConnectOptions = getMqttConnectOptions();
            Timber.v("MQTT connecting synchronously");
            IMqttToken token = this.mqttClient.connect(mqttConnectOptions);
            token.waitForCompletion();
        } catch (MqttException e) {
            if (e.getReasonCode() != 32100) {
                // Client is not already connected
                Timber.d("Releasing connectinglock");
                changeState(EndpointState.ERROR.withError(e));
                throw new MqttConnectionException(e);
            }
        } finally {
            connectingLock.release();
        }
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
        InputStream clientCaInputStream = null;
        InputStream clientCertInputStream = null;
        try {
            if (preferences.getTls()) {
                String tlsCaCrt = preferences.getTlsCaCrt();
                String tlsClientCrt = preferences.getTlsClientCrt();

                SocketFactory.SocketFactoryOptions socketFactoryOptions = new SocketFactory.SocketFactoryOptions();

                if (tlsCaCrt.length() > 0) {
                    try {
                        clientCaInputStream = applicationContext.openFileInput(tlsCaCrt);
                        socketFactoryOptions.withCaInputStream(clientCaInputStream);

                        /* The default for paho is to validate hostnames as per the HTTPS spec. However, this causes
                        a bit of a breakage for some users using self-signed certificates, where the verification of
                        the hostname is unnecessary under certain circumstances. Specifically when the fingerprint of
                        the server leaf certificate is the same as the certificate supplied as the CA (as would be the
                        case using self-signed certs.

                        So we turn off HTTPS behaviour and supply our own hostnameverifier that knows about the self-signed
                        case.
                         */

                        connectOptions.setHttpsHostnameVerificationEnabled(false);
                        try (FileInputStream caFileInputStream = applicationContext.openFileInput(tlsCaCrt)) {
                            X509Certificate ca = (X509Certificate) CertificateFactory.getInstance("X.509").generateCertificate(caFileInputStream);
                            connectOptions.setSSLHostnameVerifier(new MqttHostnameVerifier(ca));
                        }
                    } catch (FileNotFoundException e) {
                        Timber.e(e);
                    }
                }

                if (tlsClientCrt.length() > 0) {
                    try {
                        clientCertInputStream = applicationContext.openFileInput(tlsClientCrt);
                        socketFactoryOptions.withClientP12InputStream(clientCertInputStream).withClientP12Password(preferences.getTlsClientCrtPassword());
                    } catch (FileNotFoundException e) {
                        Timber.e(e);
                    }
                }

                connectOptions.setSocketFactory(new SocketFactory(socketFactoryOptions));
            }

        } catch (CertificateException | NoSuchAlgorithmException | UnrecoverableKeyException | KeyStoreException | KeyManagementException | IOException e) {
            changeState(EndpointState.ERROR.withError(e).withMessage("TLS setup failed"));
            throw new MqttConnectionException(e);
        } finally {
            try {
                if (clientCaInputStream != null) {
                    clientCaInputStream.close();
                }
                if (clientCertInputStream != null) {
                    clientCertInputStream.close();
                }
            } catch (IOException e) {
                Timber.e(e);
            }

        }

        setWill(connectOptions);

        // Autoconnect in paho is *hilariously* buggy. Expect much sadness and many race conditions
        // if you think enabling this is a good idea
        connectOptions.setAutomaticReconnect(false);
        connectOptions.setKeepAliveInterval(preferences.getKeepalive());
        connectOptions.setConnectionTimeout(preferences.getConnectionTimeoutSeconds());

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

    private void onConnect() {
        Timber.d("MQTT connected!. Running onconnect handler (threadID %s)", Thread.currentThread());
        scheduler.scheduleMqttMaybeReconnectAndPing(preferences.getKeepalive());

        Timber.d("Releasing connectinglock");
        connectingLock.release();
        changeState(EndpointState.CONNECTED);

        sendMessageConnectPressure = 0; // allow new connection attempts from queueMessageForSending
        scheduler.cancelMqttReconnect();
        // Check if we're connecting to the same broker that we were already connected to
        String connectionId = String.format("%s/%s", mqttClient.getServerURI(), mqttClient.getClientId());
        if (lastConnectionId != null && !connectionId.equals(lastConnectionId)) {
            eventBus.post(new Events.EndpointChanged());
            lastConnectionId = connectionId;
            Timber.v("lastConnectionId changed to: %s", lastConnectionId);
        }

        if (!preferences.getSub()) // Don't subscribe if base topic is invalid
            return;

        Set<String> topics = getTopicsToSubscribeTo(
                preferences.getSubTopic(),
                preferences.getInfo(),
                preferences.getPubTopicInfoPart(),
                preferences.getPubTopicEventsPart(),
                preferences.getPubTopicWaypointsPart()
        );
        // Receive commands for us
        topics.add(preferences.getPubTopicBase() + preferences.getPubTopicCommandsPart());

        subscribe(topics.toArray(new String[0]));

        messageProcessor.resetMessageSleepBlock();
    }

    @NotNull
    Set<String> getTopicsToSubscribeTo(String subTopics, Boolean subscribeToInfo, String infoTopicSuffix, String eventsTopicSuffix, String waypointsTopicSuffix) {
        Set<String> topics = new TreeSet<>();

        if (subTopics.equals(applicationContext.getString(R.string.defaultSubTopic))) {
            topics.add(subTopics);
            if (subscribeToInfo) {
                topics.add(subTopics + infoTopicSuffix);
            }
            topics.add(subTopics + eventsTopicSuffix);
            topics.add(subTopics + waypointsTopicSuffix);
        } else {
            topics.addAll(Arrays.asList(subTopics.split(" ")));
        }

        return topics;
    }

    private void subscribe(String[] topics) {
        if (!isConnected()) {
            Timber.e("subscribe when not connected");
            return;
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

    private void disconnect() {
        Timber.d("disconnect. ThreadID: %s", Thread.currentThread());
        if (isConnecting()) {
            return;
        }

        try {
            if (mqttClient != null) {
                if (mqttClient.isConnected()) {
                    Timber.d("Disconnecting");
                    this.mqttClient.disconnect().waitForCompletion();
                }
            }
        } catch (MqttException | IllegalArgumentException e) {
            Timber.e(e, "Error disconnecting from broker");
        } finally {
            changeState(EndpointState.DISCONNECTED);
            scheduler.cancelMqttPing();
            scheduler.cancelMqttReconnect();
        }
    }

    public void reconnect() {
        reconnect(null);
    }

    public void reconnect(Semaphore completionNotifier) {
        if (!Thread.currentThread().getName().equals(NETWORK_HANDLER_THREAD_NAME)) {
            runThingsOnOtherThreads.postOnNetworkHandlerDelayed(() -> reconnect(completionNotifier), 0);
            return;
        }
        if (isConnected() || isConnecting()) {
            disconnect();
        }
        try {
            connectToBroker();
        } catch (MqttConnectionException | ConfigurationIncompleteException | AlreadyConnectingToBrokerException e) {
            Timber.e(e, "Failed to reconnect to MQTT broker");
        } finally {
            if (completionNotifier != null) {
                completionNotifier.release();
            }
        }
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
        try {
            return mqttClient != null && mqttClient.isConnected();
        } catch (IllegalArgumentException e) { // The MQTT library can throw this if the clientHandle is invalid, apparently
            return false;
        }
    }

    private boolean isConnecting() {
        if (connectingLock.tryAcquire()) {
            Timber.d("MQTT not current connecting");
            connectingLock.release();
            return false;
        } else {
            Timber.d("MQTT already connecting");
            return true;
        }
    }

    private static EndpointState getState() {
        return state;
    }

    @Override
    public void onDestroy() {
        disconnect();
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
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        if (preferences.getMode() != MessageProcessorEndpointMqtt.MODE_ID) {
            return;
        }
        if (preferences.getPreferenceKey(R.string.preferenceKeyMqttProtocolLevel).equals(key) ||
                preferences.getPreferenceKey(R.string.preferenceKeyHost).equals(key) ||
                preferences.getPreferenceKey(R.string.preferenceKeyPassword).equals(key) ||
                preferences.getPreferenceKey(R.string.preferenceKeyPort).equals(key) ||
                preferences.getPreferenceKey(R.string.preferenceKeyClientId).equals(key) ||
                preferences.getPreferenceKey(R.string.preferenceKeyTLS).equals(key) ||
                preferences.getPreferenceKey(R.string.preferenceKeyTLSCaCrt).equals(key) ||
                preferences.getPreferenceKey(R.string.preferenceKeyTLSClientCrt).equals(key) ||
                preferences.getPreferenceKey(R.string.preferenceKeyTLSClientCrtPassword).equals(key) ||
                preferences.getPreferenceKey(R.string.preferenceKeyWS).equals(key) ||
                preferences.getPreferenceKey(R.string.preferenceKeyDeviceId).equals(key)
        ) {
            Timber.d("MQTT preferences changed. Reconnecting to broker. ThreadId: %s", Thread.currentThread());
            reconnect();
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

class AlreadyConnectingToBrokerException extends Exception {
}
