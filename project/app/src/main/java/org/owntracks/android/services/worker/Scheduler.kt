package org.owntracks.android.services.worker

import android.content.Context
import androidx.work.*
import dagger.hilt.android.qualifiers.ApplicationContext
import org.owntracks.android.support.Preferences
import timber.log.Timber
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class Scheduler @Inject constructor(
        @param:ApplicationContext private val context: Context,
        private val preferences: Preferences
) {
    private val workManager = WorkManager.getInstance(context)
    private val anyNetworkConstraint = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

    fun cancelAllTasks() {
        cancelMqttTasks()
        cancelHttpTasks()
        workManager.cancelAllWorkByTag(PERIODIC_TASK_SEND_LOCATION_PING)
    }

    fun cancelHttpTasks() {
        Timber.d("canceling HTTP tasks")
        workManager.cancelAllWorkByTag(ONEOFF_TASK_SEND_MESSAGE_HTTP)
    }

    fun cancelMqttTasks() {
        Timber.d("Cancelling task tag (all mqtt tasks) $ONEOFF_TASK_SEND_MESSAGE_MQTT")
        workManager.cancelAllWorkByTag(ONEOFF_TASK_SEND_MESSAGE_MQTT)
        Timber.d("Cancelling task tag (all mqtt tasks) $PERIODIC_TASK_MQTT_KEEPALIVE")
        workManager.cancelAllWorkByTag(PERIODIC_TASK_MQTT_KEEPALIVE)
        Timber.d("Cancelling task tag (all mqtt tasks) ONETIME_TASK_MQTT_RECONNECT")
        workManager.cancelAllWorkByTag(ONETIME_TASK_MQTT_RECONNECT)
    }

    fun scheduleMqttMaybeReconnectAndPing(keepAliveSeconds: Long) {
        if (keepAliveSeconds < TimeUnit.MILLISECONDS.toSeconds(MIN_PERIODIC_INTERVAL_MILLIS)) {
            Timber.i("MQTT Keepalive interval ($keepAliveSeconds) is smaller than most granular workmanager interval, setting to 900 seconds")
        }
        val interval = keepAliveSeconds.coerceAtLeast(
                TimeUnit.MILLISECONDS.toSeconds(MIN_PERIODIC_INTERVAL_MILLIS)
        )
        val mqttPingWorkRequest: WorkRequest = PeriodicWorkRequest.Builder(
                MQTTMaybeReconnectAndPingWorker::class.java,
                interval,
                TimeUnit.SECONDS
        )
                .addTag(PERIODIC_TASK_MQTT_KEEPALIVE)
                .setConstraints(anyNetworkConstraint)
                .setBackoffCriteria(BackoffPolicy.LINEAR, 30, TimeUnit.SECONDS)
                .build()
        Timber.d("WorkManager queue task $PERIODIC_TASK_MQTT_KEEPALIVE as ${mqttPingWorkRequest.id} with interval $interval")
        workManager.cancelAllWorkByTag(PERIODIC_TASK_MQTT_KEEPALIVE)
        workManager.enqueue(mqttPingWorkRequest)
    }

    fun cancelMqttPing() {
        Timber.d("Cancelling task tag $PERIODIC_TASK_MQTT_KEEPALIVE threadID: ${Thread.currentThread()}")
        workManager.cancelAllWorkByTag(PERIODIC_TASK_MQTT_KEEPALIVE)
    }

    fun scheduleLocationPing() {
        val pingWorkRequest: WorkRequest = PeriodicWorkRequest.Builder(
                SendLocationPingWorker::class.java,
                preferences.ping.toLong(),
                TimeUnit.MINUTES
        )
                .addTag(PERIODIC_TASK_SEND_LOCATION_PING)
                .setConstraints(anyNetworkConstraint)
                .build()
        Timber.d("WorkManager queue task $PERIODIC_TASK_SEND_LOCATION_PING as ${pingWorkRequest.id} with interval ${preferences.ping} minutes")
        workManager.cancelAllWorkByTag(PERIODIC_TASK_SEND_LOCATION_PING)
        workManager.enqueue(pingWorkRequest)
    }

    fun scheduleMqttReconnect() {
        val mqttReconnectWorkRequest: WorkRequest =
                OneTimeWorkRequest.Builder(MQTTReconnectWorker::class.java)
                        .addTag(ONETIME_TASK_MQTT_RECONNECT)
                        .setBackoffCriteria(BackoffPolicy.LINEAR, 5, TimeUnit.SECONDS)
                        .setConstraints(anyNetworkConstraint)
                        .build()
        Timber.d("WorkManager queue task $ONETIME_TASK_MQTT_RECONNECT as ${mqttReconnectWorkRequest.id}")
        workManager.cancelAllWorkByTag(ONETIME_TASK_MQTT_RECONNECT)
        workManager.enqueue(mqttReconnectWorkRequest)
    }

    fun cancelMqttReconnect() {
        Timber.d("Cancelling task tag $ONETIME_TASK_MQTT_RECONNECT threadID: ${Thread.currentThread()}")
        workManager.cancelAllWorkByTag(ONETIME_TASK_MQTT_RECONNECT)
    }

    companion object {
        const val MIN_PERIODIC_INTERVAL_MILLIS = PeriodicWorkRequest.MIN_PERIODIC_INTERVAL_MILLIS
        private const val ONEOFF_TASK_SEND_MESSAGE_HTTP = "SEND_MESSAGE_HTTP"
        private const val ONEOFF_TASK_SEND_MESSAGE_MQTT = "SEND_MESSAGE_MQTT"
        private const val PERIODIC_TASK_SEND_LOCATION_PING = "PERIODIC_TASK_SEND_LOCATION_PING"
        private const val PERIODIC_TASK_MQTT_KEEPALIVE = "PERIODIC_TASK_MQTT_KEEPALIVE"
        private const val ONETIME_TASK_MQTT_RECONNECT = "PERIODIC_TASK_MQTT_RECONNECT"
    }
}