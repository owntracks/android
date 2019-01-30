package org.owntracks.android.injection.modules.android.ActivityModules;

import androidx.appcompat.app.AppCompatActivity;

import org.owntracks.android.injection.scopes.PerActivity;
import org.owntracks.android.ui.preferences.connection.ConnectionActivity;
import org.owntracks.android.ui.preferences.connection.ConnectionMvvm;
import org.owntracks.android.ui.preferences.connection.ConnectionViewModel;

import dagger.Binds;
import dagger.Module;

@Module(includes = BaseActivityModule.class)
public abstract class ConnectionActivityModule {

    @Binds
    @PerActivity
    abstract AppCompatActivity bindActivity(ConnectionActivity a);

    @Binds abstract ConnectionMvvm.ViewModel bindViewModel(ConnectionViewModel viewModel);
}