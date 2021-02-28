package org.owntracks.android.ui.preferences.about

import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import org.owntracks.android.R

class AboutFragment : PreferenceFragmentCompat() {
    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.about, rootKey)
        val versionPreference = findPreference<Preference>(UI_PREFERENCE_VERSION)
        val versionName = requireActivity().packageManager.getPackageInfo(requireActivity().packageName, 0).versionName
        versionPreference?.intent?.data = Uri.parse(getString(R.string.changelogUrl).format(versionName))
        versionPreference?.setSummaryProvider { _ ->
            try {
                val pm = requireActivity().packageManager
                @Suppress("DEPRECATION")
                "%s %s (%s)".format(
                        getString(R.string.version),
                        versionName,
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) pm.getPackageInfo(requireActivity().packageName, 0).longVersionCode else pm.getPackageInfo(requireActivity().packageName, 0).versionCode
                )
            } catch (e: PackageManager.NameNotFoundException) {
                getString(R.string.na)
            }
        }
    }

    companion object {
        const val UI_PREFERENCE_VERSION = "version"
    }
}