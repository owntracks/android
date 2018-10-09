package org.owntracks.android.injection.modules.ActivityModules;

import android.support.v7.app.AppCompatActivity;

import org.owntracks.android.injection.modules.ActivityModule;
import org.owntracks.android.injection.qualifier.ActivityContext;
import org.owntracks.android.injection.scopes.PerActivity;
import org.owntracks.android.ui.map.MapMvvm;
import org.owntracks.android.ui.preferences.load.LoadActivity;
import org.owntracks.android.ui.preferences.load.LoadMvvm;
import org.owntracks.android.ui.preferences.load.LoadViewModel;

import dagger.Binds;
import dagger.Module;


@Module(includes = ActivityModule.class)
public abstract class LoadActivityModule {

    @Binds
    @PerActivity
    abstract AppCompatActivity appCompatActivity(LoadActivity a);

    @Binds abstract LoadMvvm.ViewModel bindViewModel(LoadViewModel viewModel);
}
