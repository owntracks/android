package org.owntracks.android.gms

import android.content.Context
import androidx.core.app.NotificationManagerCompat
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GoogleApiAvailability
import dagger.hilt.android.scopes.ActivityScoped
import javax.inject.Inject
import org.owntracks.android.support.OSSRequirementsChecker
import org.owntracks.android.support.Preferences

@ActivityScoped
class GMSRequirementsChecker @Inject constructor(
    private val preferences: Preferences,
    override val context: Context
) : OSSRequirementsChecker(preferences, context) {
    override fun areRequirementsMet(): Boolean {
        return isPlayServicesCheckPassed() && isLocationPermissionCheckPassed() && preferences.isSetupCompleted
    }

    override fun isPlayServicesCheckPassed(): Boolean = GoogleApiAvailability.getInstance()
        .isGooglePlayServicesAvailable(context) == ConnectionResult.SUCCESS

    override fun isNotificationsEnabled(): Boolean = NotificationManagerCompat.from(context)
        .areNotificationsEnabled()
}
