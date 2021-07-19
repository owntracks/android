package org.owntracks.android.ui.preferences

import android.os.Bundle
import dagger.hilt.android.AndroidEntryPoint
import org.owntracks.android.R

@AndroidEntryPoint
class NotificationFragment : AbstractPreferenceFragment() {
    override fun onCreatePreferencesFix(savedInstanceState: Bundle?, rootKey: String?) {
        super.onCreatePreferencesFix(savedInstanceState, rootKey)
        setPreferencesFromResource(R.xml.preferences_notification, rootKey)
    }
}