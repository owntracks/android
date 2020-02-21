package org.owntracks.android.services;

import android.location.Location;
import android.os.Build;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.gms.location.Geofence;

import org.owntracks.android.data.WaypointModel;
import org.owntracks.android.data.repos.LocationRepo;
import org.owntracks.android.data.repos.WaypointsRepo;
import org.owntracks.android.injection.scopes.PerApplication;
import org.owntracks.android.messages.MessageLocation;
import org.owntracks.android.messages.MessageTransition;
import org.owntracks.android.messages.MessageWaypoint;
import org.owntracks.android.messages.MessageWaypoints;
import org.owntracks.android.support.DeviceMetricsProvider;
import org.owntracks.android.support.MessageWaypointCollection;
import org.owntracks.android.support.Preferences;

import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;

import timber.log.Timber;

@PerApplication
public class LocationProcessor {

    private final MessageProcessor messageProcessor;
    private final Preferences preferences;
    private final LocationRepo locationRepo;
    private final WaypointsRepo waypointsRepo;
    private final DeviceMetricsProvider deviceMetricsProvider;


    public static final int MONITORING_QUIET = -1;
    public static final int MONITORING_MANUAL = 0;
    public static final int MONITORING_SIGNIFFICANT = 1;
    public static final int MONITORING_MOVE = 2;



    @Inject
    public LocationProcessor(MessageProcessor messageProcessor, Preferences preferences, LocationRepo locationRepo, WaypointsRepo waypointsRepo, DeviceMetricsProvider deviceMetricsProvider) {
        this.messageProcessor = messageProcessor;
        this.preferences = preferences;
        this.deviceMetricsProvider = deviceMetricsProvider;
        this.locationRepo = locationRepo;
        this.waypointsRepo = waypointsRepo;

    }

    private boolean ignoreLowAccuracy(@NonNull Location l) {
        int threshold = preferences.getIgnoreInaccurateLocations();
        return threshold > 0 && l.getAccuracy() > threshold;
    }

    public void publishLocationMessage(@Nullable String trigger) {
        Timber.v("trigger:%s", trigger);
        if (!locationRepo.hasLocation()) {
            Timber.e("no location available");
            return;
        }

        Location currentLocation = locationRepo.getCurrentLocation();
        List<WaypointModel> loadedWaypoints = waypointsRepo.getAllWithGeofences();

        assert currentLocation != null;
        if (ignoreLowAccuracy(currentLocation)) {
            return;
        }

        // Check if publish would trigger a region if fusedRegionDetection is enabled
        if(loadedWaypoints.size() > 0 && preferences.getFuseRegionDetection() && !MessageLocation.REPORT_TYPE_CIRCULAR.equals(trigger)) {
            for(WaypointModel waypoint : loadedWaypoints) {
                onWaypointTransition(waypoint, currentLocation, currentLocation.distanceTo(waypoint.getLocation()) <= waypoint.getGeofenceRadius() ? Geofence.GEOFENCE_TRANSITION_ENTER : Geofence.GEOFENCE_TRANSITION_EXIT, MessageTransition.TRIGGER_LOCATION);
            }
        }

        if (preferences.getMonitoring() == MONITORING_QUIET && !MessageLocation.REPORT_TYPE_USER.equals(trigger) ) {
            Timber.v("message suppressed by monitoring settings: quiet");
            return;
        }

        if (preferences.getMonitoring() == MONITORING_MANUAL && (!MessageLocation.REPORT_TYPE_USER.equals(trigger) && !MessageLocation.REPORT_TYPE_CIRCULAR.equals(trigger))) {
            Timber.v("message suppressed by monitoring settings: manual");
            return;
        }

        MessageLocation message = new MessageLocation();
        message.setLat(currentLocation.getLatitude());
        message.setLon(currentLocation.getLongitude());
        message.setAlt((int)currentLocation.getAltitude());
        message.setAcc(Math.round(currentLocation.getAccuracy()));
        if (currentLocation.hasSpeed()) {
            message.setVelocity((int)(currentLocation.getSpeed() * 3.6)); // Convert m/s to km/h
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && currentLocation.hasVerticalAccuracy()) {
            message.setVac((int)currentLocation.getVerticalAccuracyMeters());
        }
        message.setT(trigger);

        message.setTst(TimeUnit.MILLISECONDS.toSeconds(System.currentTimeMillis()));

        message.setTid(preferences.getTrackerId(true));
        message.setInRegions(calculateInregions(loadedWaypoints));

        if (preferences.getPubLocationExtendedData()) {
            message.setBatt(deviceMetricsProvider.getBatteryLevel());
            message.setConn(deviceMetricsProvider.getConnectionType());
        }

        messageProcessor.queueMessageForSending(message);
    }

    //TODO: refactor to use ObjectBox query directly
    private List<String> calculateInregions(List<WaypointModel> loadedWaypoints) {
        LinkedList<String> l = new LinkedList<>();
        for(WaypointModel w : loadedWaypoints) {
            if(w.getLastTransition() == Geofence.GEOFENCE_TRANSITION_ENTER )
                l.add(w.getDescription());

        }
        return l;
    }

    void onLocationChanged(@NonNull Location l, @Nullable String reportType) {
        locationRepo.setCurrentLocation(l);
        publishLocationMessage(reportType);
    }


    void onWaypointTransition(@NonNull WaypointModel w, @NonNull final Location l, final int transition, @NonNull final String trigger) {
        Timber.v("geofence %s/%s transition:%s, trigger:%s", w.getTst(), w.getDescription(), transition == Geofence.GEOFENCE_TRANSITION_ENTER ? "enter" : "exit", trigger);

        if (ignoreLowAccuracy(l)) {
            Timber.d("ignoring transition: low accuracy ");
            return;
        }

        // Don't send transition if the region is already triggered
        // If the region status is unknown, send transition only if the device is inside
        if (((transition == w.getLastTransition()) || (w.isUnknown() && transition == Geofence.GEOFENCE_TRANSITION_EXIT))) {
            Timber.d("ignoring initial or duplicate transition");
            w.setLastTransition(transition);
            waypointsRepo.update(w, false);
            return;
        }

        w.setLastTransition(transition);
        w.setLastTriggeredNow();
        waypointsRepo.update(w, false);

        if(preferences.getMonitoring() ==MONITORING_QUIET) {
            Timber.v("message suppressed by monitoring settings: %s", preferences.getMonitoring());
            return;
        }

        publishTransitionMessage(w, l, transition, trigger);
        if (trigger.equals(MessageTransition.TRIGGER_CIRCULAR)) {
            publishLocationMessage(MessageLocation.REPORT_TYPE_CIRCULAR);
        }
    }

    public void publishWaypointMessage(@NonNull WaypointModel e) {
        messageProcessor.queueMessageForSending(waypointsRepo.fromDaoObject(e));
    }

    private void publishTransitionMessage(@NonNull WaypointModel w, @NonNull Location triggeringLocation, int transition, String trigger) {
        MessageTransition message = new MessageTransition();
        message.setTransition(transition);
        message.setTrigger(trigger);
        message.setTid(preferences.getTrackerId(true));
        message.setLat(triggeringLocation.getLatitude());
        message.setLon(triggeringLocation.getLongitude());
        message.setAcc(triggeringLocation.getAccuracy());
        message.setTst(TimeUnit.MILLISECONDS.toSeconds(System.currentTimeMillis()));
        message.setWtst(w.getTst());
        message.setDesc(w.getDescription());
        messageProcessor.queueMessageForSending(message);
    }


    public void publishWaypointsMessage() {
        MessageWaypoints message = new MessageWaypoints();
        MessageWaypointCollection collection = new MessageWaypointCollection();
        for(WaypointModel w : waypointsRepo.getAllWithGeofences()) {
            MessageWaypoint m = new MessageWaypoint();
            m.setDesc(w.getDescription());
            m.setLat(w.getGeofenceLatitude());
            m.setLon(w.getGeofenceLongitude());
            m.setRad(w.getGeofenceRadius());
            m.setTst(w.getTst());
            collection.add(m);
        }
        message.setWaypoints(collection);
        messageProcessor.queueMessageForSending(message);
    }


}
