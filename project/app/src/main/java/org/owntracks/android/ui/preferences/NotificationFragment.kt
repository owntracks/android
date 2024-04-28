package org.owntracks.android.ui.preferences

import android.content.Intent
import android.content.Intent.FLAG_ACTIVITY_NEW_TASK
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS
import androidx.preference.Preference
import androidx.preference.SwitchPreferenceCompat
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import org.owntracks.android.R
import org.owntracks.android.preferences.Preferences
import org.owntracks.android.support.RequirementsChecker

@AndroidEntryPoint
class NotificationFragment @Inject constructor() : AbstractPreferenceFragment() {
  @Inject lateinit var requirementsChecker: RequirementsChecker

  override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
    super.onCreatePreferences(savedInstanceState, rootKey)
    setPreferencesFromResource(R.xml.preferences_notification, rootKey)
    refreshPreferenceState()
  }

  private fun refreshPreferenceState() {
    listOf(
            Preferences::notificationLocation.name,
            Preferences::notificationEvents.name,
            Preferences::notificationGeocoderErrors.name)
        .forEach { preferenceKey ->
          findPreference<SwitchPreferenceCompat>(preferenceKey)?.isEnabled =
              requirementsChecker.hasNotificationPermissions()
        }
    findPreference<Preference>("notificationPermission")?.apply {
      isVisible = !requirementsChecker.hasNotificationPermissions()
      setOnPreferenceClickListener {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
          startActivity(
              Intent(ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = Uri.parse("package:${context.packageName}")
                flags = FLAG_ACTIVITY_NEW_TASK
              })
        }
        true
      }
    }
  }

  override fun onResume() {
    refreshPreferenceState()
    super.onResume()
  }
}
