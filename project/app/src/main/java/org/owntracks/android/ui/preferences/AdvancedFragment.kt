package org.owntracks.android.ui.preferences

import android.content.Intent
import android.os.Bundle
import androidx.core.app.TaskStackBuilder
import androidx.preference.Preference
import androidx.preference.SwitchPreferenceCompat
import dagger.Binds
import dagger.Module
import org.owntracks.android.R
import org.owntracks.android.injection.modules.android.FragmentModules.BaseFragmentModule
import org.owntracks.android.injection.scopes.PerFragment
import org.owntracks.android.ui.map.MapActivity


@PerFragment
class AdvancedFragment : AbstractPreferenceFragment() {
    override fun onCreatePreferencesFix(savedInstanceState: Bundle?, rootKey: String?) {
        super.onCreatePreferencesFix(savedInstanceState, rootKey)
        setPreferencesFromResource(R.xml.preferences_advanced, rootKey)
        val remoteConfigurationPreference = findPreference<SwitchPreferenceCompat>(getString(R.string.preferenceKeyRemoteConfiguration))
        val remoteCommandPreference = findPreference<SwitchPreferenceCompat>(getString(R.string.preferenceKeyRemoteCommand))
        val remoteCommandAndConfigurationChangeListener = Preference.OnPreferenceChangeListener { preference, newValue ->
            if (newValue is Boolean) {
                when (preference.key) {
                    getString(R.string.preferenceKeyRemoteCommand) -> if (!newValue) remoteConfigurationPreference?.isChecked = false
                    getString(R.string.preferenceKeyRemoteConfiguration) -> if (newValue) remoteCommandPreference?.isChecked = true
                }
            }
            true
        }
        findPreference<SwitchPreferenceCompat>(getString(R.string.preferenceKeyDarkMode))?.setOnPreferenceChangeListener { _, _ ->
            TaskStackBuilder.create(requireActivity())
                    .addNextIntent(Intent(activity, MapActivity::class.java))
                    .addNextIntent(requireActivity().intent)
                    .startActivities()
            true
        }
        remoteConfigurationPreference?.onPreferenceChangeListener = remoteCommandAndConfigurationChangeListener
        remoteCommandPreference?.onPreferenceChangeListener = remoteCommandAndConfigurationChangeListener
    }

    @Module(includes = [BaseFragmentModule::class])
    internal abstract class FragmentModule {
        @Binds
        @PerFragment
        abstract fun bindFragment(reportingFragment: AdvancedFragment): AdvancedFragment
    }
}
