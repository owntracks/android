package org.owntracks.android.support;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.support.v4.content.ContextCompat;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;

import timber.log.Timber;

public class RequirementsChecker {
    private final Preferences preferences;
    private boolean playCheckPassed;
    private boolean permissionCheckPassed;
    private boolean initialSetupCheckPassed;

    public RequirementsChecker(Preferences preferences) {
        this.preferences = preferences;
    }

    public boolean areRequirementsMet(Context c) {
        this.playCheckPassed = GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(c) == ConnectionResult.SUCCESS;
        this.permissionCheckPassed = ContextCompat.checkSelfPermission(c, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED;
        this.initialSetupCheckPassed = preferences.getSetupCompleted();

        Timber.v("playCheckPassed:%s, permissionCheckPassed:%s, initialSetupCheckPassed%s", playCheckPassed, permissionCheckPassed, initialSetupCheckPassed);
        return isPlayCheckPassed() && isPermissionCheckPassed() && isInitialSetupCheckPassed();
    }

    public boolean isPlayCheckPassed() {
        return this.playCheckPassed;
    }

    public boolean isPermissionCheckPassed() {
        return this.permissionCheckPassed;
    }

    public boolean isInitialSetupCheckPassed() {
        return this.initialSetupCheckPassed;
    }

}
