package org.owntracks.android.services.worker

import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequest
import androidx.work.PeriodicWorkRequest
import androidx.work.WorkManager
import androidx.work.WorkRequest
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
        cancelMqttTasks()
        workManager.cancelAllWorkByTag(PERIODIC_TASK_SEND_LOCATION_PING)
    }

    /**
     * Called when the MQTT endpoint deactivates
     */
    fun cancelMqttTasks() {
        workManager.apply {
            Timber.d("Cancelling task tag (all mqtt tasks) $ONEOFF_TASK_SEND_MESSAGE_MQTT")
            cancelAllWorkByTag(ONEOFF_TASK_SEND_MESSAGE_MQTT)
            Timber.d("Cancelling task tag (all mqtt tasks) $PERIODIC_TASK_MQTT_KEEPALIVE")
            cancelAllWorkByTag(PERIODIC_TASK_MQTT_KEEPALIVE)
            Timber.d("Cancelling task tag (all mqtt tasks) $ONETIME_TASK_MQTT_RECONNECT")
            cancelUniqueWork(ONETIME_TASK_MQTT_RECONNECT)
        }
    }

    fun scheduleMqttReconnect() {
        workManager.enqueueUniqueWork(
            ONETIME_TASK_MQTT_RECONNECT,
            ExistingWorkPolicy.REPLACE,
            OneTimeWorkRequest.Builder(MQTTReconnectWorker::class.java)
                .addTag(ONETIME_TASK_MQTT_RECONNECT)
                .setBackoffCriteria(BackoffPolicy.LINEAR, 5, TimeUnit.SECONDS)
                .setConstraints(anyNetworkConstraint)
                .build()
        )
            .apply { Timber.d("WorkManager queue task $ONETIME_TASK_MQTT_RECONNECT") }
    }

    companion object {
        val MIN_PERIODIC_INTERVAL = PeriodicWorkRequest.MIN_PERIODIC_INTERVAL_MILLIS.milliseconds
        private const val ONEOFF_TASK_SEND_MESSAGE_MQTT = "SEND_MESSAGE_MQTT"
        private const val PERIODIC_TASK_SEND_LOCATION_PING = "PERIODIC_TASK_SEND_LOCATION_PING"
        private const val PERIODIC_TASK_MQTT_KEEPALIVE = "PERIODIC_TASK_MQTT_KEEPALIVE"
        private const val ONETIME_TASK_MQTT_RECONNECT = "PERIODIC_TASK_MQTT_RECONNECT"
    }

    override fun onPreferenceChanged(properties: Set<String>) {
        if (properties.contains(Preferences::ping.name)) {
            workManager.cancelAllWorkByTag(PERIODIC_TASK_SEND_LOCATION_PING)
            scheduleLocationPing()
        }
    }
}
