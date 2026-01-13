package org.owntracks.android.ui.preferences

import android.content.Intent
import android.os.Bundle
import androidx.preference.Preference
import dagger.hilt.android.AndroidEntryPoint
import org.owntracks.android.R
import org.owntracks.android.preferences.Preferences.Companion.EXPERIMENTAL_FEATURE_SHOW_EXPERIMENTAL_PREFERENCE_UI
import org.owntracks.android.ui.preferences.editor.EditorActivity

@AndroidEntryPoint
class PreferencesFragment : AbstractPreferenceFragment() {
  override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
    super.onCreatePreferences(savedInstanceState, rootKey)
    setPreferencesFromResource(R.xml.preferences_root, rootKey)
    // Have to do these manually here, as there's an android bug that prevents the activity from
    // being found when launched from intent declared on the preferences XML.
    findPreference<Preference>(UI_SCREEN_CONFIGURATION)?.intent =
        Intent(context, EditorActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION)

    findPreference<Preference>(UI_PREFERENCE_SCREEN_EXPERIMENTAL)?.run {
      this.isVisible =
          preferences.experimentalFeatures.contains(
              EXPERIMENTAL_FEATURE_SHOW_EXPERIMENTAL_PREFERENCE_UI)
    }

    // Setup click handlers for Status, About, and Exit
    findPreference<Preference>(UI_PREFERENCE_STATUS)?.setOnPreferenceClickListener {
      (activity as? PreferencesActivity)?.navigateToStatus()
      true
    }

    findPreference<Preference>(UI_PREFERENCE_ABOUT)?.setOnPreferenceClickListener {
      (activity as? PreferencesActivity)?.navigateToAbout()
      true
    }

    findPreference<Preference>(UI_PREFERENCE_EXIT)?.setOnPreferenceClickListener {
      (activity as? PreferencesActivity)?.exitApp()
      true
    }
  }

  override fun onResume() {
    super.onResume()
    findPreference<Preference>(UI_PREFERENCE_SCREEN_CONNECTION)?.summary = connectionMode
    findPreference<Preference>(UI_PREFERENCE_SCREEN_EXPERIMENTAL)?.run {
      this.isVisible =
          preferences.experimentalFeatures.contains(
              EXPERIMENTAL_FEATURE_SHOW_EXPERIMENTAL_PREFERENCE_UI)
    }
  }

  companion object {
    private const val UI_PREFERENCE_SCREEN_CONNECTION = "connectionScreen"
    private const val UI_SCREEN_CONFIGURATION = "configuration"
    private const val UI_PREFERENCE_SCREEN_EXPERIMENTAL = "experimentalScreen"
    private const val UI_PREFERENCE_STATUS = "status"
    private const val UI_PREFERENCE_ABOUT = "about"
    private const val UI_PREFERENCE_EXIT = "exit"
  }
}
