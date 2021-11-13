package org.owntracks.android.services

import android.location.Location
import org.owntracks.android.data.WaypointModel
import org.owntracks.android.data.repos.LocationRepo
import org.owntracks.android.data.repos.WaypointsRepo
import org.owntracks.android.location.Geofence
import org.owntracks.android.model.messages.MessageLocation
import org.owntracks.android.model.messages.MessageLocation.Companion.fromLocation
import org.owntracks.android.model.messages.MessageLocation.Companion.fromLocationAndWifiInfo
import org.owntracks.android.model.messages.MessageTransition
import org.owntracks.android.model.messages.MessageWaypoint
import org.owntracks.android.model.messages.MessageWaypoints
import org.owntracks.android.support.DeviceMetricsProvider
import org.owntracks.android.support.Preferences
import timber.log.Timber
import java.util.*
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LocationProcessor @Inject constructor(
        private val messageProcessor: MessageProcessor,
        private val preferences: Preferences,
        private val locationRepo: LocationRepo,
        private val waypointsRepo: WaypointsRepo,
        private val deviceMetricsProvider: DeviceMetricsProvider,
        private val wifiInfoProvider: WifiInfoProvider
) {
    fun publishLocationMessage(trigger: String?) {
        val currentLocation = locationRepo.currentPublishedLocation.value
        currentLocation?.let { publishLocationMessage(trigger, it) }
    }

    private fun publishLocationMessage(trigger: String?, currentLocation: Location) {
        if (locationRepo.currentPublishedLocation.value == null) {
            Timber.e("no location available")
            return
        }
        if (ignoreLowAccuracy(currentLocation)) {
            Timber.d(
                    "Ignoring location %s,%s as below accuracy threshold: %s",
                    currentLocation.latitude,
                    currentLocation.longitude,
                    currentLocation.accuracy
            )
            return
        }

        // Check if publish would trigger a region if fusedRegionDetection is enabled
        if (preferences.fusedRegionDetection) {
            waypointsRepo.allWithGeofences.forEach {
                onWaypointTransition(
                        it,
                        currentLocation,
                        if (currentLocation.distanceTo(it.location) <= (it.geofenceRadius + currentLocation.accuracy)) Geofence.GEOFENCE_TRANSITION_ENTER else Geofence.GEOFENCE_TRANSITION_EXIT
                )
            }
        }

        if (preferences.monitoring == MONITORING_QUIET && MessageLocation.REPORT_TYPE_USER != trigger) {
            Timber.d("location message suppressed by monitoring settings: quiet")
            return
        }
        if (preferences.monitoring == MONITORING_MANUAL && MessageLocation.REPORT_TYPE_USER != trigger) {
            Timber.d("location message suppressed by monitoring settings: manual")
            return
        }
        val message: MessageLocation
        if (preferences.pubLocationExtendedData) {
            message = fromLocationAndWifiInfo(currentLocation, wifiInfoProvider)
            message.battery = deviceMetricsProvider.batteryLevel
            message.batteryStatus = deviceMetricsProvider.batteryStatus
            message.conn = deviceMetricsProvider.connectionType
        } else {
            message = fromLocation(currentLocation)
        }
        message.trigger = trigger
        message.trackerId = preferences.getTrackerId(true)
        message.inregions = waypointsRepo.allWithGeofences
                .filter { it.lastTransition == Geofence.GEOFENCE_TRANSITION_ENTER }
                .map { it.description }
        messageProcessor.queueMessageForSending(message)
    }

    private fun ignoreLowAccuracy(l: Location): Boolean {
        val threshold = preferences.ignoreInaccurateLocations
        return threshold > 0 && l.accuracy > threshold
    }

    fun onLocationChanged(location: Location, reportType: String?) {
        locationRepo.setCurrentPublishedLocation(location)
        publishLocationMessage(reportType, location)
    }

    private fun onWaypointTransition(
            waypointModel: WaypointModel,
            location: Location,
            transition: Int,
            trigger: String = MessageTransition.TRIGGER_LOCATION
    ) {
        Timber.d(
                "geofence %s/%s transition:%s, trigger:%s",
                waypointModel.tst,
                waypointModel.description,
                if (transition == Geofence.GEOFENCE_TRANSITION_ENTER) "enter" else "exit",
                trigger
        )
        if (ignoreLowAccuracy(location)) {
            Timber.d("ignoring transition: low accuracy ")
            return
        }

        // Don't send transition if the region is already triggered
        // If the region status is unknown, send transition only if the device is inside
        if (transition == waypointModel.lastTransition || waypointModel.isUnknown && transition == Geofence.GEOFENCE_TRANSITION_EXIT) {
            Timber.d("ignoring initial or duplicate transition: %s", waypointModel.description)
            waypointModel.lastTransition = transition
            waypointsRepo.update(waypointModel)
            return
        }
        waypointModel.lastTransition = transition
        waypointModel.setLastTriggeredNow()
        waypointsRepo.update(waypointModel)
        if (preferences.monitoring == MONITORING_QUIET) {
            Timber.v("message suppressed by monitoring settings: %s", preferences.monitoring)
            return
        }
        publishTransitionMessage(waypointModel, location, transition, trigger)
    }

    private fun publishTransitionMessage(
            w: WaypointModel,
            triggeringLocation: Location,
            transition: Int,
            trigger: String
    ) {
        val message = MessageTransition()
        message.setTransition(transition)
        message.trigger = trigger
        message.trackerId = preferences.getTrackerId(true)
        message.latitude = triggeringLocation.latitude
        message.longitude = triggeringLocation.longitude
        message.accuracy = triggeringLocation.accuracy
        message.timestamp = TimeUnit.MILLISECONDS.toSeconds(triggeringLocation.time)
        message.waypointTimestamp = w.tst
        message.description = w.description
        messageProcessor.queueMessageForSending(message)
    }

    fun publishWaypointsMessage() {
        val message = MessageWaypoints()
        val collection: MutableList<MessageWaypoint> = ArrayList()
        for (w in waypointsRepo.allWithGeofences) {
            val m = MessageWaypoint()
            m.description = w.description
            m.latitude = w.geofenceLatitude
            m.longitude = w.geofenceLongitude
            m.radius = w.geofenceRadius
            m.timestamp = w.tst
            collection.add(m)
        }
        message.waypoints = collection
        messageProcessor.queueMessageForSending(message)
    }

    companion object {
        const val MONITORING_QUIET = -1
        const val MONITORING_MANUAL = 0
        const val MONITORING_SIGNIFICANT = 1
        const val MONITORING_MOVE = 2
    }
}