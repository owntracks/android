package org.owntracks.android.ui.preferences

import android.os.Bundle
import androidx.preference.SwitchPreferenceCompat
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import org.owntracks.android.R
import org.owntracks.android.preferences.Preferences.Companion.EXPERIMENTAL_FEATURES

@AndroidEntryPoint
class ExperimentalFragment @Inject constructor() : AbstractPreferenceFragment() {
  override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
    super.onCreatePreferences(savedInstanceState, rootKey)
    setPreferencesFromResource(R.xml.preferences_experimental, rootKey)

    EXPERIMENTAL_FEATURES.forEach { feature ->
      SwitchPreferenceCompat(requireContext())
          .apply {
            title = feature
            isIconSpaceReserved = false
            setOnPreferenceClickListener {
              val newFeatures = preferences.experimentalFeatures.toMutableSet()
              if ((it as SwitchPreferenceCompat).isChecked) {
                newFeatures.add(feature)
              } else {
                newFeatures.remove(feature)
              }
              preferences.experimentalFeatures = newFeatures
              true
            }
            preferenceScreen.addPreference(this)
          }
          .apply { isChecked = preferences.experimentalFeatures.contains(feature) }
    }
  }
}
