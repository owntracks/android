package org.owntracks.android.services;

import android.os.Bundle;
import android.support.annotation.NonNull;

import com.google.android.gms.gcm.GcmNetworkManager;
import com.google.android.gms.gcm.GcmTaskService;
import com.google.android.gms.gcm.OneoffTask;
import com.google.android.gms.gcm.PeriodicTask;
import com.google.android.gms.gcm.Task;
import com.google.android.gms.gcm.TaskParams;

import org.owntracks.android.App;

import java.util.concurrent.TimeUnit;

import timber.log.Timber;

public class Scheduler extends GcmTaskService {
    public static final String BUNDLE_KEY_ACTION = "DISPATCHER_ACTION";
    public static final String BUNDLE_KEY_MESSAGE_ID = "MESSAGE_ID";

    public static final String TASK_SEND_MESSAGE_HTTP = "SEND_MESSAGE_HTTP";
    public static final String TASK_SEND_MESSAGE_MQTT = "SEND_MESSAGE_MQTT";
    private static final String TASK_SEND_MQTT_PING = "TASK_SEND_MQTT_PING" ;
    private static final String TASK_SEND_LOCATION_PING = "TASK_SEND_LOCATION_PING" ;

    @Override
    public int onRunTask(TaskParams taskParams) {
        Bundle extras = taskParams.getExtras();
        String action = extras.getString(BUNDLE_KEY_ACTION);

        if(action == null) {
            Timber.e("BUNDLE_KEY_ACTION is not set");
            return GcmNetworkManager.RESULT_FAILURE;
        }

        Timber.v("BUNDLE_KEY_ACTION: %s", extras.getString(BUNDLE_KEY_ACTION));

        switch (action) {
            case TASK_SEND_MESSAGE_HTTP:
                return MessageProcessorEndpointHttp.getInstance().sendMessage(extras) ? GcmNetworkManager.RESULT_SUCCESS : GcmNetworkManager.RESULT_FAILURE;
            case TASK_SEND_MESSAGE_MQTT:
                return MessageProcessorEndpointMqtt.getInstance().sendMessage(extras) ? GcmNetworkManager.RESULT_SUCCESS : GcmNetworkManager.RESULT_FAILURE;
            case TASK_SEND_MQTT_PING:
                return MessageProcessorEndpointMqtt.getInstance().sendPing() ? GcmNetworkManager.RESULT_SUCCESS : GcmNetworkManager.RESULT_FAILURE;
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
                .setTag(TASK_SEND_MQTT_PING)
                .setExtras(getBundleForAction(TASK_SEND_MQTT_PING))
                .setPeriod(keepAliveSeconds)
                .setUpdateCurrent(true)
                .setRequiredNetwork(Task.NETWORK_STATE_CONNECTED)
                .setFlex(keepAliveSeconds)
                .build();
        Timber.v("scheduling task TASK_SEND_MQTT_PING");
        GcmNetworkManager.getInstance(App.getContext()).schedule(task);
    }

    @NonNull
    public static Bundle getBundleForAction(String action) {
        Bundle b  = new Bundle();
        b.putString(BUNDLE_KEY_ACTION, action);
        return b;
    }

    public void scheduleLocationPing() {
        Bundle b = getBundleForAction(TASK_SEND_LOCATION_PING);
    }
}
