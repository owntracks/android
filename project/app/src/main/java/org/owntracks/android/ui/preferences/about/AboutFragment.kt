package org.owntracks.android.ui.preferences.about

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.core.net.toUri
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import org.owntracks.android.BuildConfig.GIT_COMMIT
import org.owntracks.android.BuildConfig.TRANSLATION_COUNT
import org.owntracks.android.R

class AboutFragment : PreferenceFragmentCompat() {
  override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
    setPreferencesFromResource(R.xml.about, rootKey)
    val versionPreference = findPreference<Preference>(UI_PREFERENCE_VERSION)
    val versionName =
        requireActivity()
            .packageManager
            .getPackageInfoCompat(requireActivity().packageName)
            .versionName
    versionPreference?.intent?.data = getString(R.string.changelogUrl).toUri()
    versionPreference?.setSummaryProvider {
      try {
        val pm = requireActivity().packageManager

        @Suppress("DEPRECATION")
        val versionCode =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
              pm.getPackageInfo(requireActivity().packageName, 0).longVersionCode
            } else {
              pm.getPackageInfo(requireActivity().packageName, 0).versionCode
            }
        val flavor = getString(R.string.aboutFlavorName)
        "${getString(R.string.version)} $versionName ($versionCode) - $flavor"
      } catch (e: PackageManager.NameNotFoundException) {
        getString(R.string.na)
      }
    }

    findPreference<Preference>(UI_PREFERENCE_COMMIT)?.apply {
      summary = GIT_COMMIT
      setOnPreferenceClickListener {
        val clipboard =
            requireContext().getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        clipboard.setPrimaryClip(ClipData.newPlainText("commit", GIT_COMMIT))
        // Android 13 and later confirm a copy themselves.
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
          Toast.makeText(requireContext(), R.string.aboutCommitCopied, Toast.LENGTH_SHORT).show()
        }
        true
      }
    }

    findPreference<Preference>(UI_PREFERENCE_TRANSLATION)?.setSummaryProvider {
      resources.getQuantityString(
          R.plurals.aboutTranslationsSummary,
          TRANSLATION_COUNT,
          TRANSLATION_COUNT,
      )
    }
  }

  companion object {
    const val UI_PREFERENCE_VERSION = "version"
    const val UI_PREFERENCE_COMMIT = "commit"
    const val UI_PREFERENCE_TRANSLATION = "translation"
  }

  // https://stackoverflow.com/a/74741495
  private fun PackageManager.getPackageInfoCompat(packageName: String): PackageInfo =
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        getPackageInfo(packageName, PackageManager.PackageInfoFlags.of(0L))
      } else {
        @Suppress("DEPRECATION") getPackageInfo(packageName, 0)
      }
}
