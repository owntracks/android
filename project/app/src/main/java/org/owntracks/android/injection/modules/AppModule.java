package org.owntracks.android.injection.modules;

import android.content.Context;

import org.greenrobot.eventbus.EventBus;
import org.owntracks.android.App;
import org.owntracks.android.data.repos.ContactsRepo;
import org.owntracks.android.data.repos.LocationRepo;
import org.owntracks.android.data.repos.MemoryContactsRepo;
import org.owntracks.android.injection.qualifier.AppContext;
import org.owntracks.android.injection.scopes.PerApplication;
import org.owntracks.android.support.ContactImageProvider;

import dagger.Module;
import dagger.Provides;

@Module
public class AppModule {

    @Provides
    @AppContext
    @PerApplication
    protected Context provideContext(App app) {
        return app;
    }

    @Provides
    @PerApplication
    protected EventBus provideEventbus() {
        return EventBus.builder().addIndex(new org.owntracks.android.EventBusIndex()).sendNoSubscriberEvent(false).logNoSubscriberMessages(false).build();
    }


    @Provides
    @PerApplication
    protected ContactsRepo provideContactsRepo(EventBus eventBus, ContactImageProvider contactImageProvider) {
        return new MemoryContactsRepo(eventBus, contactImageProvider);
    }

    @Provides
    @PerApplication
    protected LocationRepo provideLocationRepo(EventBus eventBus) { return new LocationRepo(eventBus); }
}

