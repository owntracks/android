package org.owntracks.android.injection.modules;

import android.content.Context;
import android.content.res.Resources;
import android.os.Build;

import org.greenrobot.eventbus.EventBus;
import org.owntracks.android.App;
import org.owntracks.android.data.repos.ContactsRepo;
import org.owntracks.android.data.repos.LocationRepo;
import org.owntracks.android.data.repos.MemoryContactsRepo;
import org.owntracks.android.data.repos.ObjectboxWaypointsRepo;
import org.owntracks.android.data.repos.WaypointsRepo;
import org.owntracks.android.injection.qualifier.AppContext;
import org.owntracks.android.injection.scopes.PerApplication;
import org.owntracks.android.support.ContactImageProvider;
import org.owntracks.android.support.Preferences;
import org.owntracks.android.support.Runner;

import java.util.Locale;
import dagger.Module;
import dagger.Provides;

@Module
public class AppModule {

    @Provides
    @AppContext
    @PerApplication
    Context provideContext(App app) {
        return app;
    }

    @Provides
    @PerApplication
    static EventBus provideEventbus() {
        return EventBus.builder().addIndex(new org.owntracks.android.EventBusIndex()).sendNoSubscriberEvent(false).logNoSubscriberMessages(false).build();
    }


    @Provides
    @PerApplication
    static ContactsRepo provideContactsRepo(EventBus eventBus, ContactImageProvider contactImageProvider) {
        return new MemoryContactsRepo(eventBus, contactImageProvider);
    }

    @Provides
    @PerApplication
    static WaypointsRepo provideWaypointsRepo(@AppContext Context context, EventBus eventBus, Preferences preferences) {
        return new ObjectboxWaypointsRepo(context, eventBus, preferences);
    }

    @Provides
    @PerApplication
    static LocationRepo provideLocationRepo(EventBus eventBus) { return new LocationRepo(eventBus); }
}
