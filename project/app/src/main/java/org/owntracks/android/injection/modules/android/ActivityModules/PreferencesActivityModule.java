package org.owntracks.android.injection.modules.android.ActivityModules;

import androidx.appcompat.app.AppCompatActivity;

import org.owntracks.android.injection.modules.android.FragmentModules.PreferencesFragmentModule;
import org.owntracks.android.injection.scopes.PerActivity;
import org.owntracks.android.injection.scopes.PerFragment;
import org.owntracks.android.ui.preferences.AdvancedFragment;
import org.owntracks.android.ui.preferences.NotificationFragment;
import org.owntracks.android.ui.preferences.PreferencesActivity;
import org.owntracks.android.ui.preferences.ReportingFragment;

import dagger.Binds;
import dagger.Module;
import dagger.android.ContributesAndroidInjector;

@Module(includes = BaseActivityModule.class)
public abstract class PreferencesActivityModule {

    @Binds
    @PerActivity
    abstract AppCompatActivity bindActivity(PreferencesActivity a);

    @ContributesAndroidInjector(modules = {PreferencesFragmentModule.class})
    @PerFragment
    abstract org.owntracks.android.ui.preferences.PreferencesFragment bindPreferencesFragment();

    @ContributesAndroidInjector(modules = {ReportingFragment.FragmentModule.class})
    @PerFragment
    abstract org.owntracks.android.ui.preferences.ReportingFragment bindReportingPreferencesFragment();

    @ContributesAndroidInjector(modules = {NotificationFragment.FragmentModule.class})
    @PerFragment
    abstract org.owntracks.android.ui.preferences.NotificationFragment bindNotificationPreferencesFragment();

    @ContributesAndroidInjector(modules = {AdvancedFragment.FragmentModule.class})
    @PerFragment
    abstract org.owntracks.android.ui.preferences.AdvancedFragment bindAdvancedPreferencesFragment();

}