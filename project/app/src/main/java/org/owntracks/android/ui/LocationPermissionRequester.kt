package org.owntracks.android.ui

import android.Manifest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat.shouldShowRequestPermissionRationale
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import org.owntracks.android.R

abstract class AppCompatActivityThatMightRequestLocationPermissions :
    AppCompatActivity(),
    LocationPermissionRequester.PermissionResultCallback

class LocationPermissionRequester(
    private val activity: AppCompatActivityThatMightRequestLocationPermissions
) {
    interface PermissionResultCallback {
        fun locationPermissionGranted()
        fun locationPermissionDenied()
    }

    private val permissionRequest =
        activity.registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
            when {
                permissions[Manifest.permission.ACCESS_COARSE_LOCATION] ?: false ||
                    permissions[Manifest.permission.ACCESS_FINE_LOCATION] ?: false -> {
                    activity.locationPermissionGranted()
                }
                else -> {
                    activity.locationPermissionDenied()
                }
            }
        }

    fun requestLocationPermissions() {
        val permissions = arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)
        if (shouldShowRequestPermissionRationale(activity, Manifest.permission.ACCESS_FINE_LOCATION)) {
            // The user may have denied us once already, so show a rationale

            val locationPermissionsRationaleAlertDialog = MaterialAlertDialogBuilder(activity).setCancelable(true)
                .setIcon(R.drawable.ic_baseline_location_disabled_24)
                .setTitle(
                    activity.getString(R.string.locationPermissionRequestDialogTitle)
                )
                .setMessage(R.string.locationPermissionRequestDialogMessage)
                .setPositiveButton(
                    android.R.string.ok
                ) { _, _ ->
                    permissionRequest.launch(permissions)
                }
                .setNegativeButton(android.R.string.cancel) { _, _ ->
                    activity.locationPermissionDenied()
                }
                .create()
            if (!locationPermissionsRationaleAlertDialog.isShowing) {
                locationPermissionsRationaleAlertDialog.show()
            }
        } else {
            // No need to show rationale, just request
            permissionRequest.launch(permissions)
        }
    }
}
