package st.alr.mqttitude.services;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import st.alr.mqttitude.App;
import st.alr.mqttitude.R;
import st.alr.mqttitude.messages.BeaconMessage;
import st.alr.mqttitude.support.Defaults;
import st.alr.mqttitude.support.Events;
import st.alr.mqttitude.support.ServiceMqttCallbacks;
import st.alr.mqttitude.support.Preferences;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;
import android.preference.PreferenceManager;
import android.util.Log;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesClient;

import de.greenrobot.event.EventBus;

import org.altbeacon.beacon.Beacon;
import org.altbeacon.beacon.BeaconConsumer;
import org.altbeacon.beacon.BeaconManager;
import org.altbeacon.beacon.BeaconParser;
import org.altbeacon.beacon.MonitorNotifier;
import org.altbeacon.beacon.RangeNotifier;
import org.altbeacon.beacon.Region;
import org.altbeacon.beacon.powersave.BackgroundPowerSaver;
import org.altbeacon.beacon.startup.BootstrapNotifier;
import org.altbeacon.beacon.startup.RegionBootstrap;

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
        Log.d(this.toString(), "Found beacon(s)");
//        "_type":"beacon",
//        "uuid":"CA271EAE-5FA8-4E80-8F08-2A302A95A959",
//        "major":1,
//        "minor":1,
//        "tst":"1399028969",
//        "acc":n,
//        "rssi":n,
//        "prox":n,

        for(Beacon beacon : arg0)
        {
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
                    beacon.getTxPower());
            publishBeaconMessage(r);

            Log.d(this.toString(), "uuid " + beacon.getId1());
            Log.d(this.toString(), "major " + beacon.getId2());
            Log.d(this.toString(), "minor " + beacon.getId3());
            Log.d(this.toString(), "tst " + "not supported");
            Log.d(this.toString(), "acc " + "not supported");
            Log.d(this.toString(), "rssi " + beacon.getRssi());
            Log.d(this.toString(), "dist " + beacon.getDistance());
            Log.d(this.toString(), "name " + beacon.getBluetoothName());
            Log.d(this.toString(), "manu " + beacon.getManufacturer());
            Log.d(this.toString(), "btaddr " + beacon.getBluetoothAddress());
            Log.d(this.toString(), "type " + beacon.getBeaconTypeCode());
            Log.d(this.toString(), "txpwr " + beacon.getTxPower());
        }
    }

    @Override
    public void onCreate(ServiceProxy p) {

        this.context = p;
        this.lastPublish = 0;

        mBeaconManager = BeaconManager.getInstanceForApplication(this.context);
        //mBeaconManager.setDebug(true);

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

    private boolean shouldPublishBeacon() {
        if (this.lastPublish == 0)
            return true;

        if ((System.currentTimeMillis() - this.lastPublish) > TimeUnit.MINUTES
                .toMillis(Preferences.getPubInterval()))
            return true;

        return false;
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
