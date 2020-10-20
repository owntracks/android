package org.owntracks.android.ui.preferences

import android.os.Bundle
import dagger.Binds
import dagger.Module
import org.owntracks.android.R
import org.owntracks.android.injection.modules.android.FragmentModules.BaseFragmentModule
import org.owntracks.android.injection.scopes.PerFragment


@PerFragment
class ReportingFragment : AbstractPreferenceFragment() {
    override fun onCreatePreferencesFix(savedInstanceState: Bundle?, rootKey: String?) {
        super.onCreatePreferencesFix(savedInstanceState, rootKey)
        setPreferencesFromResource(R.xml.preferences_reporting, rootKey)
    }

    @Module(includes = [BaseFragmentModule::class])
    internal abstract class FragmentModule {
        @Binds
        @PerFragment
        abstract fun bindFragment(reportingFragment: ReportingFragment): ReportingFragment
    }
}
