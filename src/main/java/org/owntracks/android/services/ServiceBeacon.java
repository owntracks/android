package org.owntracks.android.services;

import android.bluetooth.BluetoothAdapter;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.os.Bundle;
import android.os.RemoteException;
import android.preference.PreferenceManager;
import android.util.Log;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesClient;

import org.altbeacon.beacon.Beacon;
import org.altbeacon.beacon.BeaconManager;
import org.altbeacon.beacon.BeaconParser;
import org.altbeacon.beacon.BleNotAvailableException;
import org.altbeacon.beacon.RangeNotifier;
import org.altbeacon.beacon.Region;
import org.altbeacon.beacon.startup.BootstrapNotifier;
import org.altbeacon.beacon.startup.RegionBootstrap;

import java.util.Collection;
import java.util.List;

import de.greenrobot.event.EventBus;
import org.owntracks.android.R;
import org.owntracks.android.messages.BeaconMessage;
import org.owntracks.android.support.BluetoothStateChangeReceiver;
import org.owntracks.android.support.Defaults;
import org.owntracks.android.support.Events;
import org.owntracks.android.support.Preferences;
import org.owntracks.android.support.ServiceMqttCallbacks;

// Detects Bluetooth LE beacons as defined in the AltBeacon Spec:
//  -> https://github.com/AltBeacon/spec

public class ServiceBeacon implements
        ProxyableService, ServiceMqttCallbacks,
        GooglePlayServicesClient.ConnectionCallbacks,
        GooglePlayServicesClient.OnConnectionFailedListener,
        BootstrapNotifier, RangeNotifier {

    private SharedPreferences sharedPreferences;
    private OnSharedPreferenceChangeListener preferencesChangedListener;
    private static Defaults.State.ServiceBeacon state = Defaults.State.ServiceBeacon.INITIAL;
    private ServiceProxy context;
    private BluetoothStateChangeReceiver bluetoothStateChangeReceiver;

    public void setBluetoothMode(int state) {
        switch (state) {
            case BluetoothAdapter.STATE_OFF:
                Log.v(this.toString(), "Bluetooth turned off");
                stopBeaconScanning();
                break;
            case BluetoothAdapter.STATE_TURNING_OFF:
                Log.v(this.toString(), "Bluetooth is turning off");
                stopBeaconScanning();
                break;
            case BluetoothAdapter.STATE_ON:
                Log.v(this.toString(), "Bluetooth on");
                if(Preferences.getBeaconRangingEnabled())
                    initializeBeaconScanning();
                break;
            case BluetoothAdapter.STATE_TURNING_ON:
                Log.v(this.toString(), "Bluetooth is turning on");
                break;
        }
    }

    private enum ScanningState {
        DISABLED, ENABLED
    }
    private ScanningState scanningState;

    private boolean ready = false;

    private long lastPublish;

    private RegionBootstrap regionBootstrap;
    private Region region;
    private BeaconManager mBeaconManager;

    @Override
    public void didDetermineStateForRegion(int arg0, Region arg1) {
        Log.v(this.toString(), "didDetermineStateForRegion: " + arg1.getUniqueId() + ", " + arg1.toString());
    }

    @Override
    public void didEnterRegion(Region arg0) {
        Log.d(this.toString(), "Region entered: " + arg0.getUniqueId() + ", " + arg0.toString());
        try {
            Log.v(this.toString(), "Beginning ranging");
            mBeaconManager.startRangingBeaconsInRegion(region);
            mBeaconManager.setRangeNotifier(this);
        } catch (RemoteException e) {
            Log.e(this.toString(), "Cannot start ranging");
        }


    }

    @Override
    public void didExitRegion(Region arg0) {
        Log.v(this.toString(), "didExitRegion: " + arg0.getUniqueId() + ", " + arg0.toString());
    }

    @Override
    public void didRangeBeaconsInRegion(Collection<Beacon> arg0, Region arg1) {
        for(Beacon beacon : arg0)
        {
            Log.d(this.toString(), "Found beacon: " + beacon.getId1());

            int proximity = 0;
            double distance = beacon.getDistance();

            // Calculate proximity
            if(distance < 1) // Under 1 meter: immediate
                proximity = 1;
            else if(distance < 5) // Under 4 meters: near
                proximity = 2;
            else
                proximity = 3; // More than 4 meters: far


            BeaconMessage r = new BeaconMessage(
                    beacon.getId1(),
                    beacon.getId2(),
                    beacon.getId3(),
                    beacon.getRssi(),
                    beacon.getDistance(),
                    beacon.getBluetoothName(),
                    beacon.getManufacturer(),
                    beacon.getBluetoothAddress(),
                    beacon.getBeaconTypeCode(),
                    beacon.getTxPower(),
                    System.currentTimeMillis(),
                    proximity);

            publishBeaconMessage(r);
        }
    }

    @Override
    public void onCreate(ServiceProxy p) {
        this.context = p;
        this.lastPublish = 0;

        this.sharedPreferences = PreferenceManager
                .getDefaultSharedPreferences(this.context);

        this.preferencesChangedListener = new SharedPreferences.OnSharedPreferenceChangeListener() {
            @Override
            public void onSharedPreferenceChanged(
                    SharedPreferences sharedPreference, String key) {
                handlePreferences(key);
            }
        };
        this.sharedPreferences
                .registerOnSharedPreferenceChangeListener(this.preferencesChangedListener);

        bluetoothStateChangeReceiver = new BluetoothStateChangeReceiver();
        this.context.registerReceiver(bluetoothStateChangeReceiver, new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED));

        mBeaconManager = BeaconManager.getInstanceForApplication(this.context);
        scanningState = ScanningState.DISABLED;

        // Detect all valid beacons
        region = new Region("all beacons", null, null, null);

        if(Preferences.getBeaconRangingEnabled()) {
            Log.v(this.toString(), "Ranging enabled on startup");
            initializeBeaconScanning();
        } else
            Log.v(this.toString(), "Ranging disabled on startup");
    }

    private void handlePreferences(String key)
    {
        if (key.equals(Preferences.getKey(R.string.keyBeaconRangingEnabled)))
        {
            Log.v(this.toString(), "Beacon ranging toggle");
            if(Preferences.getBeaconRangingEnabled())
                initializeBeaconScanning();
            else
                stopBeaconScanning();
        }
        else if(key.equals(Preferences.getKey(R.string.keyCustomBeaconLayout)))
        {
            Log.v(this.toString(), "Setting custom beacon layout");
            refreshCustomParser();
        }
        else if(key.equals(Preferences.getKey(R.string.keyBeaconBackgroundScanPeriod)))
        {
            Log.v(this.toString(), "Setting background beacon scan period");
            setBeaconScanningIntervals();
        }
        else if(key.equals(Preferences.getKey(R.string.keyBeaconForegroundScanPeriod)))
        {
            Log.v(this.toString(), "Setting foreground beacon scan period");
            setBeaconScanningIntervals();
        }
    }

    private void initializeBeaconScanning()
    {
        Log.v(this.toString(), "Initializing beacon scanning");

        try
        {
            if(!mBeaconManager.checkAvailability()) {
                changeState(Defaults.State.ServiceBeacon.NOBLUETOOTH);
                Log.e(this.toString(), "Bluetooth not available");
                return;
            }
        }
        catch (BleNotAvailableException e)
        {
            changeState(Defaults.State.ServiceBeacon.NOBLUETOOTH);
            Log.e(this.toString(), "Bluetooth not available");
            return;
        }

        Log.v(this.toString(), "Beacon scanning should be available");
        setBeaconScanningIntervals();

        refreshCustomParser();

        Log.v(this.toString(), "Setting up RegionBootstrap");
        regionBootstrap = new RegionBootstrap(this, region);

        scanningState = ScanningState.ENABLED;
    }

    private void setBeaconScanningIntervals()
    {
        if(scanningState != ScanningState.ENABLED)
            return;

        mBeaconManager.setBackgroundBetweenScanPeriod(Preferences.getBeaconBackgroundScanPeriod() * 1000);  // default is 300000L
        mBeaconManager.setBackgroundScanPeriod(2000L);          // default is 10000L
        mBeaconManager.setForegroundBetweenScanPeriod(Preferences.getBeaconForegroundScanPeriod() * 1000);      // default is 0L
        mBeaconManager.setForegroundScanPeriod(1100L);          // Default is 1100L
    }

    private void refreshCustomParser()
    {
        if(scanningState != ScanningState.ENABLED)
            return;

        String customBeaconlayout = Preferences.getCustomBeaconLayout();
        if(!customBeaconlayout.equals(""))
        {
            Log.v(this.toString(), "Got custom parser layout");
            List<BeaconParser> parsers = mBeaconManager.getBeaconParsers();
            Log.v(this.toString(), "Parser count: " + parsers.size());

            if(parsers.size() > 0)
                parsers.remove(parsers.get(parsers.size() - 1)); // Remove last parser

            parsers.add(new BeaconParser().setBeaconLayout(customBeaconlayout));
        }
    }

    private void stopBeaconScanning()
    {
        Log.v(this.toString(), "Stopping beacon scanning");

        if(scanningState != ScanningState.ENABLED)
            return;

        scanningState = ScanningState.DISABLED;

        regionBootstrap.disable();
        try {
            mBeaconManager.stopRangingBeaconsInRegion(region);
            mBeaconManager.stopMonitoringBeaconsInRegion(region);
            mBeaconManager = null;
        }
        catch (RemoteException e)
        {
            Log.e(this.toString(), e.toString());
        }
    }

    private void publishBeaconMessage(BeaconMessage r) {
        this.lastPublish = System.currentTimeMillis();

        // Safety checks
        if (ServiceProxy.getServiceBroker() == null) {
            Log.e(this.toString(), "publishLocationMessage but ServiceMqtt not ready");
            return;
        }

        String topic = Preferences.getPubTopicBase(true);
        if (topic == null) {
            changeState(Defaults.State.ServiceBeacon.NOTOPIC);
            return;
        }

        ServiceProxy.getServiceBroker().publish(topic, r.toString(),
                Preferences.getPubRetain(), Preferences.getPubQos(), this, r);

    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
        Log.e(this.toString(), "Failed to connect");
    }

    @Override
    public void onConnected(Bundle arg0) {
        this.ready = true;

        Log.v(this.toString(), "Connected");
    }

    @Override
    public void onDisconnected() {
        this.ready = false;
        ServiceApplication.checkPlayServices(); // show error notification if
        // play services were disabled
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.v(this.toString(), "Received an intent");

        return 0;
    }

    @Override
    public void onDestroy()
    {
        try {
            context.unregisterReceiver(bluetoothStateChangeReceiver);
        } catch (IllegalArgumentException e) {
            // Receiver was not registered, ignore
        }
    }

    public static Defaults.State.ServiceBeacon getState() {
        return state;
    }

    public static String getStateAsString(Context c) {
        return stateAsString(getState(), c);
    }

    public static String stateAsString(Defaults.State.ServiceBeacon state,
                                       Context c) {
        return Defaults.State.toString(state, c);
    }

    private void changeState(Defaults.State.ServiceBeacon newState) {
        Log.d(this.toString(), "ServiceBeacon state changed to: " + newState);
        EventBus.getDefault().postSticky(
                new Events.StateChanged.ServiceBeacon(newState));
        state = newState;
    }

    public void setBackgroundMode(boolean backgroundMode)
    {
        if(scanningState != ScanningState.ENABLED)
            return;

        mBeaconManager.setBackgroundMode(backgroundMode);
    }

    @Override
    public void publishSuccessfull(Object extra) {

    }

    @Override
    public void publishFailed(Object extra) {
        changeState(Defaults.State.ServiceBeacon.PUBLISHING_TIMEOUT);
    }

    @Override
    public void publishing(Object extra) {
        changeState(Defaults.State.ServiceBeacon.PUBLISHING);
    }

    @Override
    public void publishWaiting(Object extra) {
        changeState(Defaults.State.ServiceBeacon.PUBLISHING_WAITING);
    }

    public long getLastPublishDate() {
        return this.lastPublish;
    }

    public void onEvent(Object event) {
    }

    public boolean isReady() {
        return ready;
    }

    @Override
    public Context getApplicationContext() {
        return this.context;
    }
}
