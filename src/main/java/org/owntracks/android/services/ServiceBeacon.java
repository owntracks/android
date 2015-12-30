package org.owntracks.android.services;

import android.bluetooth.BluetoothAdapter;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.os.RemoteException;
import android.preference.PreferenceManager;
import android.util.Log;

import org.altbeacon.beacon.Beacon;
import org.altbeacon.beacon.BeaconConsumer;
import org.altbeacon.beacon.BeaconManager;
import org.altbeacon.beacon.BeaconParser;
import org.altbeacon.beacon.BleNotAvailableException;
import org.altbeacon.beacon.MonitorNotifier;
import org.altbeacon.beacon.RangeNotifier;
import org.altbeacon.beacon.Region;
import org.altbeacon.beacon.startup.BootstrapNotifier;
import org.altbeacon.beacon.startup.RegionBootstrap;

import java.util.Collection;
import java.util.List;
import java.util.ArrayList;

import de.greenrobot.event.EventBus;

import org.owntracks.android.App;
import org.owntracks.android.R;
import org.owntracks.android.messages.BeaconMessage;
import org.owntracks.android.support.receiver.BluetoothStateChangeReceiver;
import org.owntracks.android.support.Events;
import org.owntracks.android.support.Preferences;
import org.owntracks.android.support.MessageLifecycleCallbacks;

// Detects Bluetooth LE beacons as defined in the AltBeacon Spec:
//  -> https://github.com/AltBeacon/spec

public class ServiceBeacon implements BeaconConsumer, BootstrapNotifier, ProxyableService, MessageLifecycleCallbacks {
    private static final String TAG = "ServiceBeacon";

    private Context context;
    private BeaconManager beaconManager;

    @Override
    public void onBeaconServiceConnect() {
        Log.v(TAG, "onBeaconServiceConnect()");

        beaconManager.setRangeNotifier(new RangeNotifier() {
            @Override
            public void didRangeBeaconsInRegion(Collection<Beacon> beacons, Region region) {
                if (beacons.size() > 0) {
                    Log.i(TAG, "The first beacon I see is about "+beacons.iterator().next().getDistance()+" meters away.");
                }
            }
        });

        try {
            beaconManager.startRangingBeaconsInRegion(new Region("myRangingUniqueId", null, null, null));
        } catch (RemoteException e) {   Log.e(TAG, e.toString()); }

//
//        beaconManager.setMonitorNotifier(new MonitorNotifier() {
//            @Override
//            public void didEnterRegion(Region region) {
//                Log.i(TAG, "I just saw an beacon for the first time!");
//            }
//
//            @Override
//            public void didExitRegion(Region region) {
//                Log.i(TAG, "I no longer see an beacon");
//            }
//
//            @Override
//            public void didDetermineStateForRegion(int state, Region region) {
//                Log.i(TAG, "I have just switched from seeing/not seeing beacons: "+state);
//            }
//        });
//
//        try {
//            beaconManager.startMonitoringBeaconsInRegion(new Region("all beacons", null, null, null));
//        } catch (RemoteException e) {  e.printStackTrace();   }
//


    }


    @Override
    public void unbindService(ServiceConnection serviceConnection) {

    }

    @Override
    public boolean bindService(Intent intent, ServiceConnection serviceConnection, int i) {
        return false;
    }

    @Override
    public void onMessagePublishSuccessful(Object extra, boolean wasQueued) {

    }

    @Override
    public void onMessagePublishFailed(Object extra) {

    }

    @Override
    public void onMesssagePublishing(Object extra) {

    }

    @Override
    public void onMessagePublishQueued(Object extra) {

    }

    @Override
    public void onCreate(ServiceProxy c) {
        this.context = c;
        Log.v(TAG, "onCreate()");
        beaconManager = BeaconManager.getInstanceForApplication(context);
       // beaconManager.getBeaconParsers().add(new BeaconParser().setBeaconLayout("m:2-3=0215"));

        //beaconManager.getBeaconParsers().add(new BeaconParser().setBeaconLayout("m:2-3=0215,i:4-19,i:20-21,i:22-23,p:24-24")); // estimote
        beaconManager.bind(this);
        //>/Region region = new Region("all beacons", null, null, null);
        //RegionBootstrap regionBootstrap = new RegionBootstrap(this, region);

    }

    @Override
    public Context getApplicationContext() {
        return App.getContext();
    }

    @Override
    public void onDestroy() {

    }


    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return 0;
    }

    @Override
    public void onEvent(Events.Dummy event) {

    }


    @Override
    public void didEnterRegion(Region region) {
        Log.v(TAG, "didEnterRegion");
    }

    @Override
    public void didExitRegion(Region region) {
        Log.v(TAG, "didExitRegion");

    }

    @Override
    public void didDetermineStateForRegion(int i, Region region) {
        Log.v(TAG, "didDetermineStateForRegion");

    }
}
