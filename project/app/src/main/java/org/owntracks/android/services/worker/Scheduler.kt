package org.owntracks.android.services.worker

import android.content.Context
import android.os.Build
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequest
import androidx.work.PeriodicWorkRequest
import androidx.work.WorkManager
import androidx.work.WorkRequest
import androidx.work.WorkRequest.Companion.MIN_BACKOFF_MILLIS
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.Duration
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton
import org.owntracks.android.preferences.Preferences
import timber.log.Timber

@Singleton
class Scheduler
@Inject
constructor(
    private val preferences: Preferences,
    @param:ApplicationContext private val context: Context
) : Preferences.OnPreferenceChangeListener {
  init {
    preferences.registerOnPreferenceChangedListener(this)
  }

  private val anyNetworkConstraint =
      Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build()
  private val workManager = WorkManager.getInstance(context)

  /** Used by the background service to periodically ping a location */
  fun scheduleLocationPing() {
    val pingWorkRequest: WorkRequest =
        PeriodicWorkRequest.Builder(
                SendLocationPingWorker::class.java, preferences.ping.toLong(), TimeUnit.MINUTES)
            .addTag(PERIODIC_TASK_SEND_LOCATION_PING)
            .setConstraints(anyNetworkConstraint)
            .build()
    Timber.d(
        "WorkManager queue task $PERIODIC_TASK_SEND_LOCATION_PING as ${pingWorkRequest.id} " +
            "with interval ${preferences.ping} minutes")
    workManager.cancelAllWorkByTag(PERIODIC_TASK_SEND_LOCATION_PING)
    workManager.enqueue(pingWorkRequest)
  }

  /** Cancels all WorkManager tasks. Called on app exit */
  fun cancelAllTasks() {
    Timber.d("Cancelling task tag (all mqtt tasks) $ONETIME_TASK_MQTT_RECONNECT")
    workManager.cancelUniqueWork(ONETIME_TASK_MQTT_RECONNECT)
    workManager.cancelAllWorkByTag(PERIODIC_TASK_SEND_LOCATION_PING)
  }

  fun scheduleMqttReconnect() =
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            OneTimeWorkRequest.Builder(MQTTReconnectWorker::class.java)
                // Pause in case there's network turmoil
                .setInitialDelay(Duration.ofSeconds(RECONNECT_DELAY_SECONDS))
                .addTag(ONETIME_TASK_MQTT_RECONNECT)
                .setBackoffCriteria(BackoffPolicy.LINEAR, MIN_BACKOFF_MILLIS, TimeUnit.MILLISECONDS)
                .setConstraints(anyNetworkConstraint)
                .build()
          } else {
            OneTimeWorkRequest.Builder(MQTTReconnectWorker::class.java)
                .addTag(ONETIME_TASK_MQTT_RECONNECT)
                .setBackoffCriteria(BackoffPolicy.LINEAR, MIN_BACKOFF_MILLIS, TimeUnit.MILLISECONDS)
                .setConstraints(anyNetworkConstraint)
                .build()
          }
          .run {
            workManager.enqueueUniqueWork(
                ONETIME_TASK_MQTT_RECONNECT, ExistingWorkPolicy.KEEP, this)
            Timber.d("Scheduled ONETIME_TASK_MQTT_RECONNECT job")
          }

  companion object {
    private const val PERIODIC_TASK_SEND_LOCATION_PING = "PERIODIC_TASK_SEND_LOCATION_PING"
    private const val ONETIME_TASK_MQTT_RECONNECT = "ONETIME_TASK_MQTT_RECONNECT"
    private const val RECONNECT_DELAY_SECONDS = 10L
  }

  override fun onPreferenceChanged(properties: Set<String>) {
    if (properties.contains(Preferences::ping.name)) {
      workManager.cancelAllWorkByTag(PERIODIC_TASK_SEND_LOCATION_PING)
      scheduleLocationPing()
    }
  }
}
