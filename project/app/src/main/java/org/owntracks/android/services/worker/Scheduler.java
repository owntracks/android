package org.owntracks.android.services.worker;

import android.arch.lifecycle.Observer;
import android.support.annotation.Nullable;

import org.owntracks.android.App;
import org.owntracks.android.injection.components.DaggerServiceComponent;
import org.owntracks.android.services.MessageProcessor;
import org.owntracks.android.support.Preferences;

import java.util.List;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;

import androidx.work.BackoffPolicy;
import androidx.work.Constraints;
import androidx.work.NetworkType;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;
import androidx.work.WorkRequest;
import androidx.work.WorkStatus;
import timber.log.Timber;

public class Scheduler {
    public static final long MIN_PERIODIC_INTERVAL_MILLIS = PeriodicWorkRequest.MIN_PERIODIC_INTERVAL_MILLIS;
    private static final String ONEOFF_TASK_SEND_MESSAGE_HTTP = "SEND_MESSAGE_HTTP";
    private static final String ONEOFF_TASK_SEND_MESSAGE_MQTT = "SEND_MESSAGE_MQTT";
    private static final String PERIODIC_TASK_SEND_LOCATION_PING = "PERIODIC_TASK_SEND_LOCATION_PING" ;
    private static final String PERIODIC_TASK_MQTT_KEEPALIVE = "PERIODIC_TASK_MQTT_KEEPALIVE" ;
    private static final String PERIODIC_TASK_MQTT_RECONNECT = "PERIODIC_TASK_MQTT_RECONNECT";

    private WorkManager workManager = WorkManager.getInstance();

    @Inject protected Preferences preferences;
    @Inject protected MessageProcessor messageProcessor;
    private Constraints anyNetworkConstraint = new Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build();

    public Scheduler() {
        DaggerServiceComponent.builder().appComponent(App.getAppComponent()).build().inject(this);
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

    public void scheduleMqttPing(long keepAliveSeconds) {

        WorkRequest mqttPingWorkRequest = new PeriodicWorkRequest.Builder(MQTTKeepaliveWorker.class, keepAliveSeconds, TimeUnit.SECONDS)
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
                new PeriodicWorkRequest.Builder(MQTTReconnectWorker.class, MIN_PERIODIC_INTERVAL_MILLIS, TimeUnit.MILLISECONDS)
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
}
