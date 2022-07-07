package org.owntracks.android.ui.preferences

import android.os.Bundle
import dagger.hilt.android.AndroidEntryPoint
import org.owntracks.android.R
import javax.inject.Inject

@AndroidEntryPoint
class MapFragment @Inject constructor() : AbstractPreferenceFragment() {
    override fun onCreatePreferencesFix(savedInstanceState: Bundle?, rootKey: String?) {
        super.onCreatePreferencesFix(savedInstanceState, rootKey)
        setPreferencesFromResource(R.xml.preferences_map, rootKey)
    }
}
