package org.owntracks.android.injection.modules;


import android.app.Service;
import android.support.v7.app.AppCompatActivity;

import org.owntracks.android.injection.qualifier.ActivityContext;
import org.owntracks.android.injection.qualifier.AppContext;
import org.owntracks.android.injection.qualifier.ServiceContext;
import org.owntracks.android.injection.scopes.PerActivity;
import org.owntracks.android.injection.scopes.PerApplication;
import org.owntracks.android.services.BackgroundService;

import dagger.Module;
import dagger.Provides;
import dagger.android.ContributesAndroidInjector;

@Module
public abstract class ServiceModule {
    @Provides
    @PerActivity
    @ServiceContext
    static Service serviceContext(Service service) {
        return service;
    }

}
