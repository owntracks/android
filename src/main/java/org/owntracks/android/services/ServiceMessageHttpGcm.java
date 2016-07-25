package org.owntracks.android.services;

import android.os.Bundle;
import android.util.Log;

import com.google.android.gms.gcm.GcmNetworkManager;
import com.google.android.gms.gcm.GcmTaskService;
import com.google.android.gms.gcm.TaskParams;
import com.squareup.okhttp.MediaType;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.RequestBody;
import com.squareup.okhttp.Response;

import org.owntracks.android.support.interfaces.StatelessMessageEndpoint;

import java.io.IOException;


public class ServiceMessageHttpGcm extends GcmTaskService {
    private static final String TAG = "ServiceMessageHttpGcm";
    public static final String TAG_POST_MESSAGE = "TAG_POST_MESSAGE";
    public static final String BUNDLE_KEY_REQUEST_BODY = "TAG_POST_MESSAGE_KEY_REQUEST_BODY";
    public static final String BUNDLE_KEY_URL = "TAG_POST_MESSAGE_KEY_URL";
    public static final String BUNDLE_KEY_USERNAME = "TAG_POST_MESSAGE_KEY_USERNAME";
    public static final String BUNDLE_KEY_PASSWORD = "TAG_POST_MESSAGE_KEY_PASSWORD";

    private OkHttpClient mHttpClient;
    public static final MediaType JSON  = MediaType.parse("application/json; charset=utf-8");

    @Override
    public void onCreate() {
        super.onCreate();
        mHttpClient = new OkHttpClient();

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
        Log.d(TAG, "onRunTask: " + taskParams.getTag());

        String tag = taskParams.getTag();


        // Default result is success.
        int result = GcmNetworkManager.RESULT_SUCCESS;

        //if(TAG_POST_MESSAGE.equals(tag)) {
            postMessage(taskParams.getExtras());
        //}
        return result;

    }

    private void postMessage(Bundle extras) {
        Log.v(TAG, "postMessage()");

        Request request = new Request.Builder()
                .url(extras.getString(BUNDLE_KEY_URL))
                .method("POST", RequestBody.create(JSON, extras.getByteArray(BUNDLE_KEY_REQUEST_BODY)))

                .build();

        Response response = null;
        try {
            response = mHttpClient.newCall(request).execute();
            Log.v(TAG, "postMessage() - return: " + response.body().string());

        } catch (IOException e) {
            e.printStackTrace();
        }

    }

}
