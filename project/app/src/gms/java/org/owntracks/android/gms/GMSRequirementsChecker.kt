package org.owntracks.android.gms

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GoogleApiAvailability
import dagger.hilt.android.scopes.ActivityScoped
import javax.inject.Inject
import org.owntracks.android.support.OSSRequirementsChecker

@ActivityScoped
class GMSRequirementsChecker @Inject constructor(override val context: Context) :
    OSSRequirementsChecker(context) {
  override fun isPlayServicesCheckPassed(): Boolean =
      GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(context) ==
          ConnectionResult.SUCCESS

  override fun hasNotificationPermissions(): Boolean =
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED
      } else {
        true
      }
}
