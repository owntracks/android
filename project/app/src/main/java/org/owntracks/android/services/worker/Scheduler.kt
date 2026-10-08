package org.owntracks.android.services.worker

import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequest
import androidx.work.PeriodicWorkRequest
import androidx.work.WorkManager
import androidx.work.WorkRequest
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.launch
import org.owntracks.android.di.ApplicationScope
import org.owntracks.android.location.profiles.ContextProfileManager
import org.owntracks.android.location.profiles.LocatorSettings
import timber.log.Timber

@OptIn(FlowPreview::class)
@Singleton
class Scheduler
@Inject
constructor(
    private val contextProfileManager: ContextProfileManager,
    @param:ApplicationContext private val context: Context,
    @ApplicationScope scope: CoroutineScope,
) {
  private val anyNetworkConstraint =
      Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build()
  private val workManager = WorkManager.getInstance(context)

  /**
   * Whether the location ping has been asked for, as it isn't scheduled until profiles are known
   */
  @Volatile private var locationPingRequested = false

  /** The settings that the location ping is currently scheduled with */
  @Volatile private var scheduledPingSettings: LocatorSettings? = null

  init {
    // The ping can change either from the preferences or from a context profile
    scope.launch {
      var first = true
      combine(contextProfileManager.ready, contextProfileManager.locatorSettings) { ready, settings
            ->
            settings.takeIf { ready }
          }
          .filterNotNull()
          .distinctUntilChanged { old, new -> old.pingKey() == new.pingKey() }
          // So that e.g. Wi-Fi briefly dropping out doesn't send a ping each way
          .debounce { if (first) Duration.ZERO.also { first = false } else PING_CHANGE_DEBOUNCE }
          .collect { settings ->
            if (locationPingRequested && settings.pingKey() != scheduledPingSettings?.pingKey()) {
              scheduleLocationPing(settings)
            }
          }
    }
  }

  /** Used by the background service to periodically ping a location */
  fun scheduleLocationPing() {
    locationPingRequested = true
    if (contextProfileManager.ready.value) {
      scheduleLocationPing(contextProfileManager.locatorSettings.value)
    } else {
      Timber.d("Not scheduling the location ping until it's known which context profile applies")
    }
  }

  private fun scheduleLocationPing(settings: LocatorSettings) {
    val schedule = pingScheduleFor(settings)
    val pingWorkRequest: WorkRequest =
        PeriodicWorkRequest.Builder(
                SendLocationPingWorker::class.java,
                schedule.interval.inWholeMinutes,
                TimeUnit.MINUTES,
            )
            .setInitialDelay(schedule.initialDelay.inWholeMinutes, TimeUnit.MINUTES)
            .addTag(PERIODIC_TASK_SEND_LOCATION_PING)
            .setConstraints(anyNetworkConstraint)
            .build()
    Timber.d(
        "WorkManager queue task $PERIODIC_TASK_SEND_LOCATION_PING as ${pingWorkRequest.id} " +
            "with interval ${schedule.interval} after ${schedule.initialDelay}"
    )
    scheduledPingSettings = settings
    workManager.cancelAllWorkByTag(PERIODIC_TASK_SEND_LOCATION_PING)
    workManager.enqueue(pingWorkRequest)
  }

  /** The parts of the settings that the ping's schedule depends on */
  private fun LocatorSettings.pingKey() = ping to staticLocation

  /** Cancels all WorkManager tasks. Called on app exit */
  fun cancelAllTasks() {
    Timber.d("Cancelling task tag (all mqtt tasks) $ONETIME_TASK_MQTT_RECONNECT")
    workManager.cancelUniqueWork(ONETIME_TASK_MQTT_RECONNECT)
    workManager.cancelUniqueWork(PERIODIC_TASK_MQTT_CONNECTION_WATCHDOG)
    workManager.cancelAllWorkByTag(PERIODIC_TASK_SEND_LOCATION_PING)
    resetMqttReconnectBackoff()
  }

  /**
   * Starts the periodic check that the MQTT connection is still alive, reconnecting it if not.
   *
   * Every other trigger for a reconnect is reactive — a connectivity callback, a dropped
   * connection, a failed publish — so anything they collectively fail to notice is never noticed at
   * all. The most important such case is a connection that is dead but still believed to be up,
   * which produces no event of any kind.
   *
   * Enqueued with [ExistingPeriodicWorkPolicy.KEEP] so that repeatedly re-activating the endpoint
   * cannot keep pushing the next run into the future and starve the check entirely.
   *
   * The first run is delayed by a full interval. WorkManager otherwise runs a newly-enqueued
   * periodic job straight away, and the endpoint activation that enqueues it is by definition still
   * CONNECTING (or not yet configured), which the watchdog treats as unhealthy: it would tear down
   * the very connection attempt that scheduled it.
   */
  fun scheduleMqttConnectionWatchdog() {
    PeriodicWorkRequest.Builder(
            MQTTConnectionWatchdogWorker::class.java,
            CONNECTION_WATCHDOG_INTERVAL.inWholeMinutes,
            TimeUnit.MINUTES,
        )
        .addTag(PERIODIC_TASK_MQTT_CONNECTION_WATCHDOG)
        .setInitialDelay(CONNECTION_WATCHDOG_INTERVAL.inWholeMinutes, TimeUnit.MINUTES)
        .setConstraints(anyNetworkConstraint)
        .build()
        .run {
          workManager.enqueueUniquePeriodicWork(
              PERIODIC_TASK_MQTT_CONNECTION_WATCHDOG,
              ExistingPeriodicWorkPolicy.KEEP,
              this,
          )
        }
    Timber.i(
        "Scheduled $PERIODIC_TASK_MQTT_CONNECTION_WATCHDOG every $CONNECTION_WATCHDOG_INTERVAL"
    )
  }

  /** How many consecutive reconnect attempts have been scheduled without an intervening success. */
  private val reconnectAttempt = AtomicInteger(0)

  /**
   * Schedules an attempt to reconnect to the MQTT broker.
   *
   * Successive attempts back off exponentially, but never further apart than [RECONNECT_MAX_DELAY],
   * so a broker that has been unreachable for a long time is still retried promptly once it comes
   * back.
   *
   * The backoff is computed here rather than handed to WorkManager via [BackoffPolicy]: WorkManager
   * clamps its own backoff to [WorkRequest.MAX_BACKOFF_MILLIS], which is five hours, and a run of
   * failures reaches that in well under a day. Combined with Doze deferral on a device that isn't
   * exempt from battery optimisation, retries at that spacing are effectively unbounded and the app
   * never recovers on its own.
   */
  fun scheduleMqttReconnect() {
    val delay = reconnectDelayForAttempt(reconnectAttempt.getAndIncrement())
    OneTimeWorkRequest.Builder(MQTTReconnectWorker::class.java)
        // Pause in case there's network turmoil
        .setInitialDelay(delay.inWholeMilliseconds, TimeUnit.MILLISECONDS)
        .addTag(ONETIME_TASK_MQTT_RECONNECT)
        .setConstraints(anyNetworkConstraint)
        .build()
        .run {
          workManager.enqueueUniqueWork(
              ONETIME_TASK_MQTT_RECONNECT,
              ExistingWorkPolicy.REPLACE,
              this,
          )
        }
    // Logged at INFO: when this goes wrong the connection is dead for hours, and at DEBUG the
    // evidence has long since rolled out of the in-memory log buffer by the time anyone looks.
    Timber.i("Scheduled ONETIME_TASK_MQTT_RECONNECT job in $delay")
  }

  /**
   * Puts the reconnect backoff back to its shortest delay. Called when a connection is established,
   * so that the next disconnection is retried promptly rather than at whatever spacing the previous
   * run of failures had reached.
   */
  fun resetMqttReconnectBackoff() {
    reconnectAttempt.getAndSet(0).run {
      if (this > 0) Timber.d("Reset MQTT reconnect backoff after $this attempts")
    }
  }

  data class PingSchedule(val interval: Duration, val initialDelay: Duration)

  companion object {
    private val PING_CHANGE_DEBOUNCE = 30.seconds

    /**
     * Periodic work runs once as soon as it's scheduled, which sends a ping straight away. Except
     * for a static location, which has just been sent on arriving at it, so the first ping waits.
     */
    internal fun pingScheduleFor(settings: LocatorSettings): PingSchedule {
      val interval = settings.ping.minutes
      return PingSchedule(
          interval,
          if (settings.staticLocation != null) interval else Duration.ZERO,
      )
    }

    private const val PERIODIC_TASK_SEND_LOCATION_PING = "PERIODIC_TASK_SEND_LOCATION_PING"
    private const val ONETIME_TASK_MQTT_RECONNECT = "ONETIME_TASK_MQTT_RECONNECT"
    private const val PERIODIC_TASK_MQTT_CONNECTION_WATCHDOG =
        "PERIODIC_TASK_MQTT_CONNECTION_WATCHDOG"

    /**
     * How often to verify the connection. WorkManager will not run periodic work more frequently
     * than [PeriodicWorkRequest.MIN_PERIODIC_INTERVAL_MILLIS], which is fifteen minutes, so asking
     * for less would achieve nothing.
     */
    internal val CONNECTION_WATCHDOG_INTERVAL = 15.minutes

    /** Delay before the first reconnect attempt of a run. */
    internal val RECONNECT_INITIAL_DELAY = 10.seconds

    /** Ceiling on the gap between reconnect attempts, however long the failure has persisted. */
    internal val RECONNECT_MAX_DELAY = 10.minutes

    /**
     * Exponential backoff from [RECONNECT_INITIAL_DELAY], capped at [RECONNECT_MAX_DELAY].
     *
     * @param attempt zero-based count of attempts already scheduled in this run of failures
     */
    internal fun reconnectDelayForAttempt(attempt: Int): Duration {
      // Clamp the shift as well as the result: Int.shl only uses the low five bits of its operand,
      // so a large attempt count would otherwise wrap around to a small — or negative — multiplier.
      val doublings = attempt.coerceIn(0, MAX_BACKOFF_DOUBLINGS)
      return (RECONNECT_INITIAL_DELAY * (1 shl doublings)).coerceAtMost(RECONNECT_MAX_DELAY)
    }

    /**
     * Enough doublings to comfortably exceed [RECONNECT_MAX_DELAY] without overflowing the shift.
     */
    private const val MAX_BACKOFF_DOUBLINGS = 16
  }
}
