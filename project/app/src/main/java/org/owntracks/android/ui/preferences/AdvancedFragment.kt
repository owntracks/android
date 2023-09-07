package org.owntracks.android.ui.preferences

import android.Manifest.permission.ACCESS_BACKGROUND_LOCATION
import android.content.Context
import android.os.Build
import android.os.Bundle
import androidx.core.content.PermissionChecker
import androidx.preference.EditTextPreference
import androidx.preference.Preference
import androidx.preference.SwitchPreferenceCompat
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import org.owntracks.android.R
import org.owntracks.android.preferences.Preferences
import org.owntracks.android.preferences.types.ReverseGeocodeProvider

@AndroidEntryPoint
class AdvancedFragment @Inject constructor() : AbstractPreferenceFragment(), Preferences.OnPreferenceChangeListener {
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
                        Preferences::cmd.name -> if (!newValue) {
                            remoteConfigurationPreference?.isChecked = false
                        }
                        Preferences::remoteConfiguration.name -> if (newValue) {
                            remoteCommandPreference?.isChecked = true
                        }
                    }
                }
                true
            }
        remoteConfigurationPreference?.onPreferenceChangeListener = remoteCommandAndConfigurationChangeListener
        remoteCommandPreference?.onPreferenceChangeListener = remoteCommandAndConfigurationChangeListener

        findPreference<Preference>("autostartWarning")?.isVisible =
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && PermissionChecker.checkSelfPermission(
            requireActivity(),
            ACCESS_BACKGROUND_LOCATION
        ) == PermissionChecker.PERMISSION_DENIED
        setOpenCageAPIKeyPreferenceVisibility()
    }

    private fun setOpenCageAPIKeyPreferenceVisibility() {
        findPreference<EditTextPreference>(Preferences::opencageApiKey.name)?.isVisible =
            preferences.reverseGeocodeProvider == ReverseGeocodeProvider.OPENCAGE
    }

    override fun onPreferenceChanged(properties: Set<String>) {
        if (properties.contains(Preferences::reverseGeocodeProvider.name)) {
            setOpenCageAPIKeyPreferenceVisibility()
        }
    }
}
