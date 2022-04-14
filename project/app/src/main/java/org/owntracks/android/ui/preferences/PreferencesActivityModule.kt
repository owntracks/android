package org.owntracks.android.ui.preferences

import androidx.fragment.app.Fragment
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ActivityComponent
import dagger.hilt.android.scopes.FragmentScoped

@InstallIn(ActivityComponent::class)
@Module
abstract class PreferencesActivityModule {
    @Binds
    @FragmentScoped
    abstract fun bindPreferencesFragment(preferencesFragment: PreferencesFragment): Fragment

    @Binds
    @FragmentScoped
    abstract fun bindReportingPreferencesFragment(reportingFragment: ReportingFragment): Fragment

    @Binds
    @FragmentScoped
    abstract fun bindNotificationPreferencesFragment(notificationFragment: NotificationFragment): Fragment

    @Binds
    @FragmentScoped
    abstract fun bindAdvancedPreferencesFragment(advancedFragment: AdvancedFragment): Fragment

    @Binds
    @FragmentScoped
    abstract fun bindExperimentalFragment(experimentalFragment: ExperimentalFragment): Fragment

    @Binds
    @FragmentScoped
    abstract fun bindMapFragment(mapFragment: MapFragment): Fragment
}