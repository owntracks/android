package org.owntracks.android.activities;

import android.Manifest;
import android.app.Activity;
import android.app.Application;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;

import org.owntracks.android.App;
import org.owntracks.android.support.Events;
import org.owntracks.android.support.SnackbarFactory;

import de.greenrobot.event.EventBus;

public class ActivityBase extends AppCompatActivity implements SnackbarFactory.SnackbarFactoryDelegate {
    private static final String TAG = "ActivityBase";

    @Override
    public View getSnackbarTargetView() {
        return getWindow().getDecorView().findViewById(android.R.id.content);
    }

    protected boolean hasIntentExtras() {
        return getIntent() != null && getIntent().getExtras() != null;
    }

    private static final int[] PERMISSION_GRANTED = new int[]{PackageManager.PERMISSION_GRANTED};
    private static final String[] PERMISSIONS_PLACEHOLDER = new String[]{""};

    protected void runActionWithLocationPermissionCheck(int action) {
        runActionWithPermissionCheck(action, Manifest.permission.ACCESS_FINE_LOCATION);
    }

    protected void runActionWithPermissionCheck(int action, String permission) {
        Log.v(TAG, "runActionWithPermissionCheck() " + permission);
        if (ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED) {
            onRequestPermissionsResult(action, PERMISSIONS_PLACEHOLDER, PERMISSION_GRANTED);
        } else {
            ActivityCompat.requestPermissions(this, new String[]{permission}, action);
        }
    }




    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        if(grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            EventBus.getDefault().post(new Events.PermissionGranted(permissions[0])); // Notify about changed permissions

            onRunActionWithPermissionCheck(requestCode, true);
        } else {
            onRunActionWithPermissionCheck(requestCode, false);

        }
    }

    protected  void onRunActionWithPermissionCheck(int action, boolean granted) {

    }
}
