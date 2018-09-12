package org.owntracks.android.injection.components;

import org.owntracks.android.injection.scopes.PerActivity;
import org.owntracks.android.services.worker.Scheduler;

import dagger.Component;

@PerActivity
@Component(dependencies = AppComponent.class)
public interface ServiceComponent {
    void inject(org.owntracks.android.services.BackgroundService service);
    void inject(Scheduler scheduler);
    void inject(org.owntracks.android.services.MessageProcessorEndpointHttp endpoint);
    void inject(org.owntracks.android.services.MessageProcessorEndpointMqtt endpoint);


}
