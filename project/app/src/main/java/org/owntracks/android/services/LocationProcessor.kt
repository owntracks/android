package org.owntracks.android.services

import android.location.Location
import android.os.Build
import java.util.*
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton
import org.owntracks.android.data.WaypointModel
import org.owntracks.android.data.repos.LocationRepo
import org.owntracks.android.data.repos.WaypointsRepo
import org.owntracks.android.location.geofencing.Geofence
import org.owntracks.android.model.messages.MessageLocation
import org.owntracks.android.model.messages.MessageLocation.Companion.fromLocation
import org.owntracks.android.model.messages.MessageLocation.Companion.fromLocationAndWifiInfo
import org.owntracks.android.model.messages.MessageTransition
import org.owntracks.android.model.messages.MessageWaypoint
import org.owntracks.android.model.messages.MessageWaypoints
import org.owntracks.android.preferences.Preferences
import org.owntracks.android.preferences.types.MonitoringMode
import org.owntracks.android.support.DeviceMetricsProvider
import org.owntracks.android.support.MessageWaypointCollection
import timber.log.Timber

@Singleton
class LocationProcessor @Inject constructor(
    private val messageProcessor: MessageProcessor,
    private val preferences: Preferences,
    private val locationRepo: LocationRepo,
    private val waypointsRepo: WaypointsRepo,
    private val deviceMetricsProvider: DeviceMetricsProvider,
    private val wifiInfoProvider: WifiInfoProvider
) {
    private fun ignoreLowAccuracy(l: Location): Boolean {
        val threshold = preferences.ignoreInaccurateLocations
        val ignore = threshold > 0 && l.accuracy > threshold
        if (ignore) {
            Timber.d("Ignoring location (acc=${l.accuracy}) because it's below accuracy threshold of $threshold")
        }
        return ignore
    }

    @JvmOverloads
    fun publishLocationMessage(
        trigger: String?,
        location: Location? = locationRepo.currentPublishedLocation.value
    ) {
        if (location == null) return
        Timber.v("publishLocationMessage. trigger: %s", trigger)
        if (locationRepo.currentPublishedLocation.value == null) {
            Timber.e("no location available, can't publish location")
            return
        }
        val loadedWaypoints = waypointsRepo.allWithGeofences
        if (ignoreLowAccuracy(location)) return

        // Check if publish would trigger a region if fusedRegionDetection is enabled
        if (loadedWaypoints.isNotEmpty() && preferences.fusedRegionDetection && MessageLocation.REPORT_TYPE_CIRCULAR != trigger) {
            for (waypoint in loadedWaypoints) {
                onWaypointTransition(
                    waypoint,
                    location,
                    if (location.distanceTo(waypoint.location) <= waypoint.geofenceRadius + location.accuracy) {
                        Geofence.GEOFENCE_TRANSITION_ENTER
                    } else {
                        Geofence.GEOFENCE_TRANSITION_EXIT
                    },
                    MessageTransition.TRIGGER_LOCATION
                )
            }
        }
        if (preferences.monitoring === MonitoringMode.QUIET && MessageLocation.REPORT_TYPE_USER != trigger) {
            Timber.v("message suppressed by monitoring settings: quiet")
            return
        }
        if (preferences.monitoring === MonitoringMode.MANUAL &&
            MessageLocation.REPORT_TYPE_USER != trigger &&
            MessageLocation.REPORT_TYPE_CIRCULAR != trigger
        ) {
            Timber.v("message suppressed by monitoring settings: manual")
            return
        }
        val message: MessageLocation
        if (preferences.pubExtendedData) {
            message = fromLocationAndWifiInfo(location, wifiInfoProvider)
            message.battery = deviceMetricsProvider.batteryLevel
            message.batteryStatus = deviceMetricsProvider.batteryStatus
            message.conn = deviceMetricsProvider.connectionType
            message.monitoringMode = preferences.monitoring
        } else {
            message = fromLocation(location, Build.VERSION.SDK_INT)
        }
        message.trigger = trigger
        message.trackerId = preferences.tid.value
        message.inregions = calculateInregions(loadedWaypoints)
        messageProcessor.queueMessageForSending(message)
    }

    // TODO: refactor to use ObjectBox query directly
    private fun calculateInregions(loadedWaypoints: List<WaypointModel>): List<String> {
        val l = LinkedList<String>()
        for (w in loadedWaypoints) {
            if (w.lastTransition == Geofence.GEOFENCE_TRANSITION_ENTER) l.add(w.description)
        }
        return l
    }

    fun onLocationChanged(location: Location, reportType: String?) {
        locationRepo.setCurrentPublishedLocation(location)
        publishLocationMessage(reportType, location)
    }

    fun onWaypointTransition(
        waypointModel: WaypointModel,
        location: Location,
        transition: Int,
        trigger: String
    ) {
        Timber.v(
            "geofence ${waypointModel.tst}/${waypointModel.description} transition:${if (transition == Geofence.GEOFENCE_TRANSITION_ENTER) "enter" else "exit"}, trigger:$trigger"
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
            waypointsRepo.update(waypointModel, false)
            return
        }
        waypointModel.lastTransition = transition
        waypointModel.setLastTriggeredNow()
        waypointsRepo.update(waypointModel, false)
        if (preferences.monitoring === MonitoringMode.QUIET) {
            Timber.v("message suppressed by monitoring settings: %s", preferences.monitoring)
            return
        }
        publishTransitionMessage(waypointModel, location, transition, trigger)
        if (trigger == MessageTransition.TRIGGER_CIRCULAR) {
            publishLocationMessage(MessageLocation.REPORT_TYPE_CIRCULAR)
        }
    }

    fun publishWaypointMessage(e: WaypointModel) {
        messageProcessor.queueMessageForSending(waypointsRepo.fromDaoObject(e))
    }

    private fun publishTransitionMessage(
        w: WaypointModel,
        triggeringLocation: Location,
        transition: Int,
        trigger: String
    ) {
        messageProcessor.queueMessageForSending(
            MessageTransition().apply {
                setTransition(transition)
                this.trigger = trigger
                trackerId = preferences.tid.value
                latitude = triggeringLocation.latitude
                longitude = triggeringLocation.longitude
                accuracy = triggeringLocation.accuracy
                timestamp = TimeUnit.MILLISECONDS.toSeconds(triggeringLocation.time)
                waypointTimestamp = w.tst
                description = w.description
            }
        )
    }

    fun publishWaypointsMessage() {
        messageProcessor.queueMessageForSending(
            MessageWaypoints().apply {
                waypoints = MessageWaypointCollection().apply {
                    addAll(
                        waypointsRepo.allWithGeofences.map {
                            MessageWaypoint().apply {
                                description = it.description
                                latitude = it.geofenceLatitude
                                longitude = it.geofenceLongitude
                                radius = it.geofenceRadius
                                timestamp = it.tst
                            }
                        }
                    )
                }
            }
        )
    }
}
