package org.owntracks.android.support

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import org.owntracks.android.injection.scopes.PerActivity
import javax.inject.Inject

@PerActivity
open class OSSRequirementsChecker @Inject constructor(private val preferences: Preferences, open val context: Context) : RequirementsChecker {
    override fun areRequirementsMet(): Boolean {
        return isPermissionCheckPassed() && preferences.isSetupCompleted
    }

    override fun isPermissionCheckPassed(): Boolean = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED && ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED

    override fun isPlayServicesCheckPassed(): Boolean = true
}