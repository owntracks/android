package org.owntracks.android.injection.components;

import org.owntracks.android.injection.scopes.PerActivity;

import dagger.Component;

@PerActivity
@Component(dependencies = AppComponent.class)
public interface MessageProcessorComponent {
    void inject(org.owntracks.android.services.MessageProcessorEndpointHttp endpoint);
    void inject(org.owntracks.android.services.MessageProcessorEndpointMqtt endpoint);
}
