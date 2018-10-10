package org.owntracks.android.injection.modules.ActivityModules;

import android.support.v7.app.AppCompatActivity;

import org.owntracks.android.injection.modules.ActivityModule;
import org.owntracks.android.injection.modules.FragmentModules.FinishFragmentModule;
import org.owntracks.android.injection.modules.FragmentModules.PreferencesFragmentModule;
import org.owntracks.android.injection.qualifier.ActivityContext;
import org.owntracks.android.injection.scopes.PerActivity;
import org.owntracks.android.injection.scopes.PerFragment;
import org.owntracks.android.ui.map.MapMvvm;
import org.owntracks.android.ui.preferences.PreferencesActivity;
import org.owntracks.android.ui.welcome.WelcomeActivity;
import org.owntracks.android.ui.welcome.WelcomeViewModel;

import dagger.Binds;
import dagger.Module;
import dagger.android.ContributesAndroidInjector;

@Module(includes = ActivityModule.class)
public abstract class PreferencesActivityModule {

    @Binds
    @PerActivity
    abstract AppCompatActivity bindActivity(PreferencesActivity a);

    @ContributesAndroidInjector(modules = {PreferencesFragmentModule.class})
    @PerFragment
    abstract org.owntracks.android.ui.preferences.PreferencesFragment bindPreferencesFragment();

}