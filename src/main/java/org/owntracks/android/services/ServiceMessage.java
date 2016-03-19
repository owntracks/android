package org.owntracks.android.services;

import android.bluetooth.BluetoothAdapter;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Build;
import android.os.RemoteException;
import android.util.Log;

import org.altbeacon.beacon.BeaconConsumer;
import org.altbeacon.beacon.BeaconManager;
import org.altbeacon.beacon.BeaconParser;
import org.altbeacon.beacon.Identifier;
import org.altbeacon.beacon.MonitorNotifier;
import org.altbeacon.beacon.Region;
import org.owntracks.android.db.Dao;
import org.owntracks.android.db.Waypoint;
import org.owntracks.android.db.WaypointDao;
import org.owntracks.android.messages.MessageBase;
import org.owntracks.android.messages.MessageTransition;
import org.owntracks.android.support.Events;
import org.owntracks.android.support.Preferences;
import org.owntracks.android.support.interfaces.MessageReceiver;
import org.owntracks.android.support.interfaces.MessageSender;
import org.owntracks.android.support.interfaces.ServiceMessageEndpoint;

import java.util.HashMap;
import java.util.List;
import java.util.concurrent.TimeUnit;



public class ServiceMessage implements ProxyableService, MessageSender, MessageReceiver {
    private static final String TAG = "ServiceMessage";

    private Context context;
    private ServiceMessageEndpoint endpoint;




    @Override
    public void onCreate(ServiceProxy c) {
        Log.v(TAG, "onCreate()");
        endpoint = (ServiceMessageEndpoint) ServiceProxy.instantiateService(ServiceProxy.SERVICE_BROKER);
        endpoint.setOnMessageDeliveredCallback(this);
        endpoint.setOnMessageQueuedCallback(this);
        endpoint.setOnMessageReceivedCallback(this);

        this.context = c;
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

    }

    @Override
    public void onMessageDelivered(MessageBase message) {

    }

    @Override
    public void onMessageQueued(MessageBase message) {

    }

    // ServiceMessage.MessageReceiver interface
    @Override
    public void onMessageReceive(MessageBase message) {

    }

}
