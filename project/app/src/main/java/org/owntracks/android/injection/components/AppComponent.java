package org.owntracks.android.injection.components;

import android.content.Context;
import android.content.res.Resources;

import org.greenrobot.eventbus.EventBus;
import org.owntracks.android.App;
import org.owntracks.android.data.repos.ContactsRepo;
import org.owntracks.android.data.repos.LocationRepo;
import org.owntracks.android.data.repos.WaypointsRepo;
import org.owntracks.android.injection.modules.android.AndroindBindingModule;
import org.owntracks.android.injection.modules.AppModule;
import org.owntracks.android.injection.qualifier.AppContext;
import org.owntracks.android.injection.scopes.PerApplication;
import org.owntracks.android.services.MessageProcessor;
import org.owntracks.android.services.worker.MQTTKeepaliveWorker;
import org.owntracks.android.services.worker.MQTTReconnectWorker;
import org.owntracks.android.support.ContactImageProvider;
import org.owntracks.android.support.GeocodingProvider;
import org.owntracks.android.support.Parser;
import org.owntracks.android.support.Preferences;
import org.owntracks.android.support.Runner;

import dagger.BindsInstance;
import dagger.Component;
import dagger.android.AndroidInjector;
import dagger.android.support.AndroidSupportInjectionModule;
import dagger.android.support.DaggerApplication;

@PerApplication
@Component(modules={AppModule.class, AndroidSupportInjectionModule.class, AndroindBindingModule.class})
public interface AppComponent extends AndroidInjector<DaggerApplication>  {
    @Override
    void inject(DaggerApplication instance);

    void inject(App app);

    @Component.Builder
    interface Builder {
        @BindsInstance
        Builder app(App app);

        AppComponent build();
    }

    @AppContext Context context();
    Resources resources();

    ContactsRepo contactsRepo();
    WaypointsRepo waypointsRepo();
    LocationRepo locationRepo();
    EventBus eventBus();
    Parser parser();
    MessageProcessor messageProcessor();
    ContactImageProvider contactImageProvider();
    GeocodingProvider geocodingProvider();
    Preferences preferences();
    Runner runner();

    void inject(MQTTKeepaliveWorker syncWorker);
    void inject(MQTTReconnectWorker syncWorker);

}
