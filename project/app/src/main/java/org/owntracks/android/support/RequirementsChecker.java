package org.owntracks.android.support;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import androidx.core.content.ContextCompat;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;

public class RequirementsChecker {
    private final Preferences preferences;
    private final Context context;

    public RequirementsChecker(Preferences preferences, Context context) {
        this.preferences = preferences;
        this.context = context;
    }

    public boolean areRequirementsMet  () {
        return isPlayCheckPassed() && isPermissionCheckPassed() && isInitialSetupCheckPassed();
    }

    public boolean isPlayCheckPassed() {
        return GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(context) == ConnectionResult.SUCCESS;
    }

    public boolean isPermissionCheckPassed() {
        return ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED;
    }

    public boolean isInitialSetupCheckPassed() {
        return preferences.getSetupCompleted();
    }

}
