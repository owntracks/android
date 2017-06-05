package org.owntracks.android.services;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.annotation.Nullable;

import com.google.android.gms.gcm.GcmNetworkManager;
import com.google.android.gms.gcm.GcmTaskService;
import com.google.android.gms.gcm.OneoffTask;
import com.google.android.gms.gcm.Task;
import com.google.android.gms.gcm.TaskParams;

import org.owntracks.android.App;
import org.owntracks.android.messages.MessageBase;
import org.owntracks.android.support.Parser;
import org.owntracks.android.support.Preferences;

import java.net.MalformedURLException;
import java.net.URL;

import timber.log.Timber;

public class Dispatcher extends GcmTaskService {
    public static final String BUNDLE_KEY_ACTION = "DISPATCHER_ACTION";
    public static final String BUNDLE_KEY_MESSAGE_ID = "MESSAGE_ID";

    public static final String TASK_SEND_MESSAGE_HTTP = "SEND_MESSAGE_HTTP";
    public static final String TASK_SEND_MESSAGE_MQTT = "SEND_MESSAGE_MQTT";

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
                return ServiceMessageHttp.getInstance().sendMessage(extras) ? GcmNetworkManager.RESULT_SUCCESS : GcmNetworkManager.RESULT_FAILURE;
            case TASK_SEND_MESSAGE_MQTT:
                return ServiceEndpointMqtt.getInstance().sendMessage(extras) ? GcmNetworkManager.RESULT_SUCCESS : GcmNetworkManager.RESULT_FAILURE;
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
                .setService(Dispatcher.class)
                .setExecutionWindow(0L, App.isInForeground() ? 1L: 60L)
                .setTag(Long.toString(b.getLong(BUNDLE_KEY_MESSAGE_ID)))
                .setUpdateCurrent(false)
                .setRequiredNetwork(Task.NETWORK_STATE_CONNECTED)
                .setRequiresCharging(false)
                .setExtras(b)
                .build();

        Timber.v("scheduling task %s /", task.getTag());

        GcmNetworkManager.getInstance(App.getContext()).schedule(task);
    }
}
