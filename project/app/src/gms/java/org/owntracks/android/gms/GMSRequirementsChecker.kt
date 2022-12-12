package org.owntracks.android.gms

import android.content.Context
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GoogleApiAvailability
import dagger.hilt.android.scopes.ActivityScoped
import javax.inject.Inject
import org.owntracks.android.preferences.Preferences
import org.owntracks.android.support.OSSRequirementsChecker

@ActivityScoped
class GMSRequirementsChecker @Inject constructor(
    private val preferences: Preferences,
    override val context: Context
) : OSSRequirementsChecker(preferences, context) {
    override fun areRequirementsMet(): Boolean {
        return isPlayServicesCheckPassed() && isLocationPermissionCheckPassed() && preferences.setupCompleted
    }

    override fun isPlayServicesCheckPassed(): Boolean = GoogleApiAvailability.getInstance()
        .isGooglePlayServicesAvailable(context) == ConnectionResult.SUCCESS
}
