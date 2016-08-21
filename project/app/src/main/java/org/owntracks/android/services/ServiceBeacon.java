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
import org.greenrobot.eventbus.Subscribe;
import org.owntracks.android.db.Dao;
import org.owntracks.android.db.Waypoint;
import org.owntracks.android.db.WaypointDao;
import org.owntracks.android.messages.MessageTransition;
import org.owntracks.android.support.Events;
import org.owntracks.android.support.Preferences;
import org.owntracks.android.support.interfaces.ProxyableService;

// Detects Bluetooth LE beacons as defined in the AltBeacon Spec:
//  -> https://github.com/AltBeacon/spec

public class ServiceBeacon implements ProxyableService, BeaconConsumer {
    private static final String TAG = "ServiceBeacon";

    private Context context;
    private HashMap<Long, Region> activeRegions;
    private BeaconManager beaconManager;
    private WaypointDao waypointDao;

    private static final int BEACON_MODE_DEFAULT = 0;
    private static final int BEACON_MODE_LEGACY_SCANNING = 1;
    private static final int BEACON_MODE_OFF = 2;

    @Override
    public void onCreate(ServiceProxy c) {
        Log.v(TAG, "onCreate()");

        this.context = c;
        this.waypointDao = Dao.getWaypointDao();
        this.activeRegions = new HashMap<>();




        if(loadWaypointsForModeIdWithValidBeacon().size() > 0) {
            initBeaconManager();
        } else {
            Log.v(TAG, "not initializing beaconManager because no regions are setup");
        }

    }


    private void initBeaconManager() {
        // Gets additional information about available BLE features
        BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        int beaconMode = Preferences.getBeaconMode();

        if(bluetoothAdapter == null) {
            Log.e(TAG, "Bluetooth is not available");
            return;
        }
        if(beaconMode == BEACON_MODE_OFF) {
            Log.e(TAG, "Beacon scanning is disabled");
            return;

        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            Log.v(TAG, "bluetoothAdapter.isMultipleAdvertisementSupported: " + bluetoothAdapter.isMultipleAdvertisementSupported());
            Log.v(TAG, "bluetoothAdapter.isOffloadedFilteringSupported: " + bluetoothAdapter.isOffloadedFilteringSupported());
            Log.v(TAG, "bluetoothAdapter.isOffloadedScanBatchingSupported: " + bluetoothAdapter.isOffloadedScanBatchingSupported());
        }


        BeaconManager.setAndroidLScanningDisabled(beaconMode == BEACON_MODE_LEGACY_SCANNING);
        beaconManager = BeaconManager.getInstanceForApplication(context);
        beaconManager.setForegroundBetweenScanPeriod(TimeUnit.SECONDS.toMillis(30));
        beaconManager.setBackgroundBetweenScanPeriod(TimeUnit.SECONDS.toMillis(120));
        beaconManager.setBackgroundScanPeriod(TimeUnit.SECONDS.toMillis(30));
        beaconManager.getBeaconParsers().add(new BeaconParser().setBeaconLayout("m:2-3=0215,i:4-19,i:20-21,i:22-23,p:24-24"));        // TODO: make configurable
        beaconManager.bind(this);
    }



    @Override
    public void onDestroy() {
        if(beaconManager != null && beaconManager.isBound(this))
            beaconManager.unbind(this);
    }


    @Override
    public void onStartCommand(Intent intent, int flags, int startId) {

    }

    @SuppressWarnings("unused")
    @Subscribe
    public void onEvent(Events.Dummy event) {

    }

    private void publishTransitionMessage(Region triggeringRegion, String transition) {
        Waypoint w = waypointDao.load(Long.parseLong(triggeringRegion.getUniqueId()));
        if(w == null) {
            Log.e(TAG, "unable to load waypoint from entered region");
            return;
        }
        MessageTransition m = new MessageTransition();
        m.setLat(w.getGeofenceLatitude());
        m.setLon(w.getGeofenceLongitude());
        m.setDesc(w.getDescription());
        m.setTst(TimeUnit.MILLISECONDS.toSeconds(System.currentTimeMillis()));
        m.setEvent(transition);
        m.setTid(Preferences.getTrackerId(true));
        m.setWtst(TimeUnit.MILLISECONDS.toSeconds(w.getDate().getTime()));
        m.setTrigger(MessageTransition.TRIGGER_BEACON);


        ServiceProxy.getServiceMessage().sendMessage(m);

        w.setLastTriggered(System.currentTimeMillis());
        this.waypointDao.update(w);

    }

    @Override
    public void onBeaconServiceConnect() {
        Log.v(TAG, "onBeaconServiceConnect");
        beaconManager.setMonitorNotifier(new MonitorNotifier() {
            @Override
            public void didEnterRegion(Region region) {
                Log.i(TAG, "didEnterRegion " + region.getUniqueId() + " " + Long.parseLong(region.getUniqueId()));
                publishTransitionMessage(region, MessageTransition.EVENT_ENTER);
            }

            @Override
            public void didExitRegion(Region region) {
                Log.i(TAG, "didExitRegion " + region.getUniqueId());
                publishTransitionMessage(region, MessageTransition.EVENT_LEAVE);
            }

            @Override
            public void didDetermineStateForRegion(int state, Region region) {
                Log.i(TAG, "didDetermineStateForRegion " + region.getUniqueId() + " state: " + state);
            }
        });

        for(Waypoint w : loadWaypointsForModeIdWithValidBeacon()) {
            addRegion(w);
        }
    }

    @SuppressWarnings("unused")
    public void onEvent(Events.WaypointAdded e) {
        addRegion(e.getWaypoint());
    }

    private boolean hasBeaconManager() {
        return beaconManager != null;
    }

    private void removeRegion(Waypoint w) {
        try {
            Region r = activeRegions.get(w.getId());
            if(hasBeaconManager() && r != null ) {
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

        if(!hasBeaconManager()) {
            initBeaconManager();
            return;
        }


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
            if(w.getBeaconUUID() == null || w.getBeaconUUID().isEmpty())
                return false;

            Identifier.parse(w.getBeaconUUID());
            return true;
        } catch (Exception e) {
            Log.e(TAG, "beacon UUID parse exception: " + e.getMessage());
            return false;
        }
    }


    private Region getRegionFromWaypoint(Waypoint w) {
        return new Region(w.getId().toString(), Identifier.parse(w.getBeaconUUID()), w.getBeaconMajor() != null ? Identifier.fromInt(w.getBeaconMajor()) : null, w.getBeaconMinor() != null ? Identifier.fromInt(w.getBeaconMinor()) : null);
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
