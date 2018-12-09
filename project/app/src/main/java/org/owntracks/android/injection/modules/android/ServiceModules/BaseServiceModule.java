package org.owntracks.android.injection.modules.android.ServiceModules;

import android.app.Service;
import android.content.Context;

import org.owntracks.android.injection.qualifier.ServiceContext;
import org.owntracks.android.injection.scopes.PerService;

import dagger.Module;
import dagger.Provides;

@Module
public abstract class BaseServiceModule {

    @Provides
    @PerService
    @ServiceContext
    static Context serviceContext(Service service) {
        return service;
    }

}
