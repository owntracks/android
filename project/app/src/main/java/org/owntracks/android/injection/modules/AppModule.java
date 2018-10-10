package org.owntracks.android.injection.modules;

import android.app.Application;
import android.content.Context;
import android.content.res.Resources;
import android.os.Build;

import org.greenrobot.eventbus.EventBus;
import org.owntracks.android.App;
import org.owntracks.android.data.repos.ContactsRepo;
import org.owntracks.android.data.repos.WaypointsRepo;
import org.owntracks.android.injection.qualifier.AppContext;
import org.owntracks.android.injection.scopes.PerApplication;
import org.owntracks.android.services.MessageProcessor;
import org.owntracks.android.services.worker.Scheduler;
import org.owntracks.android.support.ContactImageProvider;
import org.owntracks.android.support.EncryptionProvider;
import org.owntracks.android.support.GeocodingProvider;
import org.owntracks.android.support.Parser;
import org.owntracks.android.support.Preferences;
import org.owntracks.android.support.Runner;

import java.util.Locale;
import dagger.Module;
import dagger.Provides;
import dagger.android.support.AndroidSupportInjectionModule;

@Module(includes = {DataModule.class})
public class AppModule {

    @Provides
    @AppContext
    @PerApplication
    Context provideContext(App app) {
        return app;
    }

    @Provides
    @PerApplication
    static Resources provideResources(@AppContext Context context) {
        return context.getResources();
    }

    @Provides
    @PerApplication
    static EventBus provideEventbus() {
        return EventBus.builder().addIndex(new org.owntracks.android.EventBusIndex()).sendNoSubscriberEvent(false).logNoSubscriberMessages(false).build();
    }

    @Provides
    @PerApplication
    static MessageProcessor provideMessageProcessor(EventBus eventBus, ContactsRepo repo, Preferences preferences, WaypointsRepo waypointsRepo, Parser parser, Scheduler scheduler) {
        return new MessageProcessor(eventBus, repo, preferences, waypointsRepo, parser, scheduler);
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
    static GeocodingProvider provideGeocodingProvider(@AppContext Context context, Preferences preferences) {
        return new GeocodingProvider(context, preferences);
    }

    @Provides
    @PerApplication
    static ContactImageProvider provideContactImageProvider(EventBus eventBus) { return new ContactImageProvider(eventBus); }

    @Provides
    @PerApplication
    static Runner provideRunner(@AppContext Context context) { return new Runner(context); }

    @ContributesAndroidInjector(modules = {ConnectionActivityModule.class})

}
