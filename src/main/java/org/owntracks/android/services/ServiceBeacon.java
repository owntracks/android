package org.owntracks.android.services;

import android.bluetooth.BluetoothAdapter;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.os.RemoteException;
import android.preference.PreferenceManager;
import android.util.Log;

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
import java.util.ArrayList;

import de.greenrobot.event.EventBus;
import org.owntracks.android.R;
import org.owntracks.android.messages.BeaconMessage;
import org.owntracks.android.support.receiver.BluetoothStateChangeReceiver;
import org.owntracks.android.support.Events;
import org.owntracks.android.support.Preferences;
import org.owntracks.android.support.MessageLifecycleCallbacks;

// Detects Bluetooth LE beacons as defined in the AltBeacon Spec:
//  -> https://github.com/AltBeacon/spec

public class ServiceBeacon implements
        ProxyableService, MessageLifecycleCallbacks,
        BootstrapNotifier, RangeNotifier {
    private static final String TAG = "ServiceBeacon";

    public static enum State {
        INITIAL, PUBLISHING, PUBLISHING_WAITING, PUBLISHING_TIMEOUT, NOTOPIC, NOBLUETOOTH
    }




    private SharedPreferences sharedPreferences;
    private OnSharedPreferenceChangeListener preferencesChangedListener;
    private static ServiceBeacon.State state = ServiceBeacon.State.INITIAL;
    private ServiceProxy context;
    private BluetoothStateChangeReceiver bluetoothStateChangeReceiver;

    public void setBluetoothMode(int state) {
        switch (state) {
            case BluetoothAdapter.STATE_OFF:
                Log.v(TAG, "Bluetooth turned off");
                stopBeaconScanning();
                break;
            case BluetoothAdapter.STATE_TURNING_OFF:
                Log.v(TAG, "Bluetooth is turning off");
                stopBeaconScanning();
                break;
            case BluetoothAdapter.STATE_ON:
                Log.v(TAG, "Bluetooth on");
                if(Preferences.getBeaconRangingEnabled())
                    initializeBeaconScanning();
                break;
            case BluetoothAdapter.STATE_TURNING_ON:
                Log.v(TAG, "Bluetooth is turning on");
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
        Log.v(TAG, "didDetermineStateForRegion: " + arg1.getUniqueId() + ", " + arg1.toString());
    }

    @Override
    public void didEnterRegion(Region arg0) {
        Log.d(TAG, "Region entered: " + arg0.getUniqueId() + ", " + arg0.toString());
        try {
            Log.v(TAG, "Beginning ranging");
            mBeaconManager.startRangingBeaconsInRegion(region);
            mBeaconManager.setRangeNotifier(this);
        } catch (RemoteException e) {
            Log.e(TAG, "Cannot start ranging");
        }


    }

    @Override
    public void didExitRegion(Region arg0) {
        Log.v(TAG, "didExitRegion: " + arg0.getUniqueId() + ", " + arg0.toString());
    }

    @Override
    public void didRangeBeaconsInRegion(Collection<Beacon> arg0, Region arg1) {
        for(Beacon beacon : arg0)
        {
            Log.d(TAG, "Found beacon: " + beacon.getId1());

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
            Log.v(TAG, "Ranging enabled on startup");
            initializeBeaconScanning();
        } else
            Log.v(TAG, "Ranging disabled on startup");
    }

    private void handlePreferences(String key)
    {
        if (key.equals(Preferences.getKey(R.string.keyBeaconRangingEnabled)))
        {
            Log.v(TAG, "Beacon ranging toggle");
            if(Preferences.getBeaconRangingEnabled())
                initializeBeaconScanning();
            else
                stopBeaconScanning();
        }
        else if(key.equals(Preferences.getKey(R.string.keyCustomBeaconLayout)))
        {
            Log.v(TAG, "Setting custom beacon layout");
            refreshCustomParser();
        }
        else if(key.equals(Preferences.getKey(R.string.keyBeaconBackgroundScanPeriod)))
        {
            Log.v(TAG, "Setting background beacon scan period");
            setBeaconScanningIntervals();
        }
        else if(key.equals(Preferences.getKey(R.string.keyBeaconForegroundScanPeriod)))
        {
            Log.v(TAG, "Setting foreground beacon scan period");
            setBeaconScanningIntervals();
        }
    }

    private void initializeBeaconScanning()
    {
        Log.v(TAG, "Initializing beacon scanning");

        try
        {
            if(!mBeaconManager.checkAvailability()) {
                changeState(ServiceBeacon.State.NOBLUETOOTH);
                Log.e(TAG, "Bluetooth not available");
                return;
            }
        }
        catch (BleNotAvailableException e)
        {
            changeState(ServiceBeacon.State.NOBLUETOOTH);
            Log.e(TAG, "Bluetooth not available");
            return;
        }

        Log.v(TAG, "Beacon scanning should be available");
        setBeaconScanningIntervals();

        refreshCustomParser();

        Log.v(TAG, "Setting up RegionBootstrap");
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
            Log.v(TAG, "Got custom parser layout");
            List<BeaconParser> parsers = new ArrayList<BeaconParser>(mBeaconManager.getBeaconParsers());
            Log.v(TAG, "Parser count: " + parsers.size());

            if(parsers.size() > 0)
                parsers.remove(parsers.get(parsers.size() - 1)); // Remove last parser

            parsers.add(new BeaconParser().setBeaconLayout(customBeaconlayout));
        }
    }

    private void stopBeaconScanning()
    {
        Log.v(TAG, "Stopping beacon scanning");

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
            Log.e(TAG, e.toString());
        }
    }

    private void publishBeaconMessage(BeaconMessage r) {
        this.lastPublish = System.currentTimeMillis();

        // Safety checks
        if (ServiceProxy.getServiceBroker() == null) {
            Log.e(TAG, "publishLocationMessage but ServiceMqtt not ready");
            return;
        }

        String topic = Preferences.getPubTopicEvents();
        if (topic == null) {
            changeState(ServiceBeacon.State.NOTOPIC);
            return;
        }

        ServiceProxy.getServiceBroker().publish(r, topic, Preferences.getPubQos(),
                Preferences.getPubRetain(), this, null);

    }


    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.v(TAG, "Received an intent");

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

    public static ServiceBeacon.State getState() {
        return state;
    }

    private void changeState(ServiceBeacon.State newState) {
        Log.d(TAG, "ServiceBeacon state changed to: " + newState);
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
    public void onMessagePublishSuccessful(Object extra, boolean wasQueued) {

    }

    @Override
    public void onMessagePublishFailed(Object extra) {
        changeState(ServiceBeacon.State.PUBLISHING_TIMEOUT);
    }

    @Override
    public void onMesssagePublishing(Object extra) {
        changeState(ServiceBeacon.State.PUBLISHING);
    }

    @Override
    public void onMessagePublishQueued(Object extra) {

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

    public static String getStateAsString(Context c) {
        int id;
        switch (getState()) {
            case PUBLISHING:
                id = R.string.statePublishing;
                break;
            case PUBLISHING_WAITING:
                id = R.string.stateWaiting;
                break;

            case NOTOPIC:
                id = R.string.stateNotopic;
                break;
            case NOBLUETOOTH:
                id = R.string.stateBluetoothFail;
                break;
            default:
                id = R.string.stateIdle;
        }

        return c.getString(id);
    }
}
