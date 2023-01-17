package org.owntracks.android.services;

import android.location.Location;
import android.os.Build;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.owntracks.android.data.WaypointModel;
import org.owntracks.android.data.repos.LocationRepo;
import org.owntracks.android.data.repos.WaypointsRepo;
import org.owntracks.android.location.geofencing.Geofence;
import org.owntracks.android.model.messages.MessageLocation;
import org.owntracks.android.model.messages.MessageTransition;
import org.owntracks.android.model.messages.MessageWaypoint;
import org.owntracks.android.model.messages.MessageWaypoints;
import org.owntracks.android.support.DeviceMetricsProvider;
import org.owntracks.android.support.MessageWaypointCollection;
import org.owntracks.android.preferences.types.MonitoringMode;
import org.owntracks.android.preferences.Preferences;

import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;
import javax.inject.Singleton;

import timber.log.Timber;

@Singleton
public class LocationProcessor {
    private final MessageProcessor messageProcessor;
    private final Preferences preferences;
    private final LocationRepo locationRepo;
    private final WaypointsRepo waypointsRepo;
    private final DeviceMetricsProvider deviceMetricsProvider;
    private final WifiInfoProvider wifiInfoProvider;

    @Inject
    public LocationProcessor(MessageProcessor messageProcessor, Preferences preferences, LocationRepo locationRepo, WaypointsRepo waypointsRepo, DeviceMetricsProvider deviceMetricsProvider, WifiInfoProvider wifiInfoProvider) {
        this.messageProcessor = messageProcessor;
        this.preferences = preferences;
        this.deviceMetricsProvider = deviceMetricsProvider;
        this.locationRepo = locationRepo;
        this.waypointsRepo = waypointsRepo;
        this.wifiInfoProvider = wifiInfoProvider;
    }

    private boolean ignoreLowAccuracy(@NonNull Location l) {
        int threshold = preferences.getIgnoreInaccurateLocations();
        return threshold > 0 && l.getAccuracy() > threshold;
    }

    public void publishLocationMessage(@Nullable String trigger) {
        publishLocationMessage(trigger, locationRepo.getCurrentPublishedLocation().getValue());
    }

    public void publishLocationMessage(@Nullable String trigger, @NonNull Location location) {
        Timber.v("publishLocationMessage. trigger: %s. ThreadID: %s", trigger, Thread.currentThread());
        if (locationRepo.getCurrentPublishedLocation().getValue() == null) {
            Timber.e("no location available, can't publish location");
            return;
        }

        List<WaypointModel> loadedWaypoints = waypointsRepo.getAllWithGeofences();

        if (ignoreLowAccuracy(location)) {
            return;
        }

        // Check if publish would trigger a region if fusedRegionDetection is enabled
        if (loadedWaypoints.size() > 0 && preferences.getFusedRegionDetection() && !MessageLocation.REPORT_TYPE_CIRCULAR.equals(trigger)) {
            for (WaypointModel waypoint : loadedWaypoints) {
                onWaypointTransition(
                        waypoint,
                        location,
                        location.distanceTo(waypoint.getLocation()) <= (waypoint.getGeofenceRadius() + location.getAccuracy()) ? Geofence.GEOFENCE_TRANSITION_ENTER : Geofence.GEOFENCE_TRANSITION_EXIT,
                        MessageTransition.TRIGGER_LOCATION
                );
            }
        }

        if (preferences.getMonitoring() == MonitoringMode.QUIET && !MessageLocation.REPORT_TYPE_USER.equals(trigger)) {
            Timber.v("message suppressed by monitoring settings: quiet");
            return;
        }

        if (preferences.getMonitoring() == MonitoringMode.MANUAL && (!MessageLocation.REPORT_TYPE_USER.equals(trigger) && !MessageLocation.REPORT_TYPE_CIRCULAR.equals(trigger))) {
            Timber.v("message suppressed by monitoring settings: manual");
            return;
        }

        MessageLocation message;

        if (preferences.getPubExtendedData()) {
            message = MessageLocation.fromLocationAndWifiInfo(location, wifiInfoProvider);
            message.setBattery(deviceMetricsProvider.getBatteryLevel());
            message.setBatteryStatus(deviceMetricsProvider.getBatteryStatus());
            message.setConn(deviceMetricsProvider.getConnectionType());
            message.setMonitoringMode(preferences.getMonitoring());
        } else {
            message = MessageLocation.fromLocation(location, Build.VERSION.SDK_INT);
        }
        message.setTrigger(trigger);
        message.setTrackerId(preferences.getTid().getValue());
        message.setInregions(calculateInregions(loadedWaypoints));

        messageProcessor.queueMessageForSending(message);
    }

    //TODO: refactor to use ObjectBox query directly
    private List<String> calculateInregions(List<WaypointModel> loadedWaypoints) {
        LinkedList<String> l = new LinkedList<>();
        for (WaypointModel w : loadedWaypoints) {
            if (w.getLastTransition() == Geofence.GEOFENCE_TRANSITION_ENTER)
                l.add(w.getDescription());

        }
        return l;
    }

    public void onLocationChanged(@NonNull Location location, @Nullable String reportType) {
        locationRepo.setCurrentPublishedLocation(location);
        publishLocationMessage(reportType, location);
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

        if (preferences.getMonitoring() == MonitoringMode.QUIET) {
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
        message.setTrackerId(preferences.getTid().getValue());
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
        for (WaypointModel w : waypointsRepo.getAllWithGeofences()) {
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
