package org.owntracks.android.ui.mixins

import android.Manifest
import android.content.Context
import android.os.Build
import androidx.activity.result.ActivityResultCaller
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.NotificationManagerCompat
import timber.log.Timber

class NotificationPermissionRequester(
    private val caller: ActivityResultCaller,
    private val permissionGrantedCallback: () -> Unit,
    private val permissionDeniedCallback: () -> Unit
) {
  fun hasPermission(): Boolean =
      NotificationManagerCompat.from(caller as Context).areNotificationsEnabled()

  private val permissionRequest =
      caller.registerForActivityResult(ActivityResultContracts.RequestPermission()) {
        Timber.d("Notification notification callback, result=$it ")
        if (it) {
          permissionGrantedCallback()
        } else {
          permissionDeniedCallback()
        }
      }

  fun requestNotificationPermission() {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
      permissionRequest.launch(Manifest.permission.POST_NOTIFICATIONS)
    }
  }
}
