package org.owntracks.android.ui.preferences

import android.os.Bundle
import androidx.preference.PreferenceFragmentCompat
import javax.inject.Inject
import org.owntracks.android.R
import org.owntracks.android.preferences.PreferenceDataStoreShim
import org.owntracks.android.preferences.Preferences
import org.owntracks.android.preferences.types.ConnectionMode

abstract class AbstractPreferenceFragment : PreferenceFragmentCompat() {
  @Inject lateinit var preferences: Preferences

  @Inject lateinit var preferenceDataStore: PreferenceDataStoreShim

  override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
    preferenceManager.preferenceDataStore = preferenceDataStore
  }

  protected val connectionMode: String
    get() =
        when (preferences.mode) {
          ConnectionMode.HTTP -> getString(R.string.mode_http_private_label)
          ConnectionMode.MQTT -> getString(R.string.mode_mqtt_private_label)
        }
}
