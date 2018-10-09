package org.owntracks.android.injection.modules.ActivityModules;

import android.support.v7.app.AppCompatActivity;

import org.owntracks.android.injection.modules.ActivityModule;
import org.owntracks.android.injection.qualifier.ActivityContext;
import org.owntracks.android.injection.scopes.PerActivity;
import org.owntracks.android.ui.map.MapMvvm;
import org.owntracks.android.ui.preferences.PreferencesActivity;
import org.owntracks.android.ui.welcome.WelcomeActivity;
import org.owntracks.android.ui.welcome.WelcomeViewModel;

import dagger.Binds;
import dagger.Module;

@Module(includes = ActivityModule.class)
public abstract class PreferencesActivityModule {

    @Binds
    @PerActivity
    abstract AppCompatActivity appCompatActivity(PreferencesActivity a);
}