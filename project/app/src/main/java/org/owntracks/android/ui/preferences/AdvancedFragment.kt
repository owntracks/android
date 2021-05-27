package org.owntracks.android.ui.preferences

import android.os.Bundle
import androidx.preference.*
import dagger.hilt.android.AndroidEntryPoint
import org.owntracks.android.R
import org.owntracks.android.support.Preferences
import javax.inject.Inject

@AndroidEntryPoint
class AdvancedFragment @Inject constructor() : AbstractPreferenceFragment() {
    override fun onCreatePreferencesFix(savedInstanceState: Bundle?, rootKey: String?) {
        super.onCreatePreferencesFix(savedInstanceState, rootKey)
        setPreferencesFromResource(R.xml.preferences_advanced, rootKey)
        val remoteConfigurationPreference =
            findPreference<SwitchPreferenceCompat>(getString(R.string.preferenceKeyRemoteConfiguration))
        val remoteCommandPreference =
            findPreference<SwitchPreferenceCompat>(getString(R.string.preferenceKeyRemoteCommand))
        val remoteCommandAndConfigurationChangeListener =
            Preference.OnPreferenceChangeListener { preference, newValue ->
                if (newValue is Boolean) {
                    when (preference.key) {
                        getString(R.string.preferenceKeyRemoteCommand) -> if (!newValue) remoteConfigurationPreference?.isChecked =
                            false
                        getString(R.string.preferenceKeyRemoteConfiguration) -> if (newValue) remoteCommandPreference?.isChecked =
                            true
                    }
                }
                true
            }
        remoteConfigurationPreference?.onPreferenceChangeListener =
            remoteCommandAndConfigurationChangeListener
        remoteCommandPreference?.onPreferenceChangeListener =
            remoteCommandAndConfigurationChangeListener

        findPreference<ListPreference>(getString(R.string.preferenceKeyReverseGeocodeProvider))?.setOnPreferenceChangeListener { _, newValue ->
            preferences.reverseGeocodeProvider = newValue.toString()
            setOpenCageAPIKeyPreferenceVisibility()
            true
        }
        setOpenCageAPIKeyPreferenceVisibility()

        findPreference<EditTextPreference>(getString(R.string.preferenceKeyOpencageGeocoderApiKey))?.setOnPreferenceChangeListener { preference, newValue ->
            val trimmed = (newValue as String).trim()
            preferences.openCageGeocoderApiKey = trimmed
            (preference as EditTextPreference).text = trimmed
            false
        }
    }

    private fun setOpenCageAPIKeyPreferenceVisibility() {
        findPreference<EditTextPreference>(getString(R.string.preferenceKeyOpencageGeocoderApiKey))?.isVisible =
            preferences.reverseGeocodeProvider == Preferences.REVERSE_GEOCODE_PROVIDER_OPENCAGE
    }
}