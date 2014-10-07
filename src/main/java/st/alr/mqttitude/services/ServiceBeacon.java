package st.alr.mqttitude.services;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.os.Bundle;
import android.os.RemoteException;
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

import de.greenrobot.event.EventBus;
import st.alr.mqttitude.messages.BeaconMessage;
import st.alr.mqttitude.support.Defaults;
import st.alr.mqttitude.support.Events;
import st.alr.mqttitude.support.Preferences;
import st.alr.mqttitude.support.ServiceMqttCallbacks;

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

        mBeaconManager = BeaconManager.getInstanceForApplication(this.context);

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

        // Should add prefs for these
        mBeaconManager.setBackgroundBetweenScanPeriod(30000L);  // default is 300000L
        mBeaconManager.setBackgroundScanPeriod(2000L);          // default is 10000L
        mBeaconManager.setForegroundBetweenScanPeriod(0L);      // default is 0L
        mBeaconManager.setForegroundScanPeriod(1100L);          // Default is 1100L

        // Not sure that we can publish this without violating Apple IP.
        mBeaconManager.getBeaconParsers().add(new BeaconParser().
                setBeaconLayout("m:0-3=4c000215,i:4-19,i:20-21,i:22-23,p:24-24"));

        // Detect all valid beacons
        region = new Region("all beacons", null, null, null);
        regionBootstrap = new RegionBootstrap(this, region);
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
                Preferences.getPubRetain(), Preferences.getPubQos(), 20, this, r);

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
    public void onDestroy() {
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
