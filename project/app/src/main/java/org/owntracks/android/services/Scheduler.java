package org.owntracks.android.services;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;

import com.google.android.gms.gcm.GcmNetworkManager;
import com.google.android.gms.gcm.GcmTaskService;
import com.google.android.gms.gcm.OneoffTask;
import com.google.android.gms.gcm.PeriodicTask;
import com.google.android.gms.gcm.Task;
import com.google.android.gms.gcm.TaskParams;

import org.owntracks.android.App;
import org.owntracks.android.support.Preferences;

import java.util.concurrent.TimeUnit;

import timber.log.Timber;

public class Scheduler extends GcmTaskService {
    public static final String BUNDLE_KEY_ACTION = "DISPATCHER_ACTION";
    public static final String BUNDLE_KEY_MESSAGE_ID = "MESSAGE_ID";

    public static final String ONEOFF_TASK_SEND_MESSAGE_HTTP = "SEND_MESSAGE_HTTP";
    public static final String ONEOFF_TASK_SEND_MESSAGE_MQTT = "SEND_MESSAGE_MQTT";
    private static final String PERIODIC_TASK_SEND_LOCATION_PING = "PERIODIC_TASK_SEND_LOCATION_PING" ;
    private static final String PERIODIC_TASK_MQTT_PING = "PERIODIC_TASK_MQTT_PING" ;
    private static final String PERIODIC_TASK_MQTT_RECONNECT = "PERIODIC_TASK_MQTT_RECONNECT";

    @Override
    public int onRunTask(TaskParams taskParams) {
        Bundle extras = taskParams.getExtras();
        if(extras == null) {
            Timber.e("Bundle extras are not set");
            return GcmNetworkManager.RESULT_FAILURE;
        }

        String action = extras.getString(BUNDLE_KEY_ACTION);
        if(action == null) {
            Timber.e("BUNDLE_KEY_ACTION is not set");
            return GcmNetworkManager.RESULT_FAILURE;
        }

        Timber.v("BUNDLE_KEY_ACTION: %s", extras.getString(BUNDLE_KEY_ACTION));

        switch (action) {
            case ONEOFF_TASK_SEND_MESSAGE_HTTP:
                return MessageProcessorEndpointHttp.getInstance().sendMessage(extras) ? GcmNetworkManager.RESULT_SUCCESS : GcmNetworkManager.RESULT_RESCHEDULE;
            case ONEOFF_TASK_SEND_MESSAGE_MQTT:
                return MessageProcessorEndpointMqtt.getInstance().sendMessage(extras) ? GcmNetworkManager.RESULT_SUCCESS : GcmNetworkManager.RESULT_RESCHEDULE;
            case PERIODIC_TASK_MQTT_PING:
                return MessageProcessorEndpointMqtt.getInstance().sendPing() ? GcmNetworkManager.RESULT_SUCCESS : GcmNetworkManager.RESULT_FAILURE;
            case PERIODIC_TASK_MQTT_RECONNECT:
                return MessageProcessorEndpointMqtt.getInstance().checkConnection() ? GcmNetworkManager.RESULT_SUCCESS : GcmNetworkManager.RESULT_FAILURE;
            case PERIODIC_TASK_SEND_LOCATION_PING:
                Intent mIntent = new Intent(this, BackgroundService.class);
                mIntent.setAction(BackgroundService.INTENT_ACTION_SEND_LOCATION_PING);
                startService(mIntent);

            default:
                return GcmNetworkManager.RESULT_FAILURE;
        }

    }

    public void scheduleMessage(Bundle b)  {
        if(b.get(BUNDLE_KEY_MESSAGE_ID) == null) {
            Timber.e("Bundle without BUNDLE_KEY_MESSAGE_ID");
            return;
        }
        if(b.get(BUNDLE_KEY_ACTION) == null) {
            Timber.e("Bundle without BUNDLE_KEY_ACTION");
            return;
        }

        Task task = new OneoffTask.Builder()
                .setService(Scheduler.class)
                .setExecutionWindow(0L, App.isInForeground() ? 1L: 60L)
                .setTag(Long.toString(b.getLong(BUNDLE_KEY_MESSAGE_ID)))
                .setUpdateCurrent(false)
                .setRequiredNetwork(Task.NETWORK_STATE_CONNECTED)
                .setRequiresCharging(false)
                .setExtras(b)
                .build();

        Timber.v("scheduling task %s, %s", b.get(BUNDLE_KEY_ACTION), task.getTag());

        GcmNetworkManager.getInstance(App.getContext()).schedule(task);
    }

    public void scheduleMqttPing(long keepAliveSeconds) {
        PeriodicTask task = new PeriodicTask.Builder()
                .setService(Scheduler.class)
                .setTag(PERIODIC_TASK_MQTT_PING)
                .setExtras(getBundleForAction(PERIODIC_TASK_MQTT_PING))
                .setPeriod(keepAliveSeconds)
                .setUpdateCurrent(true)
                .setRequiredNetwork(Task.NETWORK_STATE_CONNECTED)
                .build();
        Timber.v("scheduling task PERIODIC_TASK_MQTT_PING");
        GcmNetworkManager.getInstance(App.getContext()).schedule(task);
    }

    public void cancelMqttPing() {
        Timber.v("canceling task PERIODIC_TASK_MQTT_PING");
        GcmNetworkManager.getInstance(App.getContext()).cancelTask(PERIODIC_TASK_MQTT_PING, Scheduler.class);
    }

    public void scheduleLocationPing() {
        PeriodicTask task = new PeriodicTask.Builder()
                .setService(Scheduler.class)
                .setTag(PERIODIC_TASK_SEND_LOCATION_PING)
                .setExtras(getBundleForAction(PERIODIC_TASK_SEND_LOCATION_PING))
                .setPeriod(TimeUnit.MINUTES.toSeconds(Preferences.getPing()))
                .setUpdateCurrent(true)
                .setRequiredNetwork(Task.NETWORK_STATE_CONNECTED)
                .setFlex(TimeUnit.MINUTES.toSeconds(Preferences.getPing())/2)
                .build();
        Timber.v("scheduling task PERIODIC_TASK_SEND_LOCATION_PING");
        GcmNetworkManager.getInstance(App.getContext()).schedule(task);
    }

    @NonNull
    public static Bundle getBundleForAction(String action) {
        Bundle b  = new Bundle();
        b.putString(BUNDLE_KEY_ACTION, action);
        return b;
    }

    public void scheduleMqttReconnect() {
        PeriodicTask task = new PeriodicTask.Builder()
                .setService(Scheduler.class)
                .setTag(PERIODIC_TASK_MQTT_RECONNECT)
                .setExtras(getBundleForAction(PERIODIC_TASK_MQTT_RECONNECT))
                .setPeriod(TimeUnit.MINUTES.toSeconds(10))
                .setUpdateCurrent(true)
                .setRequiredNetwork(Task.NETWORK_STATE_CONNECTED)
                .setFlex(TimeUnit.MINUTES.toSeconds(10))
                .build();
        Timber.v("scheduling task PERIODIC_TASK_MQTT_RECONNECT");
        GcmNetworkManager.getInstance(App.getContext()).schedule(task);
    }



    public void cancelMqttReconnect() {
        Timber.v("canceling task PERIODIC_TASK_MQTT_RECONNECT");
        GcmNetworkManager.getInstance(App.getContext()).cancelTask(PERIODIC_TASK_MQTT_RECONNECT, Scheduler.class);

    }
}
