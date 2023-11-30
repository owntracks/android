package org.owntracks.android.services.worker

import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequest
import androidx.work.Operation
import androidx.work.PeriodicWorkRequest
import androidx.work.WorkManager
import androidx.work.WorkRequest
import androidx.work.WorkRequest.Companion.MIN_BACKOFF_MILLIS
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.time.Duration.Companion.milliseconds
import org.owntracks.android.preferences.Preferences
import timber.log.Timber

@Singleton
class Scheduler @Inject constructor(
    private val preferences: Preferences,
    @param:ApplicationContext private val context: Context
) : Preferences.OnPreferenceChangeListener {
    init {
        preferences.registerOnPreferenceChangedListener(this)
    }

    private val anyNetworkConstraint = Constraints.Builder()
        .setRequiredNetworkType(NetworkType.CONNECTED)
        .build()
    private val workManager = WorkManager.getInstance(context)

    private var mqttReconnectJob: Operation? = null

    /**
     * Used by the background service to peridically ping a location
     */
    fun scheduleLocationPing() {
        val pingWorkRequest: WorkRequest = PeriodicWorkRequest.Builder(
            SendLocationPingWorker::class.java,
            preferences.ping.toLong(),
            TimeUnit.MINUTES
        )
            .addTag(PERIODIC_TASK_SEND_LOCATION_PING)
            .setConstraints(anyNetworkConstraint)
            .build()
        Timber.d(
            "WorkManager queue task $PERIODIC_TASK_SEND_LOCATION_PING as ${pingWorkRequest.id} " +
                "with interval ${preferences.ping} minutes"
        )
        workManager.cancelAllWorkByTag(PERIODIC_TASK_SEND_LOCATION_PING)
        workManager.enqueue(pingWorkRequest)
    }

    /**
     * Cancels all WorkManager tasks. Called on app exit
     */
    fun cancelAllTasks() {
        Timber.d("Cancelling task tag (all mqtt tasks) $ONETIME_TASK_MQTT_RECONNECT")
        workManager.cancelUniqueWork(ONETIME_TASK_MQTT_RECONNECT)
        workManager.cancelAllWorkByTag(PERIODIC_TASK_SEND_LOCATION_PING)
    }

    fun scheduleMqttReconnect() {
        workManager.getWorkInfosForUniqueWork(ONETIME_TASK_MQTT_RECONNECT).run {
            if (isDone) {
                Timber.d("Scheduling ONETIME_TASK_MQTT_RECONNECT job")
                mqttReconnectJob = workManager.enqueueUniqueWork(
                    ONETIME_TASK_MQTT_RECONNECT,
                    ExistingWorkPolicy.REPLACE,
                    OneTimeWorkRequest.Builder(MQTTReconnectWorker::class.java)
                        .addTag(ONETIME_TASK_MQTT_RECONNECT)
                        .setBackoffCriteria(BackoffPolicy.LINEAR, MIN_BACKOFF_MILLIS, TimeUnit.MILLISECONDS)
                        .setConstraints(anyNetworkConstraint)
                        .build()
                )
            } else {
                Timber.d(
                    "Not attempting to schedule reconnect, as existing reconnect job is in progress."
                )
            }
        }
    }

    companion object {
        val MIN_PERIODIC_INTERVAL = PeriodicWorkRequest.MIN_PERIODIC_INTERVAL_MILLIS.milliseconds
        private const val PERIODIC_TASK_SEND_LOCATION_PING = "PERIODIC_TASK_SEND_LOCATION_PING"
        private const val ONETIME_TASK_MQTT_RECONNECT = "ONETIME_TASK_MQTT_RECONNECT"
    }

    override fun onPreferenceChanged(properties: Set<String>) {
        if (properties.contains(Preferences::ping.name)) {
            workManager.cancelAllWorkByTag(PERIODIC_TASK_SEND_LOCATION_PING)
            scheduleLocationPing()
        }
    }
}
