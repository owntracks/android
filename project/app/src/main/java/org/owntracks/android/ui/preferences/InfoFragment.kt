package org.owntracks.android.ui.preferences

import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import org.owntracks.android.R

class InfoFragment : PreferenceFragmentCompat() {
    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.preferences_info, rootKey)
        findPreference<Preference>(UI_PREFERENCE_VERSION)?.setSummaryProvider { _ ->
            try {
                val pm = requireActivity().packageManager
                @Suppress("DEPRECATION")
                "%s (%s)".format(
                        pm.getPackageInfo(requireActivity().packageName, 0).versionName,
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