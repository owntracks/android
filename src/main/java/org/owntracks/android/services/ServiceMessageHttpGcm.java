package org.owntracks.android.services;

import android.content.ComponentName;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;

import com.google.android.gms.gcm.GcmNetworkManager;
import com.google.android.gms.gcm.GcmTaskService;
import com.google.android.gms.gcm.TaskParams;

import timber.log.Timber;


public class ServiceMessageHttpGcm extends GcmTaskService {
    public static final String BUNDLE_KEY_REQUEST_BODY = "TAG_POST_MESSAGE_KEY_REQUEST_BODY";
    public static final String BUNDLE_KEY_URL = "TAG_POST_MESSAGE_KEY_URL";
    public static final String BUNDLE_KEY_USERINFO = "TAG_POST_MESSAGE_KEY_USERINFO";
    public static final String BUNDLE_KEY_MESSAGE_ID = "TAG_POST_MESSAGE_KEY_MESSAGE_ID";

    @Override
    public void onCreate() {
        super.onCreate();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        ServiceProxy.closeServiceConnection();
    }

    @Override
    public int onRunTask(TaskParams taskParams) {
        Timber.v("tag:%s",taskParams.getTag());
        postMessage(taskParams.getExtras());
        return GcmNetworkManager.RESULT_SUCCESS;
    }

    private void postMessage(Bundle extras) {
         ServiceMessageHttp.postMessage(extras.getString(BUNDLE_KEY_REQUEST_BODY), extras.getString(BUNDLE_KEY_URL), extras.getString(BUNDLE_KEY_USERINFO), this, extras.getLong(BUNDLE_KEY_MESSAGE_ID));
    }
}
