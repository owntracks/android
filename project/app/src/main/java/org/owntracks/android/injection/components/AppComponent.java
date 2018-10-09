package org.owntracks.android.injection.components;

import android.app.Application;
import android.content.Context;
import android.content.res.Resources;

import org.greenrobot.eventbus.EventBus;
import org.owntracks.android.App;
import org.owntracks.android.data.repos.ContactsRepo;
import org.owntracks.android.data.repos.LocationRepo;
import org.owntracks.android.data.repos.WaypointsRepo;
import org.owntracks.android.injection.modules.AppModule;
import org.owntracks.android.injection.modules.DataModule;
import org.owntracks.android.injection.modules.NetModule;
import org.owntracks.android.injection.qualifier.AppContext;
import org.owntracks.android.injection.scopes.PerApplication;
import org.owntracks.android.services.MessageProcessor;
import org.owntracks.android.services.worker.Scheduler;
import org.owntracks.android.support.ContactImageProvider;
import org.owntracks.android.support.GeocodingProvider;
import org.owntracks.android.support.Parser;
import org.owntracks.android.support.Preferences;
import org.owntracks.android.support.Runner;

import javax.inject.Inject;

import dagger.BindsInstance;
import dagger.Component;
import dagger.android.AndroidInjector;

@PerApplication
@Component(modules={AppModule.class, NetModule.class, DataModule.class})
public interface AppComponent extends AndroidInjector<App>  {
    @Component.Builder
    abstract class Builder extends AndroidInjector.Builder<App> {
        @Override
        public AndroidInjector<App> build() {
            return null;
        }
    }


    @AppContext Context context();
    Resources resources();

    ContactsRepo contactsRepo();
    WaypointsRepo waypointsRepo();
    LocationRepo locationRepo();
    EventBus eventBus();
    Scheduler scheduler();
    Parser parser();
    MessageProcessor messageProcessor();
    ContactImageProvider contactImageProvider();
    GeocodingProvider geocodingProvider();
    Preferences preferences();
    Runner runner();
}
