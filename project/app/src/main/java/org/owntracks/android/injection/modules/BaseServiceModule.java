package org.owntracks.android.injection.modules;

import android.app.Service;
import android.content.Context;
import android.support.v4.app.FragmentManager;
import android.support.v7.app.AppCompatActivity;

import org.owntracks.android.injection.qualifier.ActivityContext;
import org.owntracks.android.injection.qualifier.ServiceContext;
import org.owntracks.android.injection.scopes.PerActivity;
import org.owntracks.android.injection.scopes.PerService;
import org.owntracks.android.support.DrawerProvider;
import org.owntracks.android.support.Preferences;
import org.owntracks.android.support.RequirementsChecker;
import org.owntracks.android.ui.base.navigator.Navigator;

import javax.inject.Named;

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
