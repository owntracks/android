package org.owntracks.android.ui.preferences

import android.os.Bundle
import androidx.preference.PreferenceFragmentCompat
import javax.inject.Inject
import org.owntracks.android.preferences.PreferenceDataStoreShim
import org.owntracks.android.preferences.Preferences

abstract class AbstractPreferenceFragment : PreferenceFragmentCompat() {

    @Inject
    lateinit var preferences: Preferences

    @Inject
    lateinit var preferenceDataStore: PreferenceDataStoreShim

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        preferenceManager.preferenceDataStore = preferenceDataStore
    }
}
