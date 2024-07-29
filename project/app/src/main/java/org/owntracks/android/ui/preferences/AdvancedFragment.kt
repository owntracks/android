package org.owntracks.android.ui.preferences

import android.content.Context
import android.os.Bundle
import android.widget.TextView
import androidx.preference.ListPreference
import androidx.preference.Preference
import androidx.preference.SwitchPreferenceCompat
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import org.owntracks.android.R
import org.owntracks.android.preferences.Preferences
import org.owntracks.android.preferences.types.ReverseGeocodeProvider
import org.owntracks.android.support.RequirementsChecker

@AndroidEntryPoint
class AdvancedFragment @Inject constructor() :
    AbstractPreferenceFragment(), Preferences.OnPreferenceChangeListener {
  @Inject lateinit var requirementsChecker: RequirementsChecker

  override fun onAttach(context: Context) {
    super.onAttach(context)
    preferences.registerOnPreferenceChangedListener(this)
  }

  override fun onDetach() {
    super.onDetach()
    preferences.unregisterOnPreferenceChangedListener(this)
  }

  override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
    super.onCreatePreferences(savedInstanceState, rootKey)
    setPreferencesFromResource(R.xml.preferences_advanced, rootKey)
    val remoteConfigurationPreference =
        findPreference<SwitchPreferenceCompat>(Preferences::remoteConfiguration.name)
    val remoteCommandPreference = findPreference<SwitchPreferenceCompat>(Preferences::cmd.name)
    val remoteCommandAndConfigurationChangeListener =
        Preference.OnPreferenceChangeListener { preference, newValue ->
          if (newValue is Boolean) {
            when (preference.key) {
              Preferences::cmd.name ->
                  if (!newValue) {
                    remoteConfigurationPreference?.isChecked = false
                  }
              Preferences::remoteConfiguration.name ->
                  if (newValue) {
                    remoteCommandPreference?.isChecked = true
                  }
            }
          }
          true
        }
    remoteConfigurationPreference?.onPreferenceChangeListener =
        remoteCommandAndConfigurationChangeListener
    remoteCommandPreference?.onPreferenceChangeListener =
        remoteCommandAndConfigurationChangeListener

    findPreference<Preference>("autostartWarning")?.isVisible =
        !requirementsChecker.hasBackgroundLocationPermission()

    findPreference<ListPreference>(Preferences::reverseGeocodeProvider.name)
        ?.onPreferenceChangeListener =
        Preference.OnPreferenceChangeListener { preference, newValue ->
          if (newValue == ReverseGeocodeProvider.OPENCAGE.value) {
            MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.preferencesAdvancedOpencagePrivacyDialogTitle)
                .setMessage(R.string.preferencesAdvancedOpencagePrivacyDialogMessage)
                .setPositiveButton(R.string.preferencesAdvancedOpencagePrivacyDialogAccept) { _, _
                  ->
                  (preference as ListPreference).value = newValue.toString()
                }
                .setNegativeButton(R.string.preferencesAdvancedOpencagePrivacyDialogCancel, null)
                .create()
                .apply { show() }
                .findViewById<TextView>(android.R.id.message)
                ?.movementMethod = android.text.method.LinkMovementMethod.getInstance()
            false
          } else {
            true
          }
        }
    setOpenCageAPIKeyPreferenceVisibility()
  }

  private fun setOpenCageAPIKeyPreferenceVisibility() {
    setOf(Preferences::opencageApiKey.name, "opencagePrivacy").forEach {
      findPreference<Preference>(it)?.isVisible =
          preferences.reverseGeocodeProvider == ReverseGeocodeProvider.OPENCAGE
    }
  }

  override fun onPreferenceChanged(properties: Set<String>) {
    if (properties.contains(Preferences::reverseGeocodeProvider.name)) {
      setOpenCageAPIKeyPreferenceVisibility()
    }
  }
}
