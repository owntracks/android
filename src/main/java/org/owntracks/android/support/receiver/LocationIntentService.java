package org.owntracks.android.support.receiver;

import android.app.IntentService;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;

/**
 * Service that receives Location updates. It receives
 * updates in the background, even if the main Activity is not visible.
 */
public class LocationIntentService extends IntentService {
    private static final String TAG = "LocationIntentService";
    public LocationIntentService() {
        super("LocationIntentService");
        Log.v(TAG, "started");

    }

    @Override
    public void onCreate() {
        super.onCreate();
        Log.v(TAG, "onCreate");
    }
    @Override
    public void onDestroy() {
        super.onCreate();
        Log.v(TAG, "onDestroy");
    }

    /**
     * Creates an IntentService.  Invoked by your subclass's constructor.
     *
     * @param name Used to name the worker thread, important only for debugging.
     */
    public LocationIntentService(String name) {
        super(name);
    }

    /**
     * Called when a new location update is available.
     */
    @Override
    protected void onHandleIntent(Intent intent) {
        Log.v(TAG, "onHandleIntent 1: " + intent.getAction());
        Log.v(TAG, "onHandleIntent 2: " + LocationResult.hasResult(intent));

        ;
        if(LocationResult.hasResult(intent))
         Log.v(TAG, "onHandleIntent 3: " + LocationResult.extractResult(intent).getLastLocation());

    }
}