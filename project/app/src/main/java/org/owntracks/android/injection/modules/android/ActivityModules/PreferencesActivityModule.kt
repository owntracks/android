package org.owntracks.android.injection.modules.android.ActivityModules

import androidx.appcompat.app.AppCompatActivity
import dagger.Binds
import dagger.Module
import dagger.android.ContributesAndroidInjector
import org.owntracks.android.injection.scopes.PerActivity
import org.owntracks.android.injection.scopes.PerFragment
import org.owntracks.android.ui.preferences.*

@Module(includes = [BaseActivityModule::class])
abstract class PreferencesActivityModule {
    @Binds
    @PerActivity
    abstract fun bindActivity(a: PreferencesActivity?): AppCompatActivity?

    @ContributesAndroidInjector
    @PerFragment
    abstract fun bindPreferencesFragment(): PreferencesFragment?

    @ContributesAndroidInjector
    @PerFragment
    abstract fun bindReportingPreferencesFragment(): ReportingFragment?

    @ContributesAndroidInjector
    @PerFragment
    abstract fun bindNotificationPreferencesFragment(): NotificationFragment?

    @ContributesAndroidInjector
    @PerFragment
    abstract fun bindAdvancedPreferencesFragment(): AdvancedFragment?
}