package org.owntracks.android.ui.mixins

import android.Manifest.permission.ACCESS_COARSE_LOCATION
import android.Manifest.permission.ACCESS_FINE_LOCATION
import android.content.Context
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityOptionsCompat
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import org.owntracks.android.R

class LocationPermissionRequester(private val caller: ActivityResultCallerWithLocationPermissionCallback) {
    interface PermissionResultCallback {
        fun locationPermissionGranted(code: Int)
        fun locationPermissionDenied(code: Int)
    }

    private val permissionRequest =
        caller.registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
            when {
                permissions[ACCESS_COARSE_LOCATION] ?: false ||
                    permissions[ACCESS_FINE_LOCATION] ?: false -> {
                    caller.locationPermissionGranted(this.code)
                }
                else -> {
                    caller.locationPermissionDenied(this.code)
                }
            }
        }

    private var code: Int = 0

    /**
     * Request location permissions, optionally showing a request dialog first
     *
     * @param code that's passed back to the callback
     */
    fun requestLocationPermissions(
        code: Int = 0,
        context: Context,
        showPermissionRationale: (permissions: String) -> Boolean
    ) {
        this.code = code
        val permissions = arrayOf(ACCESS_FINE_LOCATION, ACCESS_COARSE_LOCATION)
        if (showPermissionRationale(ACCESS_FINE_LOCATION)) {
            // The user may have denied us once already, so show a rationale

            val locationPermissionsRationaleAlertDialog = MaterialAlertDialogBuilder(context).setCancelable(true)
                .setIcon(R.drawable.ic_baseline_location_disabled_24)
                .setTitle(R.string.locationPermissionRequestDialogTitle)
                .setMessage(R.string.locationPermissionRequestDialogMessage)
                .setPositiveButton(android.R.string.ok) { _, _ ->
                    permissionRequest.launch(permissions, ActivityOptionsCompat.makeBasic())
                }
                .setNegativeButton(android.R.string.cancel) { _, _ ->
                    caller.locationPermissionDenied(code)
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
