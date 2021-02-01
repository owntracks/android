package org.owntracks.android.injection.components;

import org.owntracks.android.App;
import org.owntracks.android.data.repos.ObjectboxWaypointsModule;
import org.owntracks.android.injection.modules.AppModule;
import org.owntracks.android.injection.modules.android.AndroindBindingModule;
import org.owntracks.android.services.worker.MQTTMaybeReconnectAndPingWorker;
import org.owntracks.android.services.worker.MQTTReconnectWorker;
import org.owntracks.android.services.worker.SendLocationPingWorker;
import org.owntracks.android.support.preferences.SharedPreferencesStoreModule;

import javax.inject.Singleton;

import dagger.BindsInstance;
import dagger.Component;
import dagger.android.AndroidInjector;
import dagger.android.support.AndroidSupportInjectionModule;
import dagger.android.support.DaggerApplication;

@Singleton
@Component(modules={
        AppModule.class,
        ObjectboxWaypointsModule.class,
        AndroidSupportInjectionModule.class,
        AndroindBindingModule.class,
        SharedPreferencesStoreModule.class}
        )
public interface AppComponent extends AndroidInjector<DaggerApplication>  {

    @Component.Builder
    interface Builder {
        @BindsInstance
        Builder app(App app);
        AppComponent build();

    }

    @Override
    void inject(DaggerApplication instance);
    void inject(App app);
    void inject(MQTTMaybeReconnectAndPingWorker worker);
    void inject(MQTTReconnectWorker worker);
    void inject(SendLocationPingWorker worker);

}
