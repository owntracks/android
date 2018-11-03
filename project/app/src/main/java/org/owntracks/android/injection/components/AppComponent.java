package org.owntracks.android.injection.components;

import org.owntracks.android.App;
import org.owntracks.android.injection.modules.ObjectboxWaypointsModule;
import org.owntracks.android.injection.modules.android.AndroindBindingModule;
import org.owntracks.android.injection.modules.AppModule;
import org.owntracks.android.injection.scopes.PerApplication;
import org.owntracks.android.services.worker.MQTTKeepaliveWorker;
import org.owntracks.android.services.worker.MQTTReconnectWorker;
import org.owntracks.android.services.worker.SendLocationPingWorker;

import androidx.work.Worker;
import dagger.BindsInstance;
import dagger.Component;
import dagger.android.AndroidInjector;
import dagger.android.support.AndroidSupportInjectionModule;
import dagger.android.support.DaggerApplication;

@PerApplication
@Component(modules={AppModule.class, ObjectboxWaypointsModule.class, AndroidSupportInjectionModule.class, AndroindBindingModule.class})
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
    void inject(MQTTKeepaliveWorker worker);
    void inject(MQTTReconnectWorker worker);
    void inject(SendLocationPingWorker worker);

}
