package org.owntracks.android.ui.preferences

import android.content.Context
import android.os.Bundle
import com.takisoft.preferencex.PreferenceFragmentCompat;
import dagger.android.support.AndroidSupportInjection
import org.owntracks.android.support.Preferences
import javax.inject.Inject

abstract class AbstractPreferenceFragment: PreferenceFragmentCompat() {
    @Inject
    lateinit var preferences: Preferences

    override fun onCreatePreferencesFix(savedInstanceState: Bundle?, rootKey: String?) {
        preferenceManager.sharedPreferencesName = preferences.sharedPreferencesName
    }
    override fun onAttach(context: Context) {
        AndroidSupportInjection.inject(this)
        super.onAttach(context)
    }
}