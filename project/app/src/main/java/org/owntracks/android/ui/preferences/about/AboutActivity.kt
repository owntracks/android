package org.owntracks.android.ui.preferences.about

import androidx.fragment.app.Fragment
import dagger.hilt.android.AndroidEntryPoint
import org.owntracks.android.ui.preferences.PreferencesActivity

@AndroidEntryPoint
class AboutActivity : PreferencesActivity() {
  override val startFragment: Fragment
    get() = AboutFragment()
}
