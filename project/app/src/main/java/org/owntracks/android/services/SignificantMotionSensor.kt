// Assisted-by: Claude Code IDE; model: claude-4.5-sonnet

package org.owntracks.android.services

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorManager
import android.hardware.TriggerEvent
import android.hardware.TriggerEventListener
import android.os.Looper
import android.os.SystemClock
import java.util.concurrent.TimeUnit
import org.owntracks.android.location.LocationCallback
import org.owntracks.android.location.LocationProviderClient
import org.owntracks.android.preferences.Preferences
import org.owntracks.android.preferences.types.MonitoringMode
import org.owntracks.android.support.RequirementsChecker
import timber.log.Timber

/**
 * Manages the significant motion sensor for triggering location requests when device movement is
 * detected.
 *
 * The significant motion sensor is a one-shot wake-up sensor that triggers when the device detects
 * significant movement (like walking). After triggering, the listener must be re-registered.
 */
class SignificantMotionSensor(
    private val context: Context,
    private val preferences: Preferences,
    private val locationProviderClient: LocationProviderClient,
    private val requirementsChecker: RequirementsChecker,
    private val locationCallback: LocationCallback,
    private val looper: Looper
) {
  private val sensorManager by lazy {
    context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
  }

  private val significantMotionSensor: Sensor? by lazy {
    sensorManager.getDefaultSensor(Sensor.TYPE_SIGNIFICANT_MOTION)
  }

  // Track when we last requested location due to significant motion (for rate limiting)
  @Volatile private var lastSignificantMotionLocationRequestTime: Long = 0L

  private val significantMotionTriggerListener =
      object : TriggerEventListener() {
        override fun onTrigger(event: TriggerEvent?) {
          Timber.d("Significant motion detected")
          onSignificantMotionDetected()
        }
      }

  /**
   * Sets up the significant motion sensor listener. This sensor triggers a location request when
   * significant movement is detected.
   */
  fun setup() {
    if (!preferences.experimentalFeatures.contains(
        Preferences.EXPERIMENTAL_FEATURE_REQUEST_LOCATION_ON_SIGNIFICANT_MOTION)) {
      Timber.d("Significant motion sensor disabled (experimental feature not enabled)")
      return
    }
    significantMotionSensor?.let { sensor ->
      Timber.d(
          "Found significant motion sensor: ${sensor.name} (vendor: ${sensor.vendor}, isWakeUpSensor: ${sensor.isWakeUpSensor})")
      // Cancel any existing registration before requesting a new one
      // (requestTriggerSensor fails if listener is already registered)
      sensorManager.cancelTriggerSensor(significantMotionTriggerListener, sensor)
      val success = sensorManager.requestTriggerSensor(significantMotionTriggerListener, sensor)
      if (success) {
        Timber.d("Significant motion sensor listener registered successfully")
      } else {
        Timber.i("Failed to register significant motion sensor listener")
      }
    }
        ?: run {
          // Log available sensors to help diagnose issues
          val allSensors = sensorManager.getSensorList(Sensor.TYPE_ALL)
          val motionRelatedSensors =
              allSensors.filter {
                it.type == Sensor.TYPE_SIGNIFICANT_MOTION ||
                    it.name.contains("motion", ignoreCase = true) ||
                    it.name.contains("movement", ignoreCase = true)
              }
          Timber.i(
              "Significant motion sensor not available. Motion-related sensors found: ${motionRelatedSensors.map { "${it.name} (type=${it.type})" }}")
        }
  }

  /**
   * Cancels the significant motion sensor listener. Should be called when the feature is disabled
   * or when cleaning up resources.
   */
  fun cancel() {
    significantMotionSensor?.let { sensor ->
      sensorManager.cancelTriggerSensor(significantMotionTriggerListener, sensor)
      Timber.d("Significant motion sensor listener cancelled")
    }
  }

  /**
   * Called when significant motion is detected. Requests a high-accuracy GPS location if enough
   * time has passed since the last request (rate limited based on
   * pegLocatorFastestIntervalToInterval, locatorInterval and moveModeLocatorInterval). Always
   * re-registers the trigger listener (since TYPE_SIGNIFICANT_MOTION is a one-shot sensor).
   */
  private fun onSignificantMotionDetected() {
    // Re-register the trigger listener first (TYPE_SIGNIFICANT_MOTION is a one-shot sensor)
    // We do this regardless of rate limiting so we don't miss future motion events
    setup()

    // Rate limit: use the same interval logic as BackgroundService location requests
    val now = SystemClock.elapsedRealtime()
    val intervalSeconds =
        if (preferences.monitoring == MonitoringMode.Move) {
          preferences.moveModeLocatorInterval
        } else {
          preferences.locatorInterval
        }
    val minIntervalMs =
        if (preferences.pegLocatorFastestIntervalToInterval) {
          TimeUnit.SECONDS.toMillis(intervalSeconds.toLong())
        } else {
          TimeUnit.SECONDS.toMillis(1)
        }
    val timeSinceLastRequest = now - lastSignificantMotionLocationRequestTime

    if (timeSinceLastRequest < minIntervalMs) {
      Timber.d(
          "Significant motion detected but rate limited. " +
              "Time since last request: ${timeSinceLastRequest}ms, min interval: ${minIntervalMs}ms")
      return
    }

    if (requirementsChecker.hasLocationPermissions()) {
      Timber.d("Requesting high-accuracy location due to significant motion detection")
      lastSignificantMotionLocationRequestTime = now
      locationProviderClient.singleHighAccuracyLocation(locationCallback, looper)
    } else {
      Timber.e("Missing location permission, cannot request location for significant motion")
    }
  }
}
