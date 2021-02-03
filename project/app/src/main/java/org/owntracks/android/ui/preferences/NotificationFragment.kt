package org.owntracks.android.ui.preferences

import android.os.Bundle
import org.owntracks.android.R
import org.owntracks.android.injection.scopes.PerFragment

@PerFragment
class NotificationFragment : AbstractPreferenceFragment() {
    override fun onCreatePreferencesFix(savedInstanceState: Bundle?, rootKey: String?) {
        super.onCreatePreferencesFix(savedInstanceState, rootKey)
        setPreferencesFromResource(R.xml.preferences_notification, rootKey)
    }
}