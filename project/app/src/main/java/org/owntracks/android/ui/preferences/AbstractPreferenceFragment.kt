package org.owntracks.android.ui.preferences

import android.os.Bundle
import com.takisoft.preferencex.PreferenceFragmentCompat
import org.owntracks.android.support.Preferences
import javax.inject.Inject

abstract class AbstractPreferenceFragment : PreferenceFragmentCompat() {
    @Inject
    lateinit var preferences: Preferences

    override fun onCreatePreferencesFix(savedInstanceState: Bundle?, rootKey: String?) {
        preferenceManager.sharedPreferencesName = preferences.sharedPreferencesName
    }
}