package org.owntracks.android.ui.preferences

import android.os.Bundle
import androidx.preference.Preference
import dagger.hilt.android.AndroidEntryPoint
import org.owntracks.android.R

@AndroidEntryPoint
class ReportingFragment : AbstractPreferenceFragment() {
    override fun onCreatePreferencesFix(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.preferences_reporting, rootKey)
        findPreference<Preference>(getString(R.string.preferenceKeyPublishExtendedData))?.setSummaryProvider {
            if (preferences.pubLocationExtendedData)
                getString(R.string.preferenceOn)
            else
                getString(R.string.preferenceOff)
        }
    }
}