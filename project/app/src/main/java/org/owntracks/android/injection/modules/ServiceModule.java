package org.owntracks.android.injection.modules;


import android.app.Service;
import android.support.v7.app.AppCompatActivity;

import org.owntracks.android.injection.qualifier.AppContext;
import org.owntracks.android.injection.qualifier.ServiceContext;
import org.owntracks.android.injection.scopes.PerApplication;
import org.owntracks.android.services.BackgroundService;

import dagger.Module;

@Module
public class ServiceModule {
    @ServiceContext
    private final Service service;

    public ServiceModule(Service service) {
        this.service = service;
    }
}
