package org.owntracks.android.services;

import android.content.Intent;
import android.util.Log;

import org.owntracks.android.App;
import org.owntracks.android.R;
import org.owntracks.android.messages.MessageBase;
import org.owntracks.android.messages.MessageCard;
import org.owntracks.android.messages.MessageCmd;
import org.owntracks.android.messages.MessageConfiguration;
import org.owntracks.android.messages.MessageLocation;
import org.owntracks.android.messages.MessageTransition;
import org.owntracks.android.messages.MessageUnknown;
import org.owntracks.android.model.FusedContact;
import org.owntracks.android.support.Events;
import org.owntracks.android.support.IncomingMessageProcessor;
import org.owntracks.android.support.MessageWaypointCollection;
import org.owntracks.android.support.Preferences;
import org.owntracks.android.support.interfaces.MessageReceiver;
import org.owntracks.android.support.interfaces.MessageSender;
import org.owntracks.android.support.interfaces.ServiceMessageEndpoint;
import org.owntracks.android.support.interfaces.StatefulServiceMessageEndpoint;

import java.util.HashMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;



public class ServiceMessage implements ProxyableService, MessageSender, MessageReceiver, IncomingMessageProcessor {
    private static final String TAG = "ServiceMessage";

    private static ServiceMessageEndpoint endpoint;
    private ThreadPoolExecutor incomingMessageProcessorExecutor;

    public void reconnect() {
        if(endpoint instanceof StatefulServiceMessageEndpoint)
            StatefulServiceMessageEndpoint.class.cast(endpoint).reconnect();
    }

    public void disconnect() {
        if(endpoint instanceof StatefulServiceMessageEndpoint)
            StatefulServiceMessageEndpoint.class.cast(endpoint).disconnect();
    }

    public enum EndpointState {
        INITIAL, IDLE, CONNECTING, CONNECTED, DISCONNECTING, DISCONNECTED, DISCONNECTED_USERDISCONNECT, DISCONNECTED_DATADISABLED, DISCONNECTED_CONFIGINCOMPLETE, EndpointState, DISCONNECTED_ERROR
    }



    @Override
    public void onCreate(ServiceProxy c) {
        Log.v(TAG, "onCreate()");
        this.incomingMessageProcessorExecutor = new ThreadPoolExecutor(2,2,1,  TimeUnit.MINUTES,new LinkedBlockingQueue<Runnable>());
        onModeChanged(Preferences.getModeId());
    }



    private void onModeChanged(int mode) {
        Log.v(TAG, "onModeChanged: " + mode);
        if(endpoint != null)
            ServiceProxy.stopService((ProxyableService) endpoint);

        if(mode == App.MODE_ID_HTTP_PRIVATE) {
            Log.v(TAG, "loading http backend");
            endpoint = (ServiceMessageHttp)ServiceProxy.instantiateService(ServiceProxy.SERVICE_MESSAGE_HTTP);
        } else {
            Log.v(TAG, "loading mqtt backend");
            endpoint = (ServiceMessageMqttExperimental)ServiceProxy.instantiateService(ServiceProxy.SERVICE_MESSAGE_MQTT);
        }

        Log.v(TAG, "endpoint instance: " + endpoint);
        if(endpoint == null) {
            Log.e(TAG, "unable to instantiate service for mode " + mode);
            return;
        }

        endpoint.setMessageReceiverCallback(this);
        endpoint.setMessageSenderCallback(this);
        ServiceProxy.getServiceNotification().updateNotificationOngoing();
    }

    @Override
    public void onDestroy() {

    }

    @Override
    public void onStartCommand(Intent intent, int flags, int startId) {

    }

    @SuppressWarnings("unused")
    @Override
    public void onEvent(Events.Dummy event) {

    }

    @SuppressWarnings("unused")
    public void onEvent(Events.ModeChanged event) {
        onModeChanged(Preferences.getModeId());
    }
    // ServiceMessage.MessageSender interface

    HashMap<Long, MessageBase> outgoingQueue = new HashMap<>();

    @Override
    public void sendMessage(MessageBase message) {
        Log.v(TAG, "sendMessage() - endpoint:" + endpoint);

        message.setOutgoing();

        if(endpoint == null) {
            return;
        }

        if(endpoint.sendMessage(message)) {
            this.onMessageQueued(message);
        }
    }



    @Override
    public void onMessageDelivered(Long messageId) {

        MessageBase m = outgoingQueue.remove(messageId);
        if(m == null) {
            Log.e(TAG, "onMessageDelivered()- messageId:"+messageId + ", error: called for unqueued message");
        } else {
            Log.v(TAG, "onMessageDelivered()-  messageId:" + m.getMessageId()+", queueLength:"+outgoingQueue.size());
            if(m instanceof MessageLocation) {
                de.greenrobot.event.EventBus.getDefault().post(m);
            }
        }
        Log.v(TAG, "onMessageDelivered()-  queueKeys:" + outgoingQueue.keySet().toString());
    }

    private void onMessageQueued(MessageBase m) {
        outgoingQueue.put(m.getMessageId(), m);
        Log.v(TAG, "onMessageQueued()- messageId:" + m.getMessageId()+", queueLength:"+outgoingQueue.size());

    }

    @Override
    public void onMessageDeliveryFailed(Long messageId) {

        MessageBase m = outgoingQueue.remove(messageId);
        if(m == null) {
            Log.e(TAG, "onMessageDeliveryFailed()- messageId:"+messageId + ", error: called for unqueued message");
        } else {
            Log.v(TAG, "onMessageDeliveryFailed()- messageId:" + m.getMessageId()+", queueLength:"+outgoingQueue.size());
            if(m.getOutgoingTTL() > 0)  {
                Log.v(TAG, "onMessageDeliveryFailed()- messageId:" + m.getMessageId()+", action: requeued");
                sendMessage(m);
            } else {
                Log.v(TAG, "onMessageDeliveryFailed()- messageId:" + m.getMessageId()+", action: discarded due to expired ttl");

            }
        }
    }

    @Override
    public void onMessageReceived(MessageBase message) {
        message.setIncomingProcessor(this);
        message.setIncoming();
        incomingMessageProcessorExecutor.execute(message);
    }

    @Override
    public void processIncomingMessage(MessageBase message) {
        Log.v(TAG, "processIncomingMessage MessageBase (" + message.getContactKey()+")");
    }

    public void processIncomingMessage(MessageUnknown message) {
        Log.v(TAG, "processIncomingMessage MessageUnknown (" + message.getContactKey()+")");
    }


    @Override
    public void processIncomingMessage(MessageLocation message) {
        Log.v(TAG, "processIncomingMessage MessageLocation (" + message.getContactKey()+")");

        //GeocodingProvider.resolve(message);
        FusedContact c = App.getFusedContact(message.getContactKey());

        if (c == null) {
            c = new FusedContact(message.getContactKey());
            c.setMessageLocation(message);
            App.addFusedContact(c);
        } else {

            //Only update contact with new location if the location message is different from the one that is already set
            if(c.setMessageLocation(message))
                App.updateFusedContact(c);
        }
    }

    @Override
    public void processIncomingMessage(MessageCard message) {
        Log.v(TAG, "processIncomingMessage MessageCard (" + message.getContactKey() + ")");
        FusedContact c = App.getFusedContact(message.getContactKey());

        if (c == null) {
            c = new FusedContact(message.getContactKey());
            c.setMessageCard(message);
            App.addFusedContact(c);
        } else {
            c.setMessageCard(message);
            App.updateFusedContact(c);
        }
    }

    @Override
    public void processIncomingMessage(MessageCmd message) {
        Log.v(TAG, "processIncomingMessage MessageCmd (" + message.getContactKey() + ")");
        if(!Preferences.getRemoteCommand()) {
            Log.e(TAG, "remote commands are disabled");
            return;
        }

        if(message.getAction().equals(MessageCmd.ACTION_REPORT_LOCATION) ) {
            ServiceProxy.getServiceLocator().reportLocationResponse();
        } else if(message.getAction().equals(MessageCmd.ACTION_WAYPOINTS)) {
            ServiceProxy.getServiceApplication().publishWaypointsMessage();
        } else if(message.getAction().equals(MessageCmd.ACTION_SET_WAYPOINTS)) {
            Log.v(TAG, "ACTION_SET_WAYPOINTS received");
            MessageWaypointCollection waypoints = message.getWaypoints();
            Log.v(TAG, "waypoints: " + waypoints);
            if(waypoints == null)
                return;

            Preferences.importWaypointsFromJson(waypoints);

        }

    }

    @Override
    public void processIncomingMessage(MessageTransition message) {
        Log.v(TAG, "processIncomingMessage MessageTransition (" + message.getContactKey() + ")");
        ServiceProxy.getServiceNotification().processMessage(message);
    }

    public void processIncomingMessage(MessageConfiguration message) {
        Log.v(TAG, "processIncomingMessage MessageConfiguration (" + message.getContactKey()+")");
        if(!Preferences.getRemoteConfiguration())
            return;

        Preferences.importFromMessage(message);
    }


    public static String getEndpointStateAsString() {
        return endpoint != null ? endpoint.getStateAsString() : App.getContext().getString(R.string.noEndpointConfigured);
    }
}
