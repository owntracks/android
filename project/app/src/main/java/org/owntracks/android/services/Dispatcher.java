package org.owntracks.android.services;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.annotation.Nullable;

import com.google.android.gms.gcm.GcmNetworkManager;
import com.google.android.gms.gcm.GcmTaskService;
import com.google.android.gms.gcm.OneoffTask;
import com.google.android.gms.gcm.Task;
import com.google.android.gms.gcm.TaskParams;

import org.owntracks.android.messages.MessageBase;
import org.owntracks.android.support.Parser;
import org.owntracks.android.support.Preferences;

import java.net.MalformedURLException;
import java.net.URL;

import timber.log.Timber;

public class Dispatcher extends GcmTaskService {
    static Dispatcher instance;
    public static final String BUNDLE_KEY_ACTION = "DISPATCHER_ACTION";

    public static final String BUNDLE_KEY_MESSAGE_MODE = "SEND_MESSAGE_KEY_MODE";

    public static final String TASK_SEND_MESSAGE_HTTP = "SEND_MESSAGE_HTTP";
    public static final String TASK_SEND_MESSAGE_MQTT = "SEND_MESSAGE_MQTT";

    public static Dispatcher getInstance() {
        if(instance == null)
            instance = new Dispatcher();
        return instance;
    }

    @Override
    public int onRunTask(TaskParams taskParams) {
        Bundle extras = taskParams.getExtras();
        switch (extras.getString(BUNDLE_KEY_ACTION)) {
            case TASK_SEND_MESSAGE_HTTP:
                return sendMessageHttp(extras);
            case TASK_SEND_MESSAGE_MQTT:
                return sendMessageMqtt(extras);
            default:
                return GcmNetworkManager.RESULT_FAILURE;
        }

    }

    private int sendMessageMqtt(Bundle extras) {
        return ServiceEndpointMqtt.getInstance().sendMessage(extras) ? GcmNetworkManager.RESULT_SUCCESS : GcmNetworkManager.RESULT_FAILURE;
    }

    private int sendMessageHttp(Bundle extras) {
        //TODO: Refactor keys
        return ServiceMessageHttp.postMessage(extras.getString(ServiceMessageHttpGcm.BUNDLE_KEY_REQUEST_BODY), extras.getString(ServiceMessageHttpGcm.BUNDLE_KEY_URL), extras.getString(ServiceMessageHttpGcm.BUNDLE_KEY_USERINFO), this, extras.getLong(ServiceMessageHttpGcm.BUNDLE_KEY_MESSAGE_ID));
    }


    public void scheduleMessage(Bundle b)  {

        Task task = new OneoffTask.Builder()
                .setService(Dispatcher.class)
                .setExecutionWindow(0, 30)
                .setTag(Long.toString("todo")
                .setUpdateCurrent(false)
                .setRequiredNetwork(Task.NETWORK_STATE_CONNECTED)
                .setRequiresCharging(false)
                .setExtras(b)
                .build();

        Timber.v("scheduling task %s", task.getTag());
        GcmNetworkManager.getInstance(this).schedule(task);
    }

    private static class BundleFactory implements org.owntracks.android.support.Preferences.OnPreferenceChangedListener {

        private BundleCreator creator;
        private static BundleFactory instance;

        static BundleFactory getInstance() {
            if(instance == null) {
                instance = new BundleFactory();
                Preferences.registerOnPreferenceChangedListener(instance);
            }
            return instance;
        }

        static BundleCreator getBundleCreator() {
            if(getInstance().creator == null) {
                if(Preferences.isModeHttpPrivate())
                    getInstance().creator = new HttpBundleCreator();
                else
                    getInstance().creator = new MqttBundleCreator();
            }
            return getInstance().creator;
        }

        @Override
        public void onAttachAfterModeChanged() {
            creator = null;
        }

        @Override
        public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String s) {

        }
    }


    private interface BundleCreator {
        @Nullable Bundle getBundle(MessageBase message);
    }

    public static void onEndpointStateChanged(ServiceMessage.EndpointState state) {

    }

    private static class HttpBundleCreator implements BundleCreator {

        private String endpointUserInfo;
        private String endpointUrl;

        HttpBundleCreator() {
            loadEndpointUrl();
        }

        private void loadEndpointUrl() {
            URL endpoint;
            try {
                endpoint = new URL(Preferences.getUrl());
                onEndpointStateChanged(ServiceMessage.EndpointState.IDLE);
            } catch (MalformedURLException e) {
                e.printStackTrace();
                onEndpointStateChanged(ServiceMessage.EndpointState.ERROR_CONFIGURATION);
                return;
            }

            this.endpointUserInfo = endpoint.getUserInfo();

            if (this.endpointUserInfo != null && this.endpointUserInfo.length() > 0) {
                this.endpointUrl = endpoint.toString().replace(endpointUserInfo+"@", "");
            } else {
                this.endpointUrl = endpoint.toString();
            }
            Timber.v("endpointUrl:%s, endpointUserInfo:%s", this.endpointUrl, this.endpointUserInfo );
        }

        @Override
        @Nullable
        public Bundle getBundle(MessageBase message) {
            Bundle b = new Bundle();
            try {
                b.putString(BUNDLE_KEY_MESSAGE_MODE, TASK_SEND_MESSAGE_HTTP);
                b.putString(ServiceMessageHttpGcm.BUNDLE_KEY_USERINFO, this.endpointUserInfo);
                b.putString(ServiceMessageHttpGcm.BUNDLE_KEY_URL, this.endpointUrl);
                b.putLong(ServiceMessageHttpGcm.BUNDLE_KEY_MESSAGE_ID, message.getMessageId());
                b.putString(ServiceMessageHttpGcm.BUNDLE_KEY_REQUEST_BODY, Parser.toJson(message));
            } catch (Exception e) {
                onEndpointStateChanged(ServiceMessage.EndpointState.ERROR);
                e.printStackTrace();
                b = null;
            }

            return b;
        }
    }
    private static class MqttBundleCreator implements BundleCreator {

        @Override
        @Nullable
        public Bundle getBundle(MessageBase message) {
            Bundle b = message.toBundle();
            b.putString(BUNDLE_KEY_MESSAGE_MODE, TASK_SEND_MESSAGE_MQTT);

            return b;
        }
    }
}
