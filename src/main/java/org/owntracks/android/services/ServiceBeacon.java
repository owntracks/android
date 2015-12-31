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

import java.util.Collection;
import java.util.List;
import java.util.ArrayList;
import java.util.UUID;

import de.greenrobot.event.EventBus;

import org.altbeacon.beacon.Beacon;
import org.altbeacon.beacon.BeaconConsumer;
import org.altbeacon.beacon.BeaconManager;
import org.altbeacon.beacon.BeaconParser;
import org.altbeacon.beacon.Identifier;
import org.altbeacon.beacon.MonitorNotifier;
import org.altbeacon.beacon.RangeNotifier;
import org.altbeacon.beacon.Region;
import org.owntracks.android.App;
import org.owntracks.android.R;
import org.owntracks.android.messages.BeaconMessage;
import org.owntracks.android.support.receiver.BluetoothStateChangeReceiver;
import org.owntracks.android.support.Events;
import org.owntracks.android.support.Preferences;
import org.owntracks.android.support.MessageLifecycleCallbacks;

// Detects Bluetooth LE beacons as defined in the AltBeacon Spec:
//  -> https://github.com/AltBeacon/spec

public class ServiceBeacon implements ProxyableService, BeaconConsumer {
    private static final String TAG = "ServiceBeacon";

    private Context context;




    private String scanId;
    private BeaconManager beaconManager;

    @Override
    public void onCreate(ServiceProxy c) {
        this.context = c;
        Log.v(TAG, "onCreate()");

        beaconManager = BeaconManager.getInstanceForApplication(context);
        beaconManager.getBeaconParsers().add(new BeaconParser().setBeaconLayout("m:2-3=0215,i:4-19,i:20-21,i:22-23,p:24-24"));

        beaconManager.bind(this);

/*
        beaconManager = new BeaconManager(context);

        beaconManager.setNearableListener(new BeaconManager.NearableListener() {
            @Override
            public void onNearablesDiscovered(List<Nearable> nearables) {
                Log.d(TAG, "Discovered nearables: " + nearables);
            }
        });

        beaconManager.setMonitoringListener(new BeaconManager.MonitoringListener() {
            @Override
            public void onEnteredRegion(Region region, List<Beacon> list) {
                Log.v(TAG, "onEnteredReagion: " + region.getIdentifier());
            }

            @Override
            public void onExitedRegion(Region region) {
                Log.v(TAG, "onExitedRegion: " + region);
            }
        });
        beaconManager.connect(new BeaconManager.ServiceReadyCallback() {
            @Override
            public void onServiceReady()

            {
                scanId = beaconManager.startNearableDiscovery();
                beaconManager.startMonitoring( new Region("iphone",UUID.fromString("8492E75F-4FD6-469D-B132-043FE94921D8"), 2128, 4301));
                Log.v(TAG, "startNearableDiscovery");
            }
        });

*/



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
    public void onBeaconServiceConnect() {
        Log.v(TAG, "onBeaconServiceConnect");
        beaconManager.setMonitorNotifier(new MonitorNotifier() {
            @Override
            public void didEnterRegion(Region region) {
                Log.i(TAG, "didEnterRegion " + region.getUniqueId());
            }

            @Override
            public void didExitRegion(Region region) {
                Log.i(TAG, "didExitRegion " + region.getUniqueId());

            }

            @Override
            public void didDetermineStateForRegion(int state, Region region) {
                Log.i(TAG, "didDetermineStateForRegion " + region.getUniqueId() + " state: " + state);
            }
        });
/*
        beaconManager.setRangeNotifier(new RangeNotifier() {
            @Override
            public void didRangeBeaconsInRegion(Collection<Beacon> beacons, Region region) {
                if (beacons.size() > 0) {
                    Log.i(TAG, "The first beacon I see is about " + beacons.iterator().next().getDistance() + " meters away.");
                }
            }
        });

        try {
            beaconManager.startRangingBeaconsInRegion(new Region("foo", Identifier.parse("DBD75A2A-78C0-425C-A22B-37646BA46884"), null, null));
        } catch (RemoteException e) {    }
*/

        try {
            beaconManager.startMonitoringBeaconsInRegion(new Region("iphone", Identifier.parse("DBD75A2A-78C0-425C-A22B-37646BA46884"), null, null));
        } catch (RemoteException e) {    }

    }

    @Override
    public Context getApplicationContext() {
        return context.getApplicationContext();
    }

    @Override
    public void unbindService(ServiceConnection serviceConnection) {
        context.unbindService(serviceConnection);
    }

    @Override
    public boolean bindService(Intent intent, ServiceConnection serviceConnection, int i) {
        return context.bindService(intent, serviceConnection, i);
    }
}
