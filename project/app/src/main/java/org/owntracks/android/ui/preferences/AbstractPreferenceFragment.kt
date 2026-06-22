package org.owntracks.android.ui.preferences

import android.os.Bundle
import android.view.View
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
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

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    super.onViewCreated(view, savedInstanceState)
    ViewCompat.setOnApplyWindowInsetsListener(listView) { v, insets ->
      val systemBarsInsets = insets.getInsets(WindowInsetsCompat.Type.systemBars())
      v.updatePadding(bottom = systemBarsInsets.bottom)
      insets
    }
    ViewCompat.requestApplyInsets(view)
  }

  protected val connectionMode: String
    get() =
        when (preferences.mode) {
          ConnectionMode.HTTP -> getString(R.string.mode_http_private_label)
          ConnectionMode.MQTT -> getString(R.string.mode_mqtt_private_label)
        }
}
