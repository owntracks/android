package org.owntracks.android.services;

import android.bluetooth.BluetoothAdapter;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Build;
import android.os.RemoteException;
import android.util.Log;

import java.util.HashMap;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.altbeacon.beacon.BeaconConsumer;
import org.altbeacon.beacon.BeaconManager;
import org.altbeacon.beacon.BeaconParser;
import org.altbeacon.beacon.Identifier;
import org.altbeacon.beacon.MonitorNotifier;
import org.altbeacon.beacon.Region;
import org.altbeacon.beacon.powersave.BackgroundPowerSaver;
import org.owntracks.android.db.Dao;
import org.owntracks.android.db.Waypoint;
import org.owntracks.android.db.WaypointDao;
import org.owntracks.android.messages.MessageTransition;
import org.owntracks.android.support.Events;
import org.owntracks.android.support.Preferences;

// Detects Bluetooth LE beacons as defined in the AltBeacon Spec:
//  -> https://github.com/AltBeacon/spec

public class ServiceBeacon implements ProxyableService, BeaconConsumer {
    private static final String TAG = "ServiceBeacon";

    private Context context;
    private HashMap<Long, Region> activeRegions;



    private String scanId;
    private BeaconManager beaconManager;
    private WaypointDao waypointDao;
    private BackgroundPowerSaver backgroundPowerSaver;

    @Override
    public void onCreate(ServiceProxy c) {
        this.context = c;
        Log.v(TAG, "onCreate()");

        // Gets aditional information about available BLE features
        BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        if(bluetoothAdapter == null) {
            Log.e(TAG, "Bluetooth is not available");
            return;
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            Log.v(TAG, "bluetoothAdapter.isMultipleAdvertisementSupported: " + bluetoothAdapter.isMultipleAdvertisementSupported());
            Log.v(TAG, "bluetoothAdapter.isOffloadedFilteringSupported: " + bluetoothAdapter.isOffloadedFilteringSupported());
            Log.v(TAG, "bluetoothAdapter.isOffloadedScanBatchingSupported: " + bluetoothAdapter.isOffloadedScanBatchingSupported());
        }


        this.waypointDao = Dao.getWaypointDao();
        this.activeRegions = new HashMap<>();
        backgroundPowerSaver = new BackgroundPowerSaver(context);




        beaconManager = BeaconManager.getInstanceForApplication(context);
        beaconManager.setForegroundBetweenScanPeriod(TimeUnit.SECONDS.toMillis(30));
        beaconManager.setBackgroundBetweenScanPeriod(TimeUnit.SECONDS.toMillis(120));
        beaconManager.setBackgroundScanPeriod(TimeUnit.SECONDS.toMillis(10));
        BeaconManager.setAndroidLScanningDisabled(false);

        beaconManager.setBackgroundScanPeriod(TimeUnit.SECONDS.toMillis(10));
        // TODO: make configurable
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
    public void onStartCommand(Intent intent, int flags, int startId) {

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
                Log.i(TAG, "didEnterRegion " + region.getUniqueId() + " " + Long.parseLong(region.getUniqueId()));

                Waypoint w = waypointDao.load(Long.parseLong(region.getUniqueId()));
                if(w == null) {
                    Log.e(TAG, "unable to load waypoint from entered region");
                    return;
                }
                MessageTransition m = new MessageTransition();
                if(w.getGeofenceLatitude() != null)
                    m.setLat(w.getGeofenceLatitude());
                else
                    m.setLat(0);
                if(w.getGeofenceLongitude() != null)
                   m.setLon(w.getGeofenceLongitude());
                else
                    m.setLon(0);
                m.setDesc(w.getDescription());
                m.setTst(TimeUnit.MILLISECONDS.toSeconds(System.currentTimeMillis()));
                m.setEvent(MessageTransition.EVENT_ENTER);
                m.setTid(Preferences.getTrackerId(true));
                m.setWtst(TimeUnit.MILLISECONDS.toSeconds(w.getDate().getTime()));
                m.setTrigger(MessageTransition.TRIGGER_BEACON);


                m.setTopic(Preferences.getPubTopicEvents());
                m.setQos(Preferences.getPubQosEvents());
                m.setRetained(Preferences.getPubRetainEvents());

                ServiceProxy.getServiceBroker().publish(m);
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



            for(Waypoint w : loadWaypointsForModeIdWithValidBeacon()) {
                //DBD75A2A-78C0-425C-A22B-37646BA46884
                addRegion(w);
            }


    }

    @SuppressWarnings("unused")
    public void onEvent(Events.WaypointAdded e) {
        addRegion(e.getWaypoint());
    }

    private void removeRegion(Waypoint w) {
        try {
            Region r = activeRegions.get(w.getId());
            if(r != null) {
                Log.v(TAG, "removing region for ID " + w.getId());
                beaconManager.stopMonitoringBeaconsInRegion(r);
                activeRegions.remove(w.getId());
            } else
                Log.e(TAG, "skipping remove, region not setup for ID " + w.getId());
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    private void addRegion(Waypoint w) {
        Log.v(TAG, "addRegion: " + w.getDescription());
        if(!isWaypointWithValidRegion(w))
            return;


        Log.v(TAG, "startMonitoringBeaconsInRegion " + w.getId() + " desc: " + w.getDescription() + " UUID: " + w.getBeaconUUID() + " " + w.getBeaconMajor() + "/" + w.getBeaconMinor());
        try {
            Region r = getRegionFromWaypoint(w);
            Log.v(TAG, r.getUniqueId() + " " + r.getId1() + " " + r.getId2() + " " + r.getId3());
            beaconManager.startMonitoringBeaconsInRegion(r);
        } catch (Exception e) {
            Log.e(TAG, "unable to add region");
            e.printStackTrace();
        }
    }

    private boolean isWaypointWithValidRegion(Waypoint w) {

        try {
            if(w.getBeaconUUID() == null)
                return false;

            Identifier.parse(w.getBeaconUUID());
            return true;
        } catch (Exception e) {
            Log.e(TAG, "beacon UUID parse exception: " + e.getMessage());
            return false;
        }
    }


    private Region getRegionFromWaypoint(Waypoint w) {
        return new Region(w.getId().toString(), Identifier.parse(w.getBeaconUUID()), Identifier.fromInt(w.getBeaconMajor()), Identifier.fromInt(w.getBeaconMinor()));
    }

    @SuppressWarnings("unused")
    public void onEvent(Events.WaypointUpdated e) {
        removeRegion(e.getWaypoint());
        addRegion(e.getWaypoint());
    }

    @SuppressWarnings("unused")
    public void onEvent(Events.WaypointRemoved e) {
        removeRegion(e.getWaypoint());
    }

    private List<Waypoint> loadWaypointsForModeIdWithValidBeacon() {
        return this.waypointDao.queryBuilder().where(WaypointDao.Properties.ModeId.eq(Preferences.getModeId()), WaypointDao.Properties.BeaconUUID.isNotNull()).build().list();
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

    public void enableForegroundMode() {
        if(beaconManager != null) {
            Log.v(TAG, "enabling foreground mode");
            beaconManager.setBackgroundMode(false);
        }
    }

    public void enableBackgroundMode() {
        if(beaconManager != null) {
            Log.v(TAG, "enabling background mode");
            beaconManager.setBackgroundMode(true);
        }

    }

}
