package org.owntracks.android.services

import android.location.Location
import android.os.Build
import java.time.Duration
import java.time.Instant
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton
import kotlin.math.roundToInt
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.owntracks.android.data.repos.LocationRepo
import org.owntracks.android.data.waypoints.WaypointModel
import org.owntracks.android.data.waypoints.WaypointsRepo
import org.owntracks.android.di.ApplicationScope
import org.owntracks.android.di.CoroutineScopes
import org.owntracks.android.location.geofencing.Geofence
import org.owntracks.android.model.messages.AddMessageStatus
import org.owntracks.android.model.messages.MessageLocation
import org.owntracks.android.model.messages.MessageLocation.Companion.fromLocation
import org.owntracks.android.model.messages.MessageStatus
import org.owntracks.android.model.messages.MessageTransition
import org.owntracks.android.model.messages.MessageWaypoint
import org.owntracks.android.model.messages.MessageWaypoints
import org.owntracks.android.model.messages.addWifi
import org.owntracks.android.net.WifiInfoProvider
import org.owntracks.android.preferences.Preferences
import org.owntracks.android.preferences.types.MonitoringMode
import org.owntracks.android.support.DeviceMetricsProvider
import org.owntracks.android.support.MessageWaypointCollection
import org.owntracks.android.test.SimpleIdlingResource
import timber.log.Timber

/**
 * A waypoint transition candidate that has been observed but not yet committed, because it hasn't
 * persisted for long enough to rule out a fix landing right on the waypoint boundary.
 */
internal data class PendingWaypointTransition(val transition: Int, val since: Instant)

/**
 * Debounces a waypoint transition candidate against previously-pending state. A candidate is only
 * committed once it has been observed consistently for at least [dwell]; a candidate that differs
 * from the currently-pending one resets the dwell timer.
 *
 * @return a pair of (transition to commit, if any) to (new pending state, if any)
 */
internal fun resolveTransitionDebounce(
    pending: PendingWaypointTransition?,
    candidate: Int,
    now: Instant,
    dwell: Duration
): Pair<Int?, PendingWaypointTransition?> =
    if (pending == null || pending.transition != candidate) {
      null to PendingWaypointTransition(candidate, now)
    } else if (Duration.between(pending.since, now) >= dwell) {
      candidate to null
    } else {
      null to pending
    }

internal fun impliedSpeedKmh(distanceMeters: Double, timeDeltaMillis: Long): Double =
    (distanceMeters / (timeDeltaMillis / 1000.0)) * 3.6

internal fun isImplausibleSpeed(
    distanceMeters: Double,
    timeDeltaMillis: Long,
    maxSpeedKmh: Int
): Boolean {
  if (maxSpeedKmh <= 0 || timeDeltaMillis <= 0) {
    return false
  }
  return impliedSpeedKmh(distanceMeters, timeDeltaMillis) > maxSpeedKmh
}

@Singleton
class LocationProcessor
@Inject
constructor(
  private val messageProcessor: MessageProcessor,
  private val preferences: Preferences,
  private val locationRepo: LocationRepo,
  private val waypointsRepo: WaypointsRepo,
  private val deviceMetricsProvider: DeviceMetricsProvider,
  private val wifiInfoProvider: WifiInfoProvider,
  @param:ApplicationScope private val scope: CoroutineScope,
  @param:CoroutineScopes.IoDispatcher private val ioDispatcher: CoroutineDispatcher,
  @param:Named("publishResponseMessageIdlingResource")
    private val publishResponseMessageIdlingResource: SimpleIdlingResource,
  @param:Named("mockLocationIdlingResource")
    private val mockLocationIdlingResource: SimpleIdlingResource,
  @param:Named("nativeGeofencingAvailable") private val nativeGeofencingAvailable: Boolean
) {
  private fun locationIsWithAccuracyThreshold(l: Location): Boolean =
      preferences.ignoreInaccurateLocations
          .run { preferences.ignoreInaccurateLocations == 0 || l.accuracy < this }
          .also {
            if (!it) {
              Timber.d(
                  "Location accuracy ${l.accuracy} is outside accuracy threshold of ${preferences.ignoreInaccurateLocations}")
            }
          }

  suspend fun publishLocationMessage(trigger: MessageLocation.ReportType) =
      locationRepo.currentPublishedLocation.value?.run { publishLocationMessage(trigger, this) }

  private val highAccuracyProviders = setOf("gps", "fused")

  // Matches the notificationResponsiveness used for native GMS geofencing, so both flavors have
  // comparable real-world hysteresis around a waypoint boundary.
  private val transitionDebounceDwell: Duration = Duration.ofMinutes(2)
  private val pendingWaypointTransitions = mutableMapOf<Long, PendingWaypointTransition>()

  private suspend fun publishLocationMessage(
      trigger: MessageLocation.ReportType,
      location: Location
  ): Result<Unit> {
    Timber.v("Maybe publishing $location with trigger $trigger")
    if (!locationIsWithAccuracyThreshold(location))
        return Result.failure(Exception("location accuracy too low"))

    // If this location has come from the network *and* the most recent location was both recent and
    // high-accuracy, then it's probably not usefully accurate. Drop it.
    locationRepo.currentPublishedLocation.value?.let { lastLocation ->
      if (highAccuracyProviders.contains(location.provider) &&
          lastLocation.provider == "network" &&
          location.time - lastLocation.time <
              preferences.discardNetworkLocationThresholdSeconds * 1000) {
        Timber.d(
            "Ignoring location from ${location.provider}, last was from gps, and time difference is less than 1s")
        return Result.failure(
            Exception("Ignoring location from ${location.provider}, last was recent and from gps"))
      }
    }

    val maxImplausibleSpeedKmh = preferences.maxImplausibleSpeedKmh
    if (maxImplausibleSpeedKmh > 0 && trigger !in responseMessageTypes) {
      locationRepo.currentPublishedLocation.value?.let { lastLocation ->
        val timeDeltaMillis = location.time - lastLocation.time
        if (timeDeltaMillis > 0) {
          val distanceMeters = location.distanceTo(lastLocation)
          if (isImplausibleSpeed(
              distanceMeters.toDouble(), timeDeltaMillis, maxImplausibleSpeedKmh)) {
            val speedKmh = impliedSpeedKmh(distanceMeters.toDouble(), timeDeltaMillis)
            Timber.d(
                "Ignoring location from ${location.provider}, implied speed ${speedKmh.roundToInt()}km/h exceeds plausible maximum ${maxImplausibleSpeedKmh}km/h")
            return Result.failure(Exception("Ignoring location due to implausible speed"))
          }
        }
      }
    }

    val loadedWaypoints = withContext(ioDispatcher) { waypointsRepo.getAll() }
    Timber.d("publishLocationMessage for $location triggered by $trigger")

    // Check if publish would trigger a region if fusedRegionDetection is enabled. Skipped entirely
    // where native OS geofencing is available (e.g. gms) - that's a purpose-built mechanism with its
    // own hysteresis, and running this alongside it causes the two to race and flip-flop on the same
    // waypoint state. Where it isn't available (e.g. oss), a transition candidate must instead be
    // observed consistently for transitionDebounceDwell before being committed, for the same reason.
    Timber.d(
        "Checking if location triggers waypoint transitions. waypoints: $loadedWaypoints, trigger=$trigger, fusedRegionDetection: ${preferences.fusedRegionDetection}, nativeGeofencingAvailable: $nativeGeofencingAvailable")
    if (loadedWaypoints.isNotEmpty() &&
        preferences.fusedRegionDetection &&
        !nativeGeofencingAvailable &&
        trigger != MessageLocation.ReportType.CIRCULAR) {
      pendingWaypointTransitions.keys.retainAll(loadedWaypoints.map { it.id }.toSet())
      loadedWaypoints.forEach { waypoint ->
        val candidate =
            if (location.distanceTo(waypoint.getLocation()) <=
                waypoint.geofenceRadius + location.accuracy) {
              Geofence.GEOFENCE_TRANSITION_ENTER
            } else {
              Geofence.GEOFENCE_TRANSITION_EXIT
            }
        if (candidate == waypoint.lastTransition) {
          pendingWaypointTransitions.remove(waypoint.id)
          Timber.d("onWaypointTransition triggered by location waypoint intersection event")
          onWaypointTransition(waypoint, location, candidate, MessageTransition.TRIGGER_LOCATION)
        } else {
          val (transitionToCommit, newPending) =
              resolveTransitionDebounce(
                  pendingWaypointTransitions[waypoint.id],
                  candidate,
                  Instant.ofEpochMilli(location.time),
                  transitionDebounceDwell)
          if (newPending == null) {
            pendingWaypointTransitions.remove(waypoint.id)
          } else {
            pendingWaypointTransitions[waypoint.id] = newPending
          }
          if (transitionToCommit != null) {
            Timber.d("onWaypointTransition triggered by location waypoint intersection event")
            onWaypointTransition(
                waypoint, location, transitionToCommit, MessageTransition.TRIGGER_LOCATION)
          }
        }
      }
    }
    if (preferences.monitoring === MonitoringMode.Quiet &&
        MessageLocation.ReportType.USER != trigger) {
      Timber.d("message suppressed by monitoring settings: quiet")
      return Result.failure(Exception("message suppressed by monitoring settings: quiet"))
    }
    if (preferences.monitoring === MonitoringMode.Manual &&
        MessageLocation.ReportType.USER != trigger &&
        MessageLocation.ReportType.CIRCULAR != trigger) {
      Timber.d("message suppressed by monitoring settings: manual")
      return Result.failure(Exception("message suppressed by monitoring settings: manual"))
    }

    val message =
        if (preferences.extendedData) {
              fromLocation(location, Build.VERSION.SDK_INT).apply {
                addWifi(wifiInfoProvider)
                battery = deviceMetricsProvider.batteryLevel
                batteryStatus = deviceMetricsProvider.batteryStatus
                conn = deviceMetricsProvider.connectionType.value
                monitoringMode = preferences.monitoring
                source = location.provider
              }
            } else {
              fromLocation(location, Build.VERSION.SDK_INT)
            }
            .apply {
              this.trigger = trigger
              trackerId = preferences.tid.toString()
              inregions = calculateInRegions(loadedWaypoints)
            }
    Timber.v("Actually publishing location $location triggered by $trigger as message=$message")
    messageProcessor.queueMessageForSending(message)
    if (responseMessageTypes.contains(trigger)) {
      publishResponseMessageIdlingResource.setIdleState(true)
    }
    return Result.success(Unit)
  }

  private val responseMessageTypes =
      listOf(
          MessageLocation.ReportType.RESPONSE,
          MessageLocation.ReportType.USER,
          MessageLocation.ReportType.CIRCULAR)

  private fun calculateInRegions(loadedWaypoints: List<WaypointModel>): List<String> =
      loadedWaypoints
          .filter { it.lastTransition == Geofence.GEOFENCE_TRANSITION_ENTER }
          .map { it.description }
          .toList()

  /**
   * Called when a new location is received from the device, or directly from the user via the map
   *
   * @param location received from the device
   * @param reportType type of report that
   */
  suspend fun onLocationChanged(location: Location, reportType: MessageLocation.ReportType) {
    Timber.v("OnLocationChanged $location $reportType")
    if (location.time > locationRepo.currentLocationTime ||
        reportType != MessageLocation.ReportType.DEFAULT) {
      if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S || location.isMock) {
        Timber.v("Idling location")
        mockLocationIdlingResource.setIdleState(true)
      }
      publishLocationMessage(reportType, location).run {
        if (isSuccess) {
          locationRepo.setCurrentPublishedLocation(location)
        } else {
          Timber.d("Not publishing location: ${exceptionOrNull()?.message}")
        }
      }
    } else {
      Timber.v("Not re-sending message with same timestamp as last")
    }
  }

  fun onWaypointTransition(
      waypointModel: WaypointModel,
      location: Location,
      transition: Int,
      trigger: String
  ) {
    if (!locationIsWithAccuracyThreshold(location)) {
      Timber.d(
          "ignoring transition for $location, transition=$transition, trigger=$trigger: low accuracy")
      return
    }
    Timber.d("OnWaypointTransition $waypointModel $location $transition $trigger")
    scope.launch {
      // If the transition hasn't changed, or has moved from unknown to exit, don't notify.
      if (transition == waypointModel.lastTransition ||
          (waypointModel.isUnknown() && transition == Geofence.GEOFENCE_TRANSITION_EXIT)) {
        waypointModel.lastTransition = transition
        waypointsRepo.update(waypointModel, false)
      } else {
        waypointModel.lastTransition = transition
        waypointModel.lastTriggered = Instant.now()
        waypointsRepo.update(waypointModel, false)
        if (preferences.monitoring === MonitoringMode.Quiet) {
          Timber.d("message suppressed by monitoring settings: ${preferences.monitoring}")
        } else {
          publishTransitionMessage(waypointModel, location, transition, trigger)
          if (trigger == MessageTransition.TRIGGER_CIRCULAR) {
            publishLocationMessage(MessageLocation.ReportType.CIRCULAR, location)
          }
        }
      }
    }
  }

  fun publishWaypointMessage(e: WaypointModel) {
    messageProcessor.queueMessageForSending(waypointsRepo.fromDaoObject(e))
  }

  private fun publishTransitionMessage(
      waypointModel: WaypointModel,
      triggeringLocation: Location,
      transition: Int,
      trigger: String
  ) {
    messageProcessor.queueMessageForSending(
        MessageTransition().apply {
          setTransition(transition)
          this.trigger = trigger
          trackerId = preferences.tid.toString()
          latitude = triggeringLocation.latitude
          longitude = triggeringLocation.longitude
          accuracy = triggeringLocation.accuracy.roundToInt()
          timestamp = TimeUnit.MILLISECONDS.toSeconds(triggeringLocation.time)
          waypointTimestamp = waypointModel.tst.epochSecond
          description = waypointModel.description
        })
  }

  suspend fun publishWaypointsMessage() {
    messageProcessor.queueMessageForSending(
        MessageWaypoints().apply {
          waypoints =
              MessageWaypointCollection().apply {
                withContext(ioDispatcher) {
                  addAll(
                      waypointsRepo.getAll().map {
                        MessageWaypoint().apply {
                          description = it.description
                          latitude = it.geofenceLatitude.value
                          longitude = it.geofenceLongitude.value
                          radius = it.geofenceRadius
                          timestamp = it.tst.epochSecond
                        }
                      })
                }
              }
        })
    publishResponseMessageIdlingResource.setIdleState(true)
  }

  fun publishStatusMessage() {
    // Getting appHibernation takes a while, so lets not block the main thread
    scope.launch(ioDispatcher) {
      messageProcessor.queueMessageForSending(
          MessageStatus().apply {
            android =
                AddMessageStatus().apply {
                  wifistate = wifiInfoProvider.isWiFiEnabled()
                  powerSave = deviceMetricsProvider.powerSave
                  batteryOptimizations = deviceMetricsProvider.batteryOptimizations
                  appHibernation = deviceMetricsProvider.appHibernation
                  locationPermission = deviceMetricsProvider.locationPermission
                }
          })
      publishResponseMessageIdlingResource.setIdleState(true)
    }
  }
}
