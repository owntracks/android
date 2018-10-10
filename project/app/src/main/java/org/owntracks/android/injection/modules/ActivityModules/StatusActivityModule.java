package org.owntracks.android.injection.modules.ActivityModules;

import android.support.v7.app.AppCompatActivity;

import org.owntracks.android.injection.modules.ActivityModule;
import org.owntracks.android.injection.qualifier.ActivityContext;
import org.owntracks.android.injection.scopes.PerActivity;
import org.owntracks.android.ui.map.MapMvvm;
import org.owntracks.android.ui.status.StatusActivity;
import org.owntracks.android.ui.status.StatusMvvm;
import org.owntracks.android.ui.status.StatusViewModel;

import dagger.Binds;
import dagger.Module;


@Module(includes = ActivityModule.class)
public abstract class StatusActivityModule {

    @Binds
    @PerActivity
    abstract AppCompatActivity bindActivity(StatusActivity a);

    @Binds abstract StatusMvvm.ViewModel bindViewModel(StatusViewModel viewModel);
}