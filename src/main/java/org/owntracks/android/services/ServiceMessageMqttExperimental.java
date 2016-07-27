package org.owntracks.android.services;

import android.content.Intent;
import android.support.v4.util.Pair;
import android.util.Log;

import org.eclipse.paho.android.service.MqttAndroidClient;
import org.eclipse.paho.client.mqttv3.DisconnectedBufferOptions;
import org.eclipse.paho.client.mqttv3.IMqttActionListener;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.IMqttMessageListener;
import org.eclipse.paho.client.mqttv3.IMqttToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttCallbackExtended;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.MqttPersistenceException;
import org.owntracks.android.R;
import org.owntracks.android.messages.MessageBase;
import org.owntracks.android.messages.MessageCmd;
import org.owntracks.android.messages.MessageEvent;
import org.owntracks.android.messages.MessageLocation;
import org.owntracks.android.messages.MessageTransition;
import org.owntracks.android.messages.MessageUnknown;
import org.owntracks.android.messages.MessageWaypoint;
import org.owntracks.android.messages.MessageWaypoints;
import org.owntracks.android.services.ServiceMessage.EndpointState;
import org.owntracks.android.support.Events;
import org.owntracks.android.support.OutgoingMessageProcessor;
import org.owntracks.android.support.PausableThreadPoolExecutor;
import org.owntracks.android.support.Preferences;
import org.owntracks.android.support.SocketFactory;
import org.owntracks.android.support.StatisticsProvider;
import org.owntracks.android.support.interfaces.MessageReceiver;
import org.owntracks.android.support.interfaces.MessageSender;
import org.owntracks.android.support.interfaces.StatefulServiceMessageEndpoint;
import org.owntracks.android.support.interfaces.StatelessMessageEndpoint;
import org.owntracks.android.support.receiver.Parser;
import org.owntracks.android.services.ServiceMessage;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.SocketTimeoutException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import de.greenrobot.event.EventBus;

import static org.owntracks.android.services.ServiceMessage.EndpointState.*;

public class ServiceMessageMqttExperimental implements ProxyableService, OutgoingMessageProcessor, RejectedExecutionHandler, StatefulServiceMessageEndpoint{

    private static final String TAG = "ServiceMessageMqttE";
    private PausableThreadPoolExecutor pubPool;
    private MessageSender messageSender;
    private MessageReceiver messageReceiver;
    private ServiceProxy context;
    private Throwable error;
    private MqttAndroidClient client;
    private DisconnectedBufferOptions disconnectedBufferOptions;

    @Override
    public void onCreate(ServiceProxy c) {
        Log.v(TAG, "loaded experimental MQTT backend");
        this.context = c;


        this.disconnectedBufferOptions = new DisconnectedBufferOptions();
        this.disconnectedBufferOptions.setBufferEnabled(true);
        this.disconnectedBufferOptions.setBufferSize(1024);
        this.disconnectedBufferOptions.setPersistBuffer(false);
        this.disconnectedBufferOptions.setDeleteOldestMessages(true);

        initPausedPubPool();
        connect();


    }

    private MqttConnectOptions getConnectOptions() {
        MqttConnectOptions options = new MqttConnectOptions();

        try {
            // SESSION OPTIONS
            options.setAutomaticReconnect(true);
            options.setConnectionTimeout(10);
            options.setMaxInflight(15);
            options.setKeepAliveInterval(Preferences.getKeepalive());

            // AUTHENTICATION
            if (Preferences.getAuth()) {
                options.setPassword(Preferences.getPassword().toCharArray());
                options.setUserName(Preferences.getUsername());
            }

            // TLS
            if (Preferences.getTls()) {
                String tlsCaCrt = Preferences.getTlsCaCrtName();
                String tlsClientCrt = Preferences.getTlsClientCrtName();

                SocketFactory.SocketFactoryOptions socketFactoryOptions = new SocketFactory.SocketFactoryOptions();

                if (tlsCaCrt.length() > 0) {
                    try {
                        socketFactoryOptions.withCaInputStream(context.openFileInput(tlsCaCrt));
                    } catch (FileNotFoundException e) {
                        e.printStackTrace();
                    }
                }

                if (tlsClientCrt.length() > 0)						{
                    try {
                        socketFactoryOptions.withClientP12InputStream(context.openFileInput(tlsClientCrt)).withClientP12Password(Preferences.getTlsClientCrtPassword());
                    } catch (FileNotFoundException e1) {
                        e1.printStackTrace();
                    }
                }


                options.setSocketFactory(new SocketFactory(socketFactoryOptions));
            }



            options.setCleanSession(Preferences.getCleanSession());

            return options;

        } catch (Exception e) { // Catch paho and socket factory exceptions
            Log.e(TAG, e.toString());
            e.printStackTrace();
            changeState(e);
            return null;
        }
    }

    private String getConnectClientId() {
        return Preferences.getClientId(true);
    }

    private String getConnectUri() {
        String prefix;

        if (Preferences.getTls()) {
            if (Preferences.getWs())
                prefix = "wss";
            else
                prefix = "ssl";
        } else if (Preferences.getWs()) {
                prefix = "ws";
        } else {
            prefix = "tcp";
        }

        return prefix + "://" + Preferences.getHost() + ":" + Preferences.getPort();
    }


    private void connect() {
        if(!Preferences.canConnect()) {
            changeState(DISCONNECTED_CONFIGINCOMPLETE);
            return;
        }



        try {
            this.client = new MqttAndroidClient(context, getConnectUri(), getConnectClientId());
            this.client.setCallback(iClientCallback);
            changeState(CONNECTING);
            client.connect(getConnectOptions(), null, iCallbackConnection);
        } catch (MqttException e) {
            changeState(e);
            e.printStackTrace();
        }
    }

    private MqttCallbackExtended iClientCallback = new MqttCallbackExtended() {
        @Override
        public void connectComplete(boolean reconnect, String serverURI) {
            Log.v(TAG, "connectComplete reconnect:" + reconnect);
            if(reconnect)
                subscribe();
        }

        @Override
        public void deliveryComplete(IMqttDeliveryToken token) {
            Log.v(TAG, "deliveryComplete");
        }

        @Override
        public void connectionLost(Throwable cause) {
            Log.v(TAG, "connectionLost");
            if(cause != null)
                cause.printStackTrace();
            changeState(cause);
        }

        @Override
        public void messageArrived(String topic, MqttMessage message) throws Exception {
            Log.v(TAG, "messageArrived");
            try {
                MessageBase m = Parser.deserializeSync(message.getPayload());
                if(!m.isValidMessage()) {
                    Log.e(TAG, "message failed validation: " + message.getPayload());
                    return;
                }

                m.setTopic(getBaseTopic(m, topic));
                m.setRetained(message.isRetained());
                m.setQos(message.getQos());
                messageReceiver.onMessageReceived(m);
                if(m instanceof MessageUnknown) {
                    Log.v(TAG, "unknown message topic: " + topic +" payload: " + new String(message.getPayload()));
                }

            } catch (Exception e) {

                Log.e(TAG, "JSON parser exception for message: " + new String(message.getPayload()));
                Log.e(TAG, e.getMessage() +" " + e.getCause());

                e.printStackTrace();
            }
        }

    };

    private void subscribe() {

        String[] topics = getSubTopics();
        int qos[] = getSubTopicsQos(topics);
        Log.v(TAG, "subscribe: " + Arrays.toString(topics));

        try {
            IMqttToken subToken = client.subscribe(topics, qos);
            subToken.setActionCallback(iCallbackSubscription);
        } catch (MqttException e) {
            e.printStackTrace();
        }

    }

    private IMqttActionListener iCallbackConnection = new IMqttActionListener() {

        @Override
        public void onSuccess(IMqttToken asyncActionToken) {
            changeState(CONNECTED);
            client.setBufferOpts(disconnectedBufferOptions);
            subscribe();
        }

        @Override
        public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
            // Something went wrong e.g. connection timeout or firewall problems
            Log.d(TAG, "ConnCallback onFailure");
            exception.printStackTrace();
            changeState(exception);
        }

    };

    private IMqttActionListener iCallbackSubscription = new IMqttActionListener() {

        @Override
        public void onSuccess(IMqttToken asyncActionToken) {
            Log.v(TAG, "SubCallback onSuccess");
        }

        @Override
        public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
            Log.v(TAG, "SubCallback onFailure");
        }
    };



    public String[] getSubTopics() {
        List<String> topics = new ArrayList<>();
        String subTopicBase = Preferences.getSubTopic();

        if(subTopicBase.endsWith("#")) { // wildcard sub will match everything anyway
            topics.add(subTopicBase);
        } else {

            topics.add(subTopicBase);
            if(Preferences.getInfo())
                topics.add(subTopicBase + Preferences.getPubTopicInfoPart());

            if (!Preferences.isModeMqttPublic()) {
                topics.add(Preferences.getPubTopicBase(true) + Preferences.getPubTopicCommandsPart());
                topics.add(subTopicBase + Preferences.getPubTopicEventsPart());
                topics.add(subTopicBase + Preferences.getPubTopicWaypointsPart());
            }


        }

        return topics.toArray(new String[topics.size()]);

    }
    private int[] getSubTopicsQos(String[] topics) {
        int[] qos = new int[topics.length];
        Arrays.fill(qos, 2);
        return qos;
    }


    @Override
    public void setMessageSenderCallback(MessageSender callback) {
        this.messageSender = callback;
    }

    @Override
    public void setMessageReceiverCallback(MessageReceiver callback) {
        this.messageReceiver = callback;
    }


    private static EndpointState state = EndpointState.IDLE;


    public static EndpointState getState() {
        return state;
    }

    @Override
    public String getConnectionState() {
        int id;
        switch (getState()) {
            case IDLE:
                id = R.string.connectivityIdle;
                break;
            case CONNECTED:
                id = R.string.connectivityConnected;
                break;
            case CONNECTING:
                id = R.string.connectivityConnecting;
                break;
            case DISCONNECTED_DATADISABLED:
                id = R.string.connectivityDisconnectedDataDisabled;
                break;
            case DISCONNECTED_ERROR:
                id = R.string.error;
                break;
            case DISCONNECTED_CONFIGINCOMPLETE:
                id = R.string.connectivityDisconnectedConfigIncomplete;
                break;
            default:
                id = R.string.connectivityDisconnected;

        }
        return context.getString(id);
    }


    @Override
    public void onDestroy() {

    }

    @Override
    public void onStartCommand(Intent intent, int flags, int startId) {

    }
    private void initPausedPubPool() {
        Log.v(TAG, "Executor initPausedPubPool with new paused queue");
        if(pubPool != null && !pubPool.isShutdown()) {
            Log.v(TAG, "Executor shutting down existing executor " + pubPool);
            pubPool.shutdownNow();
        }
        this.pubPool = new PausableThreadPoolExecutor(1,1,1, TimeUnit.MINUTES,new LinkedBlockingQueue<Runnable>());
        this.pubPool.setRejectedExecutionHandler(this);
        Log.v(TAG, "Executor created new executor instance: " + pubPool);
        pubPool.resume();
    }



    @Override
    public void onEvent(Events.Dummy event) {

    }


    private void publishMessage(MessageBase message) {
        //Log.v(TAG, "publishMessage: " + message + ", q size: " + pubPool.getQueue().size());
        try {
            MqttMessage m = new MqttMessage();
            m.setPayload(Parser.serializeSync(message).getBytes());
            m.setQos(message.getQos());
            m.setRetained(message.getRetained());

            Log.v(TAG, "buffered messages " + client.getBufferedMessageCount());


            IMqttDeliveryToken pubToken = client.publish(message.getTopic(), m);
            pubToken.setActionCallback(iCallbackPublish);
            pubToken.setUserContext(message);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    private IMqttActionListener iCallbackPublish = new IMqttActionListener() {

        @Override
        public void onSuccess(IMqttToken asyncActionToken) {
            Log.v(TAG, "PubCallback onSuccess");
            messageSender.onMessageDelivered(MessageBase.class.cast(asyncActionToken.getUserContext()).getMessageId());

        }

        @Override
        public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
            Log.v(TAG, "PubCallback onFailure");
            exception.printStackTrace();
        }
    };


    @Override
    public void processOutgoingMessage(MessageBase message) {
        message.setTopic(Preferences.getPubTopicBase());
        publishMessage(message);
    }

    @Override
    public void processOutgoingMessage(MessageCmd message) {
        message.setTopic(Preferences.getPubTopicCommands());
        publishMessage(message);
    }

    @Override
    public void processOutgoingMessage(MessageEvent message) {
        message.setTopic(Preferences.getPubTopicEvents());
        publishMessage(message);
    }

    @Override
    public void processOutgoingMessage(MessageLocation message) {
        message.setTopic(Preferences.getPubTopicLocations());
        message.setQos(Preferences.getPubQosLocations());
        message.setRetained(Preferences.getPubRetainLocations());

        publishMessage(message);
    }

    @Override
    public void processOutgoingMessage(MessageTransition message) {
        message.setTopic(Preferences.getPubTopicEvents());
        publishMessage(message);
    }

    @Override
    public void processOutgoingMessage(MessageWaypoint message) {
        message.setTopic(Preferences.getPubTopicWaypoints());
        publishMessage(message);
    }

    @Override
    public void processOutgoingMessage(MessageWaypoints message) {
        message.setTopic(Preferences.getPubTopicWaypoints());
        publishMessage(message);
    }

    @Override
    public void rejectedExecution(Runnable r, ThreadPoolExecutor executor) {

    }

    private void changeState(Throwable e) {
        error = e;
        changeState(DISCONNECTED_ERROR, e);
    }

    private void changeState(EndpointState newState) {
        changeState(newState, null);
    }

    private void changeState(EndpointState newState, Throwable e) {
        //Log.d(TAG, "ServiceMessageMqtt state changed to: " + newState);
        state = newState;
        EventBus.getDefault().postSticky(new Events.EndpointStateChanged(newState, e));
    }


    @Override
    public boolean sendMessage(MessageBase message) {
        Log.v(TAG, "sendMessage base: " + message + " " + message.getClass());


        message.setOutgoingProcessor(this);
        Log.v(TAG, "enqueueing message to pubPool. running: " + pubPool.isRunning() + ", q size:" + pubPool.getQueue().size());
        StatisticsProvider.setInt(StatisticsProvider.SERVICE_BROKER_QUEUE_LENGTH, pubPool.getQueueLength());

        this.pubPool.queue(message);
        return true;
    }

    private String getBaseTopic(MessageBase message, String topic){

        if (message.getBaseTopicSuffix() != null && topic.endsWith(message.getBaseTopicSuffix())) {
            return topic.substring(0, (topic.length() - message.getBaseTopicSuffix().length()));
        } else {
            return topic;
        }
    }


    @Override
    public void reconnect() {
        disconnect();
        connect();

    }

    @Override
    public void disconnect() {
        if(this.client != null && this.client.isConnected())
            try {
                this.client.disconnect();
            } catch (MqttException e) {
                e.printStackTrace();
            }

    }
}
