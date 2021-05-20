package org.owntracks.android.ui.preferences

import android.os.Bundle
import dagger.hilt.android.AndroidEntryPoint
import org.owntracks.android.R

@AndroidEntryPoint
class ReportingFragment : AbstractPreferenceFragment() {
    override fun onCreatePreferencesFix(savedInstanceState: Bundle?, rootKey: String?) {
        super.onCreatePreferencesFix(savedInstanceState, rootKey)
        setPreferencesFromResource(R.xml.preferences_reporting, rootKey)
    }
}
