package org.owntracks.android.services;

import android.app.Service;
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

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;

import timber.log.Timber;

public class ServiceMessageDispatcher extends GcmTaskService {
    public static final String BUNDLE_KEY_MESSAGE_MODE = "SEND_MESSAGE_KEY_MODE";

    public static final String BUNDLE_KEY_MESSAGE_PAYLOAD = "SEND_MESSAGE_KEY_PAYLOAD";
    public static final String BUNDLE_KEY_MESSAGE_TARGET = "SEND_MESSAGE_KEY_TARGET";
    public static final String BUNDLE_KEY_MESSAGE_ID = "SEND_MESSAGE_KEY_ID";

    public static final String TASK_SEND_MESSAGE_HTTP = "SEND_MESSAGE_HTTP";
    public static final String TASK_SEND_MESSAGE_MQTT = "SEND_MESSAGE_MQTT";

    @Override
    public int onRunTask(TaskParams taskParams) {
        Bundle extras = taskParams.getExtras();
        switch (taskParams.getTag()) {
            case TASK_SEND_MESSAGE_HTTP:
                return sendMessageHttp(extras);
            case TASK_SEND_MESSAGE_MQTT:
                return sendMessageMqtt(extras);
            default:
                return GcmNetworkManager.RESULT_FAILURE;
        }

    }

    private int sendMessageMqtt(Bundle extras) {
        return ServiceMessageMqttGCM.
    }

    private int sendMessageHttp(Bundle extras) {
        //TODO: Refactor keys
        return ServiceMessageHttp.postMessage(extras.getString(ServiceMessageHttpGcm.BUNDLE_KEY_REQUEST_BODY), extras.getString(ServiceMessageHttpGcm.BUNDLE_KEY_URL), extras.getString(ServiceMessageHttpGcm.BUNDLE_KEY_USERINFO), this, extras.getLong(ServiceMessageHttpGcm.BUNDLE_KEY_MESSAGE_ID));
    }


    public void scheduleMessage(MessageBase message)  {
        Bundle b = BundleFactory.getBundleCreator().getBundle(message);
        if(b == null) {
            Timber.e("no bundle returned for message %s", message.getMessageId());
            return;
        }

        Task task = new OneoffTask.Builder()
                .setService(ServiceMessageDispatcher.class)
                .setExecutionWindow(0, 30)
                .setTag(b.getString(BUNDLE_KEY_MESSAGE_MODE))
                .setUpdateCurrent(false)
                .setRequiredNetwork(Task.NETWORK_STATE_CONNECTED)
                .setRequiresCharging(false)
                .setExtras(b)
                .build();

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
            Bundle b = new Bundle();
            b.putString(BUNDLE_KEY_MESSAGE_MODE, TASK_SEND_MESSAGE_MQTT);
            return b;
        }
    }
}
