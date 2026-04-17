package org.owntracks.android.ui.preferences

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.preference.Preference
import androidx.preference.SwitchPreferenceCompat
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import org.owntracks.android.R
import org.owntracks.android.preferences.Preferences
import org.owntracks.android.support.RequirementsChecker

@AndroidEntryPoint
class RemoteControlFragment @Inject constructor() : AbstractPreferenceFragment() {
  @Inject lateinit var requirementsChecker: RequirementsChecker

  override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
    super.onCreatePreferences(savedInstanceState, rootKey)
    setPreferencesFromResource(R.xml.preferences_remote_control, rootKey)

    findPreference<SwitchPreferenceCompat>(Preferences::allowIntentControl.name)
        ?.onPreferenceClickListener =
        Preference.OnPreferenceClickListener {
          refreshPreferenceState()
          false
        }

    findPreference<Preference>(Preferences::intentAuthKey.name)?.apply {
      summary = preferences.intentAuthKey
      onPreferenceClickListener =
          Preference.OnPreferenceClickListener {
            val clipboard =
                requireContext().getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            clipboard.setPrimaryClip(
                ClipData.newPlainText("intentAuthKey", preferences.intentAuthKey))
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
              Toast.makeText(requireContext(), R.string.intentAuthKeyCopied, Toast.LENGTH_SHORT)
                  .show()
            }
            true
          }
    }

    findPreference<SwitchPreferenceCompat>(Preferences::allowConfigurationByURIAndConfigFile.name)
        ?.onPreferenceChangeListener =
        Preference.OnPreferenceChangeListener { preference, newValue ->
          if (newValue == true) {
            MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.preferencesAllowConfigurationByURIAndConfigFileWarningTitle)
                .setMessage(R.string.preferencesAllowConfigurationByURIAndConfigFileWarningMessage)
                .setIcon(R.drawable.ic_baseline_warning_24)
                .setPositiveButton(R.string.enable) { _, _ ->
                  preferences.allowConfigurationByURIAndConfigFile = true
                  (preference as SwitchPreferenceCompat).isChecked = true
                }
                .setNegativeButton(R.string.cancel, null)
                .show()
            false // block the commit; dialog's positive button handles it if confirmed
          } else {
            true // disabling needs no confirmation
          }
        }

    refreshPreferenceState()
  }

  private fun refreshPreferenceState() {
    findPreference<Preference>(Preferences::intentAuthKey.name)?.isEnabled =
        preferences.allowIntentControl
  }

  override fun onResume() {
    refreshPreferenceState()
    super.onResume()
  }
}
