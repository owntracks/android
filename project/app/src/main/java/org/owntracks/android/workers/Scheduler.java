
package org.owntracks.android.workers;

import org.owntracks.android.App;

import java.util.concurrent.TimeUnit;

import androidx.work.BackoffPolicy;
import androidx.work.Constraints;
import androidx.work.NetworkType;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;
import androidx.work.WorkRequest;
import timber.log.Timber;

public class Scheduler {
    public static final String BUNDLE_KEY_ACTION = "DISPATCHER_ACTION";
    public static final String BUNDLE_KEY_MESSAGE_ID = "MESSAGE_ID";

    public static final String ONEOFF_TASK_SEND_MESSAGE_HTTP = "SEND_MESSAGE_HTTP";
    public static final String ONEOFF_TASK_SEND_MESSAGE_MQTT = "SEND_MESSAGE_MQTT";
    private static final String PERIODIC_TASK_SEND_LOCATION_PING = "PERIODIC_TASK_SEND_LOCATION_PING";
    private static final String PERIODIC_TASK_MQTT_KEEPALIVE = "PERIODIC_TASK_MQTT_KEEPALIVE";
    private static final String PERIODIC_TASK_MQTT_RECONNECT = "PERIODIC_TASK_MQTT_RECONNECT";
    private static final String PERIODIC_TASK_PROCESS_QUEUE = "PERIODIC_TASK_PROCESS_QUEUE";
    private WorkManager workManager = WorkManager.getInstance();
    private Constraints anyNetworkConstraint = new Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build();

    public Scheduler() {
    }

    public void cancelHttpTasks() {
        Timber.v("Cancelling task tag %s", ONEOFF_TASK_SEND_MESSAGE_HTTP);
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

    public void scheduleMqttPing(long keepAliveSeconds) {
        WorkRequest mqttPingWorkRequest = new PeriodicWorkRequest.Builder(MQTTKeepaliveWorker.class, keepAliveSeconds, TimeUnit.SECONDS)
                .addTag(PERIODIC_TASK_MQTT_KEEPALIVE)
                .setConstraints(anyNetworkConstraint)
                .setBackoffCriteria(BackoffPolicy.LINEAR, 30, TimeUnit.SECONDS)
                .build();
        Timber.v("WorkManager queue task %s as %s", PERIODIC_TASK_MQTT_KEEPALIVE,mqttPingWorkRequest.getId());
        workManager.cancelAllWorkByTag(PERIODIC_TASK_MQTT_KEEPALIVE);
        workManager.enqueue(mqttPingWorkRequest);
    }

    public void cancelMqttPing() {
        Timber.v("Cancelling task tag %s", PERIODIC_TASK_MQTT_KEEPALIVE);
        workManager.cancelAllWorkByTag(PERIODIC_TASK_MQTT_KEEPALIVE);
    }


    public void scheduleLocationPing() {
        WorkRequest pingWorkRequest =
                new PeriodicWorkRequest.Builder(SendLocationPingWorker.class, App.getPreferences().getPing(), TimeUnit.SECONDS)
                        .addTag(PERIODIC_TASK_SEND_LOCATION_PING)
                        .setBackoffCriteria(BackoffPolicy.LINEAR, 30, TimeUnit.SECONDS)
                        .setConstraints(anyNetworkConstraint)
                        .build();
        Timber.v("WorkManager queue task %s as %s", PERIODIC_TASK_SEND_LOCATION_PING,pingWorkRequest.getId());
        workManager.cancelAllWorkByTag(PERIODIC_TASK_SEND_LOCATION_PING);
        workManager.enqueue(pingWorkRequest);
    }

    public void scheduleMqttReconnect() {
        WorkRequest mqttReconnectWorkRequest =
                new PeriodicWorkRequest.Builder(MQTTReconnectWorker.class, 10, TimeUnit.MINUTES)
                        .addTag(PERIODIC_TASK_MQTT_RECONNECT)
                        .setBackoffCriteria(BackoffPolicy.LINEAR, 30, TimeUnit.SECONDS)
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

    private static int RESULT_SUCCESS = 0;
    private static int RESULT_FAIL_RETRY = 1;
    private static int RESULT_FAIL_NORETRY = 2;

    public static int returnSuccess() {
        return RESULT_SUCCESS;
    }

    public static int returnFailRetry() {
        Timber.v("RESULT_FAIL_RETRY");
        return RESULT_FAIL_RETRY;
    }

    public static int returnFailNoretry() {
        Timber.v("RESULT_FAIL_NORETRY");
        return RESULT_FAIL_NORETRY;
    }

}