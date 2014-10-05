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
import org.altbeacon.beacon.RangeNotifier;
import org.altbeacon.beacon.Region;
import org.altbeacon.beacon.powersave.BackgroundPowerSaver;
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
    private static Defaults.State.ServiceLocator state = Defaults.State.ServiceLocator.INITIAL;
    private ServiceProxy context;

    private boolean ready = false;

    private long lastPublish;

    private RegionBootstrap regionBootstrap;
    private Region region;
    private BeaconManager mBeaconManager;
    private BackgroundPowerSaver mBackgroundPowerSaver;

    @Override
    public void didDetermineStateForRegion(int arg0, Region arg1) {
        Log.d(this.toString(), "Got a didDetermineStateForRegion call");
        Log.d(this.toString(), "State: " + arg1.getUniqueId() + ", " + arg1.toString());
    }

    @Override
    public void didEnterRegion(Region arg0) {
        Log.d(this.toString(), "Region entered: " + arg0.getUniqueId() + ", " + arg0.toString());
        try {
            Log.d(this.toString(), "Beginning ranging");
            mBeaconManager.startRangingBeaconsInRegion(region);
            mBeaconManager.setRangeNotifier(this);
        } catch (RemoteException e) {
            Log.e(this.toString(), "Cannot start ranging");
        }
    }

    @Override
    public void didExitRegion(Region arg0) {
        Log.d(this.toString(), "Got a didExitRegion call");
        Log.d(this.toString(), "Region exited: " + arg0.getUniqueId() + ", " + arg0.toString());
    }

    @Override
    public void didRangeBeaconsInRegion(Collection<Beacon> arg0, Region arg1) {
        for(Beacon beacon : arg0)
        {
            Log.d(this.toString(), "Found beacon: " + beacon.getId1());

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
                    System.currentTimeMillis());

            publishBeaconMessage(r);
        }
    }

    @Override
    public void onCreate(ServiceProxy p) {

        this.context = p;
        this.lastPublish = 0;

        mBeaconManager = BeaconManager.getInstanceForApplication(this.context);
        //mBeaconManager.setDebug(true);


        mBeaconManager.getBeaconParsers().add(new BeaconParser().
                setBeaconLayout("m:0-3=4c000215,i:4-19,i:20-21,i:22-23,p:24-24"));

        //mBackgroundPowerSaver = new BackgroundPowerSaver(this.context);

        // wake up the app when any beacon is seen (you can specify specific id filers in the parameters below)
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
            changeState(Defaults.State.ServiceLocator.NOTOPIC);
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

    public static Defaults.State.ServiceLocator getState() {
        return state;
    }

    public static String getStateAsString(Context c) {
        return stateAsString(getState(), c);
    }

    public static String stateAsString(Defaults.State.ServiceLocator state,
                                       Context c) {
        return Defaults.State.toString(state, c);
    }

    private void changeState(Defaults.State.ServiceLocator newState) {
        Log.d(this.toString(), "ServiceLocator state changed to: " + newState);
        EventBus.getDefault().postSticky(
                new Events.StateChanged.ServiceLocator(newState));
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
        changeState(Defaults.State.ServiceLocator.PUBLISHING_TIMEOUT);
    }

    @Override
    public void publishing(Object extra) {
        changeState(Defaults.State.ServiceLocator.PUBLISHING);
    }

    @Override
    public void publishWaiting(Object extra) {
        changeState(Defaults.State.ServiceLocator.PUBLISHING_WAITING);
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
    public Context getApplicationContext() { // FLAT WRONG (I think?)
        return this.context;
    }
}
