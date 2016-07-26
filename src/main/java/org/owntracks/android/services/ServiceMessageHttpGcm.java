package org.owntracks.android.services;

import android.content.ComponentName;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Base64;
import android.util.Log;

import com.google.android.gms.gcm.GcmNetworkManager;
import com.google.android.gms.gcm.GcmTaskService;
import com.google.android.gms.gcm.TaskParams;
import com.squareup.okhttp.MediaType;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.RequestBody;
import com.squareup.okhttp.Response;

import org.antlr.v4.runtime.misc.NotNull;
import org.owntracks.android.messages.MessageBase;
import org.owntracks.android.support.interfaces.StatelessMessageEndpoint;
import org.owntracks.android.support.receiver.Parser;

import java.io.IOException;


public class ServiceMessageHttpGcm extends GcmTaskService {
    private static final String TAG = "ServiceMessageHttpGcm";
    public static final String TAG_POST_MESSAGE = "TAG_POST_MESSAGE";
    public static final String BUNDLE_KEY_REQUEST_BODY = "TAG_POST_MESSAGE_KEY_REQUEST_BODY";
    public static final String BUNDLE_KEY_URL = "TAG_POST_MESSAGE_KEY_URL";
    public static final String BUNDLE_KEY_USERINFO = "TAG_POST_MESSAGE_KEY_USERINFO";
    public static final String BUNDLE_KEY_MESSAGE_ID = "TAG_POST_MESSAGE_KEY_MESSAGE_ID";
    private ServiceProxy service;

    ServiceConnection mServiceConnection;

    @Override
    public void onCreate() {
        super.onCreate();
        mServiceConnection = new ServiceConnection() {
            @Override
            public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
                Log.d(TAG, "onServiceConnected");
                service = (ServiceProxy)((ServiceBindable.ServiceBinder)iBinder).getService();
            }

            @Override
            public void onServiceDisconnected(ComponentName componentName) {
                Log.d(TAG, "onServiceDisconnected");
                service = null;
            }
        };
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        ServiceProxy.closeServiceConnection();
    }


    @Override
    public void onInitializeTasks() {
        // When your package is removed or updated, all of its network tasks are cleared by
        // the GcmNetworkManager. You can override this method to reschedule them in the case of
        // an updated package. This is not called when your application is first installed.
        //
        // This is called on your application's main thread.

        // TODO(developer): In a real app, this should be implemented to re-schedule important tasks.
    }


    @Override
    public int onRunTask(TaskParams taskParams) {
        Log.d(TAG, "onRunTask(): " + taskParams.getTag());

        postMessage(taskParams.getExtras());
        return GcmNetworkManager.RESULT_SUCCESS;
    }

    private void postMessage(Bundle extras) {
        Log.v(TAG, "postMessage()");
         ServiceMessageHttp.postMessage(extras.getString(BUNDLE_KEY_REQUEST_BODY), extras.getString(BUNDLE_KEY_URL), extras.getString(BUNDLE_KEY_USERINFO), this, extras.getLong(BUNDLE_KEY_MESSAGE_ID));
    }



}
