package org.owntracks.android.ui.map


import android.Manifest.permission.ACCESS_COARSE_LOCATION
import android.Manifest.permission.ACCESS_FINE_LOCATION
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.test.core.app.ApplicationProvider.getApplicationContext
import androidx.test.platform.app.InstrumentationRegistry.getInstrumentation
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.UiObjectNotFoundException
import androidx.test.uiautomator.UiSelector
import com.schibsted.spain.barista.interaction.BaristaSleepInteractions.sleepThread

object LocationPermissionGranter {

    private const val PERMISSIONS_DIALOG_DELAY = 3000
    private val PERMISSIONS_DIALOG_ALLOW_ID = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        "com.android.permissioncontroller:id/permission_allow_button"
    } else {
        "com.android.packageinstaller:id/permission_allow_button"
    }

    private val PERMISSIONS_DIALOG_ALLOW_FOREGROUND_ID = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        "com.android.permissioncontroller:id/permission_allow_foreground_only_button"
    } else {
        "com.android.packageinstaller:id/permission_allow_button"
    }
    //    private static final String PERMISSIONS_DIALOG_DENY_ID = "com.android.packageinstaller:id/permission_deny_button";

    @JvmStatic
    fun allowPermissionsIfNeeded(permissionNeeded: String) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !hasNeededPermission(getApplicationContext(),
                            permissionNeeded)) {
                sleepThread(PERMISSIONS_DIALOG_DELAY.toLong())
                val device = UiDevice.getInstance(getInstrumentation())

                val resourceId = if (permissionNeeded == ACCESS_FINE_LOCATION || permissionNeeded == ACCESS_COARSE_LOCATION) {
                    PERMISSIONS_DIALOG_ALLOW_FOREGROUND_ID
                } else {
                    PERMISSIONS_DIALOG_ALLOW_ID
                }
                val allowPermissions = device.findObject(UiSelector()
                        .clickable(true)
                        .checkable(false)
                        .resourceId(resourceId))
                if (allowPermissions.exists()) {
                    allowPermissions.click()
                }
            }
        } catch (e: UiObjectNotFoundException) {
            Log.e("Barista", "There is no permissions dialog to interact with", e)
        }
    }

    private fun hasNeededPermission(context: Context, permissionNeeded: String): Boolean {
        val permissionStatus = checkSelfPermission(context, permissionNeeded)
        return permissionStatus == PackageManager.PERMISSION_GRANTED
    }

    private fun checkSelfPermission(context: Context, permission: String): Int {
        return context.checkPermission(permission, android.os.Process.myPid(), android.os.Process.myUid())
    }
}