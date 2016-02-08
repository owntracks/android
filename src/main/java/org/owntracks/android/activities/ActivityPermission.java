package org.owntracks.android.activities;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.util.Log;


public class ActivityPermission extends ActivityBase{
    private static final int RESULT_ACCESS_COARSE_LOCATION = 1;
    private static final String TAG = "ActivityPermission";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Log.v(TAG, "requesting Manifest.permission.ACCESS_COARSE_LOCATION");
        ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_COARSE_LOCATION}, RESULT_ACCESS_COARSE_LOCATION);

    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        Log.v(TAG, "onRequestPermissionsResult");
        switch (requestCode) {
            case RESULT_ACCESS_COARSE_LOCATION: {
                Log.v(TAG, "request code: RESULT_ACCESS_COARSE_LOCATION");
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Log.v(TAG, "RESULT_ACCESS_COARSE_LOCATION permission granted");
                    // permission was granted, yay! Do the
                    // contacts-related task you need to do.

                } else {
                    Log.v(TAG, "RESULT_ACCESS_COARSE_LOCATION permission denied");

                    // permission denied, boo! Disable the
                    // functionality that depends on this permission.
                }
                return;
            }

        }
    }

}
