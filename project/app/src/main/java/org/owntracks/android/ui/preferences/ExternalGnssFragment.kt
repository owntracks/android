package org.owntracks.android.ui.preferences

import android.os.Bundle
import androidx.preference.EditTextPreference
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import org.owntracks.android.R
import org.owntracks.android.location.external.ExternalGnssController
import org.owntracks.android.location.external.NtripConfig

/**
 * Dedicated preference screen to enable the external USB GNSS receiver and configure an NTRIP
 * caster. Uses its own SharedPreferences file so it does not collide with OwnTracks' own keyed
 * preference store.
 */
@AndroidEntryPoint
class ExternalGnssFragment : PreferenceFragmentCompat() {

  @Inject lateinit var externalGnssController: ExternalGnssController

  override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
    preferenceManager.sharedPreferencesName = NtripConfig.PREFS_NAME
    setPreferencesFromResource(R.xml.preferences_external_gnss, rootKey)

    findPreference<EditTextPreference>(NtripConfig.KEY_PORT)?.setOnPreferenceChangeListener {
        _,
        newValue ->
      (newValue as? String)?.toIntOrNull() != null || newValue == ""
    }

    findPreference<Preference>("external_gnss_connect")?.setOnPreferenceClickListener {
      externalGnssController.tryStartFromAttachedDevices()
      true
    }
  }
}
