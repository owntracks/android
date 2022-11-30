package org.owntracks.android.services.worker;

import android.content.Context;

import androidx.work.BackoffPolicy;
import androidx.work.Constraints;
import androidx.work.NetworkType;
import androidx.work.OneTimeWorkRequest;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;
import androidx.work.WorkRequest;

import org.owntracks.android.preferences.Preferences;

import java.util.concurrent.TimeUnit;

import javax.inject.Inject;
import javax.inject.Singleton;

import dagger.hilt.android.qualifiers.ApplicationContext;
import timber.log.Timber;

@Singleton
public class Scheduler {
    public static final long MIN_PERIODIC_INTERVAL_MILLIS = PeriodicWorkRequest.MIN_PERIODIC_INTERVAL_MILLIS;
    private static final String ONEOFF_TASK_SEND_MESSAGE_HTTP = "SEND_MESSAGE_HTTP";
    private static final String ONEOFF_TASK_SEND_MESSAGE_MQTT = "SEND_MESSAGE_MQTT";
    private static final String PERIODIC_TASK_SEND_LOCATION_PING = "PERIODIC_TASK_SEND_LOCATION_PING";
    private static final String PERIODIC_TASK_MQTT_KEEPALIVE = "PERIODIC_TASK_MQTT_KEEPALIVE";
    private static final String ONETIME_TASK_MQTT_RECONNECT = "PERIODIC_TASK_MQTT_RECONNECT";
    private final Context context;
    private final Constraints anyNetworkConstraint = new Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build();
    @Inject
    Preferences preferences;

    @Inject
    public Scheduler(@ApplicationContext Context context) {
        this.context = context;
    }

    public void cancelAllTasks() {
        cancelMqttTasks();
        cancelHttpTasks();
        WorkManager.getInstance(this.context).cancelAllWorkByTag(PERIODIC_TASK_SEND_LOCATION_PING);
    }

    public void cancelHttpTasks() {
        Timber.d("canceling tasks");
        WorkManager.getInstance(this.context).cancelAllWorkByTag(ONEOFF_TASK_SEND_MESSAGE_HTTP);
    }

    public void cancelMqttTasks() {
        Timber.d("Cancelling task tag (all mqtt tasks) %s", ONEOFF_TASK_SEND_MESSAGE_MQTT);
        WorkManager.getInstance(this.context).cancelAllWorkByTag(ONEOFF_TASK_SEND_MESSAGE_MQTT);
        Timber.d("Cancelling task tag (all mqtt tasks) %s", PERIODIC_TASK_MQTT_KEEPALIVE);
        WorkManager.getInstance(this.context).cancelAllWorkByTag(PERIODIC_TASK_MQTT_KEEPALIVE);
        Timber.d("Cancelling task tag (all mqtt tasks) %s", ONETIME_TASK_MQTT_RECONNECT);
        WorkManager.getInstance(this.context).cancelAllWorkByTag(ONETIME_TASK_MQTT_RECONNECT);
    }

    public void scheduleMqttMaybeReconnectAndPing(long keepAliveSeconds) {
        if (keepAliveSeconds < TimeUnit.MILLISECONDS.toSeconds(MIN_PERIODIC_INTERVAL_MILLIS)) {
            Timber.i("MQTT Keepalive interval is smaller than most granular workmanager interval, setting to 900 seconds");
            keepAliveSeconds = TimeUnit.MILLISECONDS.toSeconds(MIN_PERIODIC_INTERVAL_MILLIS);
        }
        WorkRequest mqttPingWorkRequest = new PeriodicWorkRequest.Builder(MQTTMaybeReconnectAndPingWorker.class, keepAliveSeconds, TimeUnit.SECONDS)
                .addTag(PERIODIC_TASK_MQTT_KEEPALIVE)
                .setConstraints(anyNetworkConstraint)
                .setBackoffCriteria(BackoffPolicy.LINEAR, 30, TimeUnit.SECONDS)
                .build();
        Timber.d("WorkManager queue task %s as %s with interval %s", PERIODIC_TASK_MQTT_KEEPALIVE, mqttPingWorkRequest.getId(), keepAliveSeconds);
        WorkManager.getInstance(this.context).cancelAllWorkByTag(PERIODIC_TASK_MQTT_KEEPALIVE);
        WorkManager.getInstance(this.context).enqueue(mqttPingWorkRequest);
    }

    public void cancelMqttPing() {
        Timber.d("Cancelling task tag %s threadID: %s", PERIODIC_TASK_MQTT_KEEPALIVE, Thread.currentThread());
        WorkManager.getInstance(this.context).cancelAllWorkByTag(PERIODIC_TASK_MQTT_KEEPALIVE);
    }

    public void scheduleLocationPing() {
        WorkRequest pingWorkRequest =
                new PeriodicWorkRequest.Builder(SendLocationPingWorker.class, preferences.getPing(), TimeUnit.MINUTES)
                        .addTag(PERIODIC_TASK_SEND_LOCATION_PING)
                        .setConstraints(anyNetworkConstraint)
                        .build();
        Timber.d("WorkManager queue task %s as %s with interval %s minutes", PERIODIC_TASK_SEND_LOCATION_PING, pingWorkRequest.getId(), preferences.getPing());
        WorkManager.getInstance(this.context).cancelAllWorkByTag(PERIODIC_TASK_SEND_LOCATION_PING);
        WorkManager.getInstance(this.context).enqueue(pingWorkRequest);
    }

    public void scheduleMqttReconnect() {
        WorkRequest mqttReconnectWorkRequest =
                new OneTimeWorkRequest.Builder(MQTTReconnectWorker.class)
                        .addTag(ONETIME_TASK_MQTT_RECONNECT)
                        .setBackoffCriteria(BackoffPolicy.LINEAR, 5, TimeUnit.SECONDS)
                        .setConstraints(anyNetworkConstraint)
                        .build();

        Timber.d("WorkManager queue task %s as %s", ONETIME_TASK_MQTT_RECONNECT, mqttReconnectWorkRequest.getId());
        WorkManager.getInstance(this.context).cancelAllWorkByTag(ONETIME_TASK_MQTT_RECONNECT);
        WorkManager.getInstance(this.context).enqueue(mqttReconnectWorkRequest);
    }

    public void cancelMqttReconnect() {
        Timber.d("Cancelling task tag %s threadID: %s", ONETIME_TASK_MQTT_RECONNECT, Thread.currentThread());
        WorkManager.getInstance(this.context).cancelAllWorkByTag(ONETIME_TASK_MQTT_RECONNECT);
    }
}
