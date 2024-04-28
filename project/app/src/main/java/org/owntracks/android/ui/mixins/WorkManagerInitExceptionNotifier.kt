package org.owntracks.android.ui.mixins

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import org.owntracks.android.App
import org.owntracks.android.R

/**
 * Provides a mixin for activities that want to notify the user that the WorkManager initialization
 * failed. Usually used by activities that might be the "first" activity viewed by the user
 * ([org.owntracks.android.ui.map.MapActivity],
 * [org.owntracks.android.ui.preferences.PreferencesActivity] etc.)
 *
 * @constructor Create empty Work manager init exception notifier
 */
interface WorkManagerInitExceptionNotifier {
  fun notifyOnWorkManagerInitFailure(appCompatActivity: AppCompatActivity)

  class Impl : WorkManagerInitExceptionNotifier {
    override fun notifyOnWorkManagerInitFailure(appCompatActivity: AppCompatActivity) {
      (appCompatActivity.applicationContext as App).workManagerFailedToInitialize.observe(
          appCompatActivity) { value ->
            if (value) {
              MaterialAlertDialogBuilder(appCompatActivity)
                  .setIcon(R.drawable.ic_baseline_warning_24)
                  .setTitle(
                      appCompatActivity.getString(
                          R.string.workmanagerInitializationErrorDialogTitle))
                  .setMessage(
                      appCompatActivity.getString(
                          R.string.workmanagerInitializationErrorDialogMessage))
                  .setPositiveButton(
                      appCompatActivity.getString(
                          R.string.workmanagerInitializationErrorDialogOpenSettingsLabel)) { _, _ ->
                        appCompatActivity.startActivity(
                            Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                              data = Uri.fromParts("package", appCompatActivity.packageName, "")
                            })
                      }
                  .setCancelable(true)
                  .show()
            }
          }
    }
  }
}
