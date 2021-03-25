package org.owntracks.android.gms

import android.content.Context
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GoogleApiAvailability
import org.owntracks.android.injection.scopes.PerActivity
import org.owntracks.android.support.OSSRequirementsChecker
import org.owntracks.android.support.Preferences
import javax.inject.Inject

@PerActivity
class GMSRequirementsChecker @Inject constructor(private val preferences: Preferences, override val context: Context) : OSSRequirementsChecker(preferences, context) {
    override fun areRequirementsMet(): Boolean {
        return isPlayServicesCheckPassed() && isPermissionCheckPassed() && preferences.isSetupCompleted
    }

    override fun isPlayServicesCheckPassed(): Boolean = GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(context) == ConnectionResult.SUCCESS
}
