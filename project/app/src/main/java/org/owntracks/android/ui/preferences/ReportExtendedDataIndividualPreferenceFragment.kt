package org.owntracks.android.ui.preferences

import android.os.Bundle
import dagger.hilt.android.AndroidEntryPoint
import org.owntracks.android.R

@AndroidEntryPoint
class ReportExtendedDataIndividualPreferenceFragment : PreferenceFragmentCompatMasterSwitch() {
    override fun onCreatePreferencesFix(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.preferences_reporting_extended_individual, rootKey)
    }
}