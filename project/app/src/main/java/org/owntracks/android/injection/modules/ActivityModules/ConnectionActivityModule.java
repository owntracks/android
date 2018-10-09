package org.owntracks.android.injection.modules.ActivityModules;

import android.support.v7.app.AppCompatActivity;

import org.owntracks.android.injection.modules.ActivityModule;
import org.owntracks.android.injection.qualifier.ActivityContext;
import org.owntracks.android.injection.scopes.PerActivity;
import org.owntracks.android.ui.map.MapMvvm;
import org.owntracks.android.ui.preferences.connection.ConnectionActivity;
import org.owntracks.android.ui.preferences.connection.ConnectionMvvm;
import org.owntracks.android.ui.preferences.connection.ConnectionViewModel;

import dagger.Binds;
import dagger.Module;

@Module(includes = ActivityModule.class)
public abstract class ConnectionActivityModule {

    @Binds
    @PerActivity
    abstract AppCompatActivity appCompatActivity(ConnectionActivity a);

    @Binds abstract ConnectionMvvm.ViewModel bindViewModel(ConnectionViewModel viewModel);
}