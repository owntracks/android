package org.owntracks.android.injection.modules;

import android.app.Application;
import android.content.Context;
import android.content.res.Resources;
import android.os.Build;

import org.greenrobot.eventbus.EventBus;
import org.owntracks.android.data.repos.ContactsRepo;
import org.owntracks.android.data.repos.WaypointsRepo;
import org.owntracks.android.injection.qualifier.AppContext;
import org.owntracks.android.injection.scopes.PerApplication;
import org.owntracks.android.services.MessageProcessor;
import org.owntracks.android.services.Scheduler;
import org.owntracks.android.support.ContactImageProvider;
import org.owntracks.android.support.EncryptionProvider;
import org.owntracks.android.support.GeocodingProvider;
import org.owntracks.android.support.Parser;
import org.owntracks.android.support.Preferences;
import org.owntracks.android.support.Runner;

import java.util.Locale;

import dagger.Module;
import dagger.Provides;

@Module
public class AppModule {

    private final Application mApp;

    public AppModule(Application app) {
        mApp = app;
    }

    @Provides
    @PerApplication
    @AppContext
    Context provideAppContext() {
        return mApp;
    }

    @Provides
    @PerApplication
    Resources provideResources() {
        return mApp.getResources();
    }

    @Provides
    @PerApplication
    static EventBus provideEventbus() {
        return EventBus.builder().addIndex(new org.owntracks.android.EventBusIndex()).sendNoSubscriberEvent(false).logNoSubscriberMessages(false).build();
    }


    @Provides
    @PerApplication
    static Scheduler provideScheduler() {
        return new Scheduler();
    }

    @Provides
    @PerApplication
    static MessageProcessor provideMessageProcessor(EventBus eventBus, ContactsRepo repo, Preferences preferences) {
        return new MessageProcessor(eventBus, repo, preferences);
    }

    @SuppressWarnings("deprecation")
    @Provides
    @PerApplication
    static Locale provideLocale(@AppContext Context context) {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.N ? context.getResources().getConfiguration().getLocales().get(0) : context.getResources().getConfiguration().locale;
    }


    @Provides
    @PerApplication
    static Parser provideParser(EncryptionProvider provider) {
        return new Parser(provider);
    }

    @Provides
    @PerApplication
    static EncryptionProvider provideEncryptionProvider(Preferences preferences) {
        return new EncryptionProvider(preferences);
    }

    @Provides
    @PerApplication
    static GeocodingProvider provideGeocodingProvider(Preferences preferences) {
        return new GeocodingProvider(preferences);
    }

    @Provides
    @PerApplication
    static ContactImageProvider provideContactImageProvider() { return new ContactImageProvider(); }

    @Provides
    @PerApplication
    static Preferences providePreferences(@AppContext Context context, WaypointsRepo waypointsRepo) { return new Preferences(context, waypointsRepo); }

    @Provides
    @PerApplication
    static Runner provideRunner(@AppContext Context context) { return new Runner(context); }
}
