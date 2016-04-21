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
import org.owntracks.android.support.GeocodingProvider;
import org.owntracks.android.support.IncomingMessageProcessor;
import org.owntracks.android.support.MessageWaypointCollection;
import org.owntracks.android.support.Preferences;
import org.owntracks.android.support.interfaces.MessageReceiver;
import org.owntracks.android.support.interfaces.MessageSender;
import org.owntracks.android.support.interfaces.ServiceMessageEndpoint;
import org.owntracks.android.support.interfaces.StatefulServiceMessageEndpoint;

import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;



public class ServiceMessage implements ProxyableService, MessageSender, MessageReceiver, IncomingMessageProcessor {
    private static final String TAG = "ServiceMessage";

    private static ServiceMessageEndpoint endpoint;
    private ThreadPoolExecutor pool;

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
        this.pool= new ThreadPoolExecutor(2,2,1,  TimeUnit.MINUTES,new LinkedBlockingQueue<Runnable>());
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
            endpoint = (ServiceMessageMqtt)ServiceProxy.instantiateService(ServiceProxy.SERVICE_MESSAGE_MQTT);
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
    @Override
    public void sendMessage(MessageBase message) {
        message.setOutgoing();

        if(endpoint == null) {
            Log.e(TAG, "sendMessage called without a endpoint instance");
            return;
        }

        Log.v(TAG, "sendMessage with endpoint " + endpoint);
        endpoint.sendMessage(message);
    }

    @Override
    public void onMessageDelivered(MessageBase message) {
        Log.v(TAG, "message delivered: " + message + " " + message.isOutgoing());

        if(message instanceof MessageLocation) {
            de.greenrobot.event.EventBus.getDefault().post(message);
        }
    }

    @Override
    public void onMessageQueued(MessageBase message) {
        Log.v(TAG, "message delivered: " + message);

    }

    @Override
    public void onMessageReceived(MessageBase message) {
        Log.v(TAG, "onMessageReceived(): " + message);
        message.setIncomingProcessor(this);
        message.setIncoming();
        pool.execute(message);

    }

    @Override
    public void processMessage(MessageBase message) {
        Log.v(TAG, "processMessage MessageBase (" + message.getContactKey()+")");
    }

    public void processMessage(MessageUnknown message) {
        Log.v(TAG, "processMessage MessageUnknown (" + message.getContactKey()+")");
    }


    @Override
    public void processMessage(MessageLocation message) {
        Log.v(TAG, "processMessage MessageLocation (" + message.getContactKey()+")");

        GeocodingProvider.resolve(message);
        FusedContact c = App.getFusedContact(message.getContactKey());

        if (c == null) {
            c = new FusedContact(message.getContactKey());
            c.setMessageLocation(message);
            App.addFusedContact(c);
        } else {
            c.setMessageLocation(message);
            App.updateFusedContact(c);
        }
    }

    @Override
    public void processMessage(MessageCard message) {
        Log.v(TAG, "processMessage MessageCard (" + message.getContactKey() + ")");
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
    public void processMessage(MessageCmd message) {
        Log.v(TAG, "processMessage MessageCmd (" + message.getContactKey() + ")");
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
    public void processMessage(MessageTransition message) {
        Log.v(TAG, "processMessage MessageTransition (" + message.getContactKey() + ")");
        ServiceProxy.getServiceNotification().processMessage(message);
    }

    public void processMessage(MessageConfiguration message) {
        Log.v(TAG, "processMessage MessageConfiguration (" + message.getContactKey()+")");
        if(!Preferences.getRemoteConfiguration())
            return;

        Preferences.importFromMessage(message);
    }


    public static String getEndpointStateAsString() {
        return endpoint != null ? endpoint.getStateAsString() : App.getContext().getString(R.string.noEndpointConfigured);
    }
}
