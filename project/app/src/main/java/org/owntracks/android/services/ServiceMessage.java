package org.owntracks.android.services;

import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.util.Log;
import android.util.LongSparseArray;

import org.greenrobot.eventbus.Subscribe;
import org.owntracks.android.App;
import org.owntracks.android.messages.MessageBase;
import org.owntracks.android.messages.MessageCard;
import org.owntracks.android.messages.MessageClear;
import org.owntracks.android.messages.MessageCmd;
import org.owntracks.android.messages.MessageLocation;
import org.owntracks.android.messages.MessageTransition;
import org.owntracks.android.messages.MessageUnknown;
import org.owntracks.android.messages.MessageWaypoints;
import org.owntracks.android.support.Events;
import org.owntracks.android.support.IncomingMessageProcessor;
import org.owntracks.android.support.OutgoingMessageProcessor;
import org.owntracks.android.support.Preferences;
import org.owntracks.android.support.widgets.Toasts;
import org.owntracks.android.support.interfaces.ProxyableService;

import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import timber.log.Timber;


public class ServiceMessage implements ProxyableService, IncomingMessageProcessor {
    private static final String TAG = "ServiceMessage";
    public static final String RECEIVER_ACTION_CLEAR_CONTACT_EXTRA_TOPIC = "RECEIVER_ACTION_CLEAR_CONTACT_EXTRA_TOPIC" ;
    public static final String RECEIVER_ACTION_CLEAR_CONTACT = "RECEIVER_ACTION_CLEAR_CONTACT";

    private ThreadPoolExecutor incomingMessageProcessorExecutor;
    private ThreadPoolExecutor outgoingMessageProcessorExecutor;
    private OutgoingMessageProcessor outgoingMessageProcessor;

    private ServiceProxy context;

    public void reconnect() {
        // TODO: move to dispatcher
        //if(endpoint instanceof StatefulServiceMessageEndpoint)
        //    StatefulServiceMessageEndpoint.class.cast(endpoint).reconnect();
    }

    public void disconnect() {
        //TODO: move to dispather
        //if(endpoint instanceof StatefulServiceMessageEndpoint)
        //    StatefulServiceMessageEndpoint.class.cast(endpoint).disconnect();
    }

    public ServiceProxy getContext() {
        return context;
    }

    public enum EndpointState {
        INITIAL,
        IDLE,
        CONNECTING,
        CONNECTED,
        DISCONNECTING,
        DISCONNECTED,
        DISCONNECTED_USERDISCONNECT,
        ERROR,
        ERROR_DATADISABLED,
        ERROR_CONFIGURATION;

        public String getLabel(Context context) {
            Resources res = context.getResources();
            int resId = res.getIdentifier(this.name(), "string", context.getPackageName());
            if (0 != resId) {
                return (res.getString(resId));
            }
            return (name());
        }

        public boolean isErrorState() {
            return this == ERROR || this == ERROR_DATADISABLED || this == ERROR_CONFIGURATION;
        }
    }



    @Override
    public void onCreate(ServiceProxy c) {
        this.context = c;
        this.incomingMessageProcessorExecutor = new ThreadPoolExecutor(2,2,1,  TimeUnit.MINUTES,new LinkedBlockingQueue<Runnable>());
        this.outgoingMessageProcessorExecutor = new ThreadPoolExecutor(2,2,1,  TimeUnit.MINUTES,new LinkedBlockingQueue<Runnable>());
        this.loadOutgoingMessageProcessor(Preferences.getModeId());

        onEndpointStateChanged(EndpointState.INITIAL);
    }

    private void loadOutgoingMessageProcessor(int mode){
        Timber.v("mode:%s", mode);
        if(outgoingMessageProcessorExecutor != null) {
            outgoingMessageProcessorExecutor.purge();
        }

        if(outgoingMessageProcessor != null) {
            outgoingMessageProcessor.onDestroy();
        }


            Timber.v("instantiating new outgoingMessageProcessorExecutor");
        switch (mode) {
            case App.MODE_ID_HTTP_PRIVATE:
                this.outgoingMessageProcessor = new ServiceMessageHttp();
            case App.MODE_ID_MQTT_PRIVATE:
            case App.MODE_ID_MQTT_PUBLIC:
            default:
                this.outgoingMessageProcessor = new ServiceEndpointMqtt();

        }
        this.outgoingMessageProcessor.onAssociate(this);
    }



    @Override
    public void onDestroy() {
        Timber.d("onDestroy");
        if(outgoingMessageProcessor != null)
            outgoingMessageProcessor.onDestroy();
    }

    @Override
    public void onStartCommand(Intent intent) {

    }

    @Subscribe
    public void onEvent(Events.Dummy event) {

    }

    @Subscribe
    public void onEvent(Events.ModeChanged event) {
        loadOutgoingMessageProcessor(Preferences.getModeId());
    }

    private LongSparseArray<MessageBase> outgoingQueue = new LongSparseArray<>();

    void sendMessage(MessageBase message) {
        Timber.v("executing message on outgoingMessageProcessor");
        message.setOutgoingProcessor(outgoingMessageProcessor);
        this.outgoingMessageProcessorExecutor.execute(message);

    }
//    void sendMessage(MessageBase message) {
//        Timber.v("endpoint:%s, message:%s",endpoint, message);
//
//        message.setOutgoing();
//        try {
//
//            if(endpoint == null) {
//                Timber.e("no endpoint, creating on demand");
//                endpoint = instantiateEndpoint(Preferences.getModeId());
//            }
//
//            if(!endpoint.isReady()) {
//                Timber.e("endpoint is not ready: %s", endpoint);
//                endpoint.probe();
//                return;
//            }
//
//            if(endpoint.sendMessage(message)) {
//                this.onMessageQueued(message);
//            }
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
//
//    }



     void onMessageDelivered(Long messageId) {
        MessageBase m = outgoingQueue.get(messageId);
        outgoingQueue.remove(messageId);

        if(m == null) {
            Log.e(TAG, "onMessageDelivered()- messageId:"+messageId + ", error: called for unqueued message");
        } else {
            Log.v(TAG, "onMessageDelivered()-  messageId:" + m.getMessageId()+", queueLength:"+outgoingQueue.size());
            if(m instanceof MessageLocation) {
                App.getEventBus().post(m);
            }
        }
    }

    private void onMessageQueued(MessageBase m) {
        outgoingQueue.put(m.getMessageId(), m);

        Log.v(TAG, "onMessageQueued()- messageId:" + m.getMessageId()+", queueLength:"+outgoingQueue.size());
        if(m instanceof MessageLocation && MessageLocation.REPORT_TYPE_USER.equals(MessageLocation.class.cast(m).getT()))
            Toasts.showMessageQueued();
    }

    public void onMessageDeliveryFailed(Long messageId) {

        MessageBase m = outgoingQueue.get(messageId);
        outgoingQueue.remove(messageId);

        if(m == null) {
            Timber.e("type:base, messageId:%s, error: called for unqueued message", messageId);
        } else {
            Timber.e("type:base, messageId:%s, queueLength:%s", messageId, outgoingQueue.size());
            if(m.getOutgoingTTL() > 0)  {
                Timber.e("type:base, messageId:%s, action: requeued",m.getMessageId() );
                sendMessage(m);
            } else {
                Timber.e("type:base, messageId:%s, action: discarded due to expired ttl",m.getMessageId() );
            }
        }
    }

    public void onMessageReceived(MessageBase message) {
        message.setIncomingProcessor(this);
        message.setIncoming();
        incomingMessageProcessorExecutor.execute(message);
    }

    public void onEndpointStateChanged(EndpointState newState) {
        App.getEventBus().postSticky(new Events.EndpointStateChanged(newState));
    }

    public void onEndpointStateChanged(EndpointState newState,  String message) {
        App.getEventBus().postSticky(new Events.EndpointStateChanged(newState, message));
    }

    public void onEndpointStateChanged(EndpointState newState, Exception e) {
        Timber.v("new state:%s",newState);
        App.getEventBus().postSticky(new Events.EndpointStateChanged(newState, e));
    }


    @Override
    public void processIncomingMessage(MessageBase message) {
        Timber.v("type:base, key:%s", message.getContactKey());
    }

    public void processIncomingMessage(MessageUnknown message) {
        Timber.v("type:unknown, key:%s", message.getContactKey());
    }

    @Override
    public void processIncomingMessage(MessageClear message) {
        App.getContactsRepo().remove(message.getContactKey());
    }


    @Override
    public void processIncomingMessage(MessageLocation message) {
        App.getContactsRepo().update(message.getContactKey(),message);

    }

    @Override
    public void processIncomingMessage(MessageCard message) {
        App.getContactsRepo().update(message.getContactKey(),message);
    }

    @Override
    public void processIncomingMessage(MessageCmd message) {
        if(!Preferences.getRemoteCommand()) {
            Timber.e("remote commands are disabled");
            return;
        }


        if(!Preferences.getPubTopicCommands().equals(message.getTopic())) {
            Timber.e("cmd message received on wrong topic");
            return;
        }

        switch (message.getAction()) {
            case MessageCmd.ACTION_REPORT_LOCATION:
                ServiceProxy.getServiceLocator().reportLocationResponse();
                break;
            case MessageCmd.ACTION_WAYPOINTS:
                ServiceProxy.getServiceLocator().publishWaypointsMessage();
                break;
            case MessageCmd.ACTION_SET_WAYPOINTS:
                MessageWaypoints w = message.getWaypoints();
                if(w != null)
                    Preferences.importWaypointsFromJson(w.getWaypoints());
                break;
            case MessageCmd.ACTION_SET_CONFIGURATION:
                Preferences.importFromMessage(message.getConfiguration());
                break;
        }
    }

    @Override
    public void processIncomingMessage(MessageTransition message) {
        ServiceProxy.getServiceNotification().processMessage(message);
    }


}
