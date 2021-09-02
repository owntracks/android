package org.owntracks.android.ui.preferences

import android.os.Build
import android.os.Bundle
import androidx.preference.Preference
import dagger.hilt.android.AndroidEntryPoint
import org.owntracks.android.R

@AndroidEntryPoint
class AutoStartOnBootIndividualPreferenceFragment : PreferenceFragmentCompatMasterSwitch() {
    override fun onCreatePreferencesFix(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.preferences_autostart_individual, rootKey)
        findPreference<Preference>("autostartWarning")?.isVisible =
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.R
    }
}