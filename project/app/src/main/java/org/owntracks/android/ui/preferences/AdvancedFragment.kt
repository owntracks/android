package org.owntracks.android.ui.preferences

import android.os.Bundle
import androidx.core.app.NotificationManagerCompat
import androidx.preference.Preference
import dagger.Binds
import dagger.Module
import org.owntracks.android.R
import org.owntracks.android.injection.modules.android.FragmentModules.BaseFragmentModule
import org.owntracks.android.injection.scopes.PerFragment
import org.owntracks.android.support.TimberDebugLogFileTree
import timber.log.Timber

@PerFragment
class AdvancedFragment : AbstractPreferenceFragment() {

    override fun onCreatePreferencesFix(savedInstanceState: Bundle?, rootKey: String?) {
        super.onCreatePreferencesFix(savedInstanceState, rootKey)
        setPreferencesFromResource(R.xml.preferences_advanced, rootKey)

        findPreference<Preference>(getString(R.string.preferenceKeyDebugLog))?.onPreferenceChangeListener = Preference.OnPreferenceChangeListener { _: Preference, newValue: Any? ->
            if (newValue as Boolean) {
                enableDebugLog()
            } else {
                disableDebugLog()
            }
            true
        }
    }

    private fun enableDebugLog() {
        Timber.plant(TimberDebugLogFileTree(requireActivity()))
        Timber.d("Debug logging enabled")
    }

    private fun disableDebugLog() {
        Timber.forest().filterIsInstance<TimberDebugLogFileTree>().forEach { Timber.uproot(it) }
        NotificationManagerCompat.from(requireContext()).cancel(TimberDebugLogFileTree.DEBUG_NOTIFICATION_ID)
        Timber.i("Debug logging disabled")

    }

    @Module(includes = [BaseFragmentModule::class])
    internal abstract class FragmentModule {
        @Binds
        @PerFragment
        abstract fun bindFragment(reportingFragment: AdvancedFragment): AdvancedFragment
    }

}
