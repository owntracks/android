package org.owntracks.android.services.worker;

import androidx.work.BackoffPolicy;
import androidx.work.Constraints;
import androidx.work.NetworkType;
import androidx.work.OneTimeWorkRequest;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;
import androidx.work.WorkRequest;

import org.owntracks.android.injection.scopes.PerApplication;
import org.owntracks.android.support.Preferences;

import java.util.concurrent.TimeUnit;

import javax.inject.Inject;

import timber.log.Timber;

@PerApplication
public class Scheduler {
    public static final long MIN_PERIODIC_INTERVAL_MILLIS = PeriodicWorkRequest.MIN_PERIODIC_INTERVAL_MILLIS;
    private static final String ONEOFF_TASK_SEND_MESSAGE_HTTP = "SEND_MESSAGE_HTTP";
    private static final String ONEOFF_TASK_SEND_MESSAGE_MQTT = "SEND_MESSAGE_MQTT";
    private static final String PERIODIC_TASK_SEND_LOCATION_PING = "PERIODIC_TASK_SEND_LOCATION_PING" ;
    private static final String PERIODIC_TASK_MQTT_KEEPALIVE = "PERIODIC_TASK_MQTT_KEEPALIVE" ;
    private static final String PERIODIC_TASK_MQTT_RECONNECT = "PERIODIC_TASK_MQTT_RECONNECT";
    private static final String PERIODIC_TASK_PROCESS_OUTGOING_MESSAGES = "PERIODIC_TASK_PROCESS_OUTGOING_MESSAGES";

    private WorkManager workManager = WorkManager.getInstance();

    @Inject
    Preferences preferences;

    private Constraints anyNetworkConstraint = new Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build();

    @Inject
    public Scheduler() {
    }

    public void cancelHttpTasks() {
        Timber.v("canceling tasks");
        workManager.cancelAllWorkByTag(ONEOFF_TASK_SEND_MESSAGE_HTTP);
    }

    public void cancelMqttTasks() {
       Timber.v("Cancelling task tag (all mqtt tasks) %s", ONEOFF_TASK_SEND_MESSAGE_MQTT);
        workManager.cancelAllWorkByTag(ONEOFF_TASK_SEND_MESSAGE_MQTT);
        Timber.v("Cancelling task tag (all mqtt tasks) %s", PERIODIC_TASK_MQTT_KEEPALIVE);
        workManager.cancelAllWorkByTag(PERIODIC_TASK_MQTT_KEEPALIVE);
        Timber.v("Cancelling task tag (all mqtt tasks) %s", PERIODIC_TASK_MQTT_RECONNECT);
        workManager.cancelAllWorkByTag(PERIODIC_TASK_MQTT_RECONNECT);
    }

    public void scheduleMqttMaybeReconnectAndPing(long keepAliveSeconds) {
        WorkRequest mqttPingWorkRequest = new PeriodicWorkRequest.Builder(MQTTMaybeReconnectAndPingWorker.class, keepAliveSeconds, TimeUnit.SECONDS)
                .addTag(PERIODIC_TASK_MQTT_KEEPALIVE)
                .setConstraints(anyNetworkConstraint)
                .setBackoffCriteria(BackoffPolicy.LINEAR, 30, TimeUnit.SECONDS)
                .build();
        Timber.v("WorkManager queue task %s as %s with interval %s", PERIODIC_TASK_MQTT_KEEPALIVE,mqttPingWorkRequest.getId(), keepAliveSeconds);
        workManager.cancelAllWorkByTag(PERIODIC_TASK_MQTT_KEEPALIVE);
        workManager.enqueue(mqttPingWorkRequest);
    }

    public void cancelMqttPing() {
        Timber.v("Cancelling task tag %s", PERIODIC_TASK_MQTT_KEEPALIVE);
        workManager.cancelAllWorkByTag(PERIODIC_TASK_MQTT_KEEPALIVE);
    }

    public void scheduleLocationPing() {
        WorkRequest pingWorkRequest =
                new PeriodicWorkRequest.Builder(SendLocationPingWorker.class, preferences.getPing(), TimeUnit.MINUTES)
                        .addTag(PERIODIC_TASK_SEND_LOCATION_PING)
                        .setConstraints(anyNetworkConstraint)
                        .build();
        Timber.v("WorkManager queue task %s as %s with interval %s", PERIODIC_TASK_SEND_LOCATION_PING,pingWorkRequest.getId(), preferences.getPing());
        workManager.cancelAllWorkByTag(PERIODIC_TASK_SEND_LOCATION_PING);
        workManager.enqueue(pingWorkRequest);
    }

    public void scheduleMqttReconnect() {
        WorkRequest mqttReconnectWorkRequest =
                new OneTimeWorkRequest.Builder(MQTTReconnectWorker.class)
                        .addTag(PERIODIC_TASK_MQTT_RECONNECT)
                        .setBackoffCriteria(BackoffPolicy.LINEAR, 5, TimeUnit.SECONDS)
                        .setConstraints(anyNetworkConstraint)
                        .build();

        Timber.v("WorkManager queue task %s as %s", PERIODIC_TASK_MQTT_RECONNECT, mqttReconnectWorkRequest.getId());
        workManager.cancelAllWorkByTag(PERIODIC_TASK_MQTT_RECONNECT);
        workManager.enqueue(mqttReconnectWorkRequest);
    }

    public void cancelMqttReconnect() {
        Timber.v("Cancelling task tag %s", PERIODIC_TASK_MQTT_RECONNECT);
        workManager.cancelAllWorkByTag(PERIODIC_TASK_MQTT_RECONNECT);
    }
}
