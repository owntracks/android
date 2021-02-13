package org.owntracks.android.support

import android.Manifest
import android.content.pm.PackageManager
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GoogleApiAvailability
import org.owntracks.android.injection.scopes.PerActivity
import javax.inject.Inject

@PerActivity
class RequirementsChecker @Inject constructor(private val preferences: Preferences, val context: AppCompatActivity) {
    fun areRequirementsMet(): Boolean {
        return isPlayCheckPassed && isPermissionCheckPassed && preferences.isSetupCompleted
    }

    val isPlayCheckPassed: Boolean
        get() = GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(context) == ConnectionResult.SUCCESS
    val isPermissionCheckPassed: Boolean
        get() = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED && ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
}