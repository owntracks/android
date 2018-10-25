package org.owntracks.android.injection.modules.android.ServiceModules;

import android.app.Service;

import org.owntracks.android.injection.scopes.PerActivity;
import org.owntracks.android.services.BackgroundService;

import dagger.Binds;
import dagger.Module;

@Module(includes = BaseServiceModule.class)
public abstract class BackgroundServiceModule {

    @Binds
    @PerActivity
    abstract Service bindService(BackgroundService s);

}
