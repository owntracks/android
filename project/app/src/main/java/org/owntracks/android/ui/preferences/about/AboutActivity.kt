package org.owntracks.android.ui.preferences.about

import androidx.fragment.app.Fragment
import org.owntracks.android.ui.preferences.PreferencesActivity

class AboutActivity : PreferencesActivity() {
    override fun getStartFragment(): Fragment {
        return AboutFragment()
    }
}