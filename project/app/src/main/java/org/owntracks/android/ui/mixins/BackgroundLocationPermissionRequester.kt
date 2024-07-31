package org.owntracks.android.ui.mixins

import android.Manifest.permission.ACCESS_BACKGROUND_LOCATION
import android.content.Context
import android.os.Build
import androidx.activity.result.ActivityResultCaller
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import org.owntracks.android.R
import timber.log.Timber

class BackgroundLocationPermissionRequester(
    caller: ActivityResultCaller,
    private val permissionGrantedCallback: () -> Unit,
    private val permissionDeniedCallback: () -> Unit
) {

  private val permissionRequest =
      caller.registerForActivityResult(ActivityResultContracts.RequestPermission()) { result ->
        Timber.d("Background permission callback, result=$result ")
        if (result) {
          permissionGrantedCallback()
        } else {
          permissionDeniedCallback()
        }
      }

  /** Request background location permissions, optionally showing a request dialog first */
  @RequiresApi(Build.VERSION_CODES.Q)
  fun requestLocationPermissions(
      context: Context,
      showPermissionRationale: (permissions: String) -> Boolean
  ) {
    Timber.d("Requesting Background Location Permissions")
    if (showPermissionRationale(ACCESS_BACKGROUND_LOCATION)) {
      // The user may have denied us once already, so show a rationale
      Timber.d("Showing Background Location permission rationale")
      MaterialAlertDialogBuilder(context)
          .setCancelable(false)
          .setIcon(R.drawable.baseline_share_location_24)
          .setTitle(R.string.backgroundLocationPermissionRequestDialogTitle)
          .setMessage(R.string.backgroundLocationPermissionRequestDialogText)
          .setPositiveButton(android.R.string.ok) { _, _ ->
            permissionRequest.launch(ACCESS_BACKGROUND_LOCATION)
          }
          .setNegativeButton(android.R.string.cancel) { _, _ -> permissionDeniedCallback() }
          .show()
    } else {
      permissionRequest.launch(ACCESS_BACKGROUND_LOCATION)
    }
  }
}
