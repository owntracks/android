package org.owntracks.android.ui.preferences

import android.os.Bundle
import androidx.preference.SwitchPreferenceCompat
import org.owntracks.android.R
import org.owntracks.android.injection.scopes.PerFragment
import org.owntracks.android.support.Preferences.Companion.EXPERIMENTAL_FEATURES

@PerFragment
class ExperimentalFragment : AbstractPreferenceFragment() {
    override fun onCreatePreferencesFix(savedInstanceState: Bundle?, rootKey: String?) {
        super.onCreatePreferencesFix(savedInstanceState, rootKey)
        setPreferencesFromResource(R.xml.preferences_experimental, rootKey)

        EXPERIMENTAL_FEATURES.forEach { feature ->
            SwitchPreferenceCompat(requireContext()).apply {
                title = feature
                isChecked = preferences.isExperimentalFeatureEnabled(feature)
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
        }


    }

}
