package org.owntracks.android.services;

import android.bluetooth.BluetoothAdapter;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Build;
import android.os.RemoteException;
import android.util.Log;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.eventbus.EventBus;

import org.altbeacon.beacon.BeaconConsumer;
import org.altbeacon.beacon.BeaconManager;
import org.altbeacon.beacon.BeaconParser;
import org.altbeacon.beacon.Identifier;
import org.altbeacon.beacon.MonitorNotifier;
import org.altbeacon.beacon.Region;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.owntracks.android.App;
import org.owntracks.android.db.Dao;
import org.owntracks.android.db.Waypoint;
import org.owntracks.android.db.WaypointDao;
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

import java.util.HashMap;
import java.util.List;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;



public class ServiceMessage implements ProxyableService, MessageSender, MessageReceiver, IncomingMessageProcessor {
    private static final String TAG = "ServiceMessage";

    private Context context;
    private ServiceMessageEndpoint endpoint;
    private ObjectMapper mapper;
    private ThreadPoolExecutor pool;





    @Override
    public void onCreate(ServiceProxy c) {
        Log.v(TAG, "onCreate()");
        this.context = c;
        this.mapper = new ObjectMapper();
        this.context = c;
        this.pool= new ThreadPoolExecutor(2,2,1,  TimeUnit.MINUTES,new LinkedBlockingQueue<Runnable>());

        this.endpoint = (ServiceMessageEndpoint) ServiceProxy.instantiateService(ServiceProxy.SERVICE_BROKER);
        this.endpoint.setMessageReceiverCallback(this);
        this.endpoint.setMessageSenderCallback(this);

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


    // ServiceMessage.MessageSender interface
    @Override
    public void sendMessage(MessageBase message) {
        message.setOutgoing();
        endpoint.sendMessage(message);
    }

    @Override
    public void onMessageDelivered(MessageBase message) {
        Log.v(TAG, "message delivered: " + message);

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
        message.setIncomingProcessor(this);
        pool.execute(message);

    }

    @Override
    public void processMessage(MessageBase message) {
        Log.v(TAG, "processMessage MessageBase (" + message.getTopic()+")");
    }

    public void processMessage(MessageUnknown message) {
        Log.v(TAG, "processMessage MessageUnknown (" + message.getTopic()+")");
    }


    @Override
    public void processMessage(MessageLocation message) {
        Log.v(TAG, "processMessage MessageLocation (" + message.getTopic()+")");

        GeocodingProvider.resolve(message);
        FusedContact c = App.getFusedContact(message.getTopic());

        if (c == null) {
            c = new FusedContact(message.getTopic());
            c.setMessageLocation(message);
            App.addFusedContact(c);
        } else {
            c.setMessageLocation(message);
            App.updateFusedContact(c);
        }
    }

    @Override
    public void processMessage(MessageCard message) {
        Log.v(TAG, "processMessage MessageCard (" + message.getTopic() + ")");
        FusedContact c = App.getFusedContact(message.getTopic());

        if (c == null) {
            c = new FusedContact(message.getTopic());
            c.setMessageCard(message);
            App.addFusedContact(c);
        } else {
            c.setMessageCard(message);
            App.updateFusedContact(c);
        }
    }

    @Override
    public void processMessage(MessageCmd message) {
        Log.v(TAG, "processMessage MessageCmd (" + message.getTopic() + ")");
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
        Log.v(TAG, "processMessage MessageTransition (" + message.getTopic() + ")");
        ServiceProxy.getServiceNotification().processMessage(message);
    }

    public void processMessage(MessageConfiguration message) {
        Log.v(TAG, "processMessage MessageConfiguration (" + message.getTopic()+")");
        if(!Preferences.getRemoteConfiguration())
            return;

        Preferences.importFromMessage(message);
    }



}
