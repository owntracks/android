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
import org.owntracks.android.model.messages.MessageLocation;
import org.owntracks.android.model.messages.MessageTransition;
import org.owntracks.android.model.messages.MessageWaypoint;
import org.owntracks.android.model.messages.MessageWaypoints;
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
    public static final int MONITORING_SIGNIFICANT = 1;
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
        Timber.v("trigger: %s. ThreadID: %s", trigger, Thread.currentThread());
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
        if(loadedWaypoints.size() > 0 && preferences.getFusedRegionDetection() && !MessageLocation.REPORT_TYPE_CIRCULAR.equals(trigger)) {
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
        message.setLatitude(currentLocation.getLatitude());
        message.setLongitude(currentLocation.getLongitude());
        message.setAltitude((int)currentLocation.getAltitude());
        message.setAccuracy(Math.round(currentLocation.getAccuracy()));
        if (currentLocation.hasSpeed()) {
            message.setVelocity((int)(currentLocation.getSpeed() * 3.6)); // Convert m/s to km/h
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && currentLocation.hasVerticalAccuracy()) {
            message.setVerticalAccuracy((int)currentLocation.getVerticalAccuracyMeters());
        }
        message.setTrigger(trigger);

        message.setTimestamp(TimeUnit.MILLISECONDS.toSeconds(currentLocation.getTime()));

        message.setTrackerId(preferences.getTrackerId(true));
        message.setInregions(calculateInregions(loadedWaypoints));

        if (preferences.getPubLocationExtendedData()) {
            message.setBattery(deviceMetricsProvider.getBatteryLevel());
            message.setBatteryStatus(deviceMetricsProvider.getBatteryStatus());
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


    void onWaypointTransition(@NonNull WaypointModel waypointModel, @NonNull final Location location, final int transition, @NonNull final String trigger) {
        Timber.v("geofence %s/%s transition:%s, trigger:%s", waypointModel.getTst(), waypointModel.getDescription(), transition == Geofence.GEOFENCE_TRANSITION_ENTER ? "enter" : "exit", trigger);

        if (ignoreLowAccuracy(location)) {
            Timber.d("ignoring transition: low accuracy ");
            return;
        }

        // Don't send transition if the region is already triggered
        // If the region status is unknown, send transition only if the device is inside
        if (((transition == waypointModel.getLastTransition()) || (waypointModel.isUnknown() && transition == Geofence.GEOFENCE_TRANSITION_EXIT))) {
            Timber.d("ignoring initial or duplicate transition: %s", waypointModel.getDescription());
            waypointModel.setLastTransition(transition);
            waypointsRepo.update(waypointModel, false);
            return;
        }

        waypointModel.setLastTransition(transition);
        waypointModel.setLastTriggeredNow();
        waypointsRepo.update(waypointModel, false);

        if(preferences.getMonitoring() ==MONITORING_QUIET) {
            Timber.v("message suppressed by monitoring settings: %s", preferences.getMonitoring());
            return;
        }

        publishTransitionMessage(waypointModel, location, transition, trigger);
        if (trigger.equals(MessageTransition.TRIGGER_CIRCULAR)) {
            publishLocationMessage(MessageLocation.REPORT_TYPE_CIRCULAR);
        }
    }

    void publishWaypointMessage(@NonNull WaypointModel e) {
        messageProcessor.queueMessageForSending(waypointsRepo.fromDaoObject(e));
    }

    private void publishTransitionMessage(@NonNull WaypointModel w, @NonNull Location triggeringLocation, int transition, String trigger) {
        MessageTransition message = new MessageTransition();
        message.setTransition(transition);
        message.setTrigger(trigger);
        message.setTrackerId(preferences.getTrackerId(true));
        message.setLatitude(triggeringLocation.getLatitude());
        message.setLongitude(triggeringLocation.getLongitude());
        message.setAccuracy(triggeringLocation.getAccuracy());
        message.setTimestamp(TimeUnit.MILLISECONDS.toSeconds(triggeringLocation.getTime()));
        message.setWaypointTimestamp(w.getTst());
        message.setDescription(w.getDescription());
        messageProcessor.queueMessageForSending(message);
    }

    public void publishWaypointsMessage() {
        MessageWaypoints message = new MessageWaypoints();
        MessageWaypointCollection collection = new MessageWaypointCollection();
        for(WaypointModel w : waypointsRepo.getAllWithGeofences()) {
            MessageWaypoint m = new MessageWaypoint();
            m.setDescription(w.getDescription());
            m.setLatitude(w.getGeofenceLatitude());
            m.setLongitude(w.getGeofenceLongitude());
            m.setRadius(w.getGeofenceRadius());
            m.setTimestamp(w.getTst());
            collection.add(m);
        }
        message.setWaypoints(collection);
        messageProcessor.queueMessageForSending(message);
    }
}
