package org.owntracks.android.injection.modules;

import android.app.Application;
import android.content.Context;
import android.content.res.Resources;
import android.os.Build;

import org.greenrobot.eventbus.EventBus;
import org.owntracks.android.App;
import org.owntracks.android.data.repos.ContactsRepo;
import org.owntracks.android.data.repos.WaypointsRepo;
import org.owntracks.android.injection.modules.ActivityModules.ConnectionActivityModule;
import org.owntracks.android.injection.modules.ActivityModules.ContactsActivityModule;
import org.owntracks.android.injection.modules.ActivityModules.EditorActivityModule;
import org.owntracks.android.injection.modules.ActivityModules.LoadActivityModule;
import org.owntracks.android.injection.modules.ActivityModules.MapActivityModule;
import org.owntracks.android.injection.modules.ActivityModules.PreferencesActivityModule;
import org.owntracks.android.injection.modules.ActivityModules.RegionActivityModule;
import org.owntracks.android.injection.modules.ActivityModules.RegionsActivityModule;
import org.owntracks.android.injection.modules.ActivityModules.StatusActivityModule;
import org.owntracks.android.injection.modules.ActivityModules.WelcomeActivityModule;
import org.owntracks.android.injection.modules.FragmentModules.PlayFragmentModule;
import org.owntracks.android.injection.qualifier.AppContext;
import org.owntracks.android.injection.scopes.PerActivity;
import org.owntracks.android.injection.scopes.PerApplication;
import org.owntracks.android.injection.scopes.PerFragment;
import org.owntracks.android.services.MessageProcessor;
import org.owntracks.android.services.worker.Scheduler;
import org.owntracks.android.support.ContactImageProvider;
import org.owntracks.android.support.EncryptionProvider;
import org.owntracks.android.support.GeocodingProvider;
import org.owntracks.android.support.Parser;
import org.owntracks.android.support.Preferences;
import org.owntracks.android.support.Runner;
import org.owntracks.android.ui.region.RegionActivity;
import org.owntracks.android.ui.region.RegionViewModel;

import java.util.Locale;

import javax.inject.Singleton;

import dagger.Binds;
import dagger.Module;
import dagger.Provides;
import dagger.android.AndroidInjectionModule;
import dagger.android.ContributesAndroidInjector;
import dagger.android.support.AndroidSupportInjectionModule;

@Module(includes = AndroidSupportInjectionModule.class)
public abstract class AppModule {
    @Provides
    @PerApplication
    @AppContext
    static Context provideContext(App application) {
        return application.getApplicationContext();
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
    static Scheduler provideScheduler() {
        return new Scheduler(); // Needs to have zero argument constructor
    }

    @Provides
    @PerApplication
    static MessageProcessor provideMessageProcessor(EventBus eventBus, ContactsRepo repo, Preferences preferences, WaypointsRepo waypointsRepo) {
        return new MessageProcessor(eventBus, repo, preferences, waypointsRepo);
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
    static ContactImageProvider provideContactImageProvider(EventBus eventBus) { return new ContactImageProvider(eventBus); }

    @Provides
    @PerApplication
    static Runner provideRunner(@AppContext Context context) { return new Runner(context); }

    @PerActivity
    @ContributesAndroidInjector(modules = {ContactsActivityModule.class})
    abstract org.owntracks.android.ui.contacts.ContactsActivity bindContactsActivity();

    @PerActivity
    @ContributesAndroidInjector(modules = {MapActivityModule.class})
    abstract org.owntracks.android.ui.map.MapActivity bindMapActivity();

    @PerActivity
    @ContributesAndroidInjector(modules = {PreferencesActivityModule.class})
    abstract org.owntracks.android.ui.preferences.PreferencesActivity bindPreferencesActivity();

    @PerActivity
    @ContributesAndroidInjector(modules = {ConnectionActivityModule.class})
    abstract org.owntracks.android.ui.preferences.connection.ConnectionActivity bindConnectionActivity();

    @PerActivity
    @ContributesAndroidInjector(modules = {EditorActivityModule.class})
    abstract org.owntracks.android.ui.preferences.editor.EditorActivity bindEditorActivity();

    @PerActivity
    @ContributesAndroidInjector(modules = {LoadActivityModule.class})
    abstract org.owntracks.android.ui.preferences.load.LoadActivity bindLoadActivity();

    @PerActivity
    @ContributesAndroidInjector(modules = {StatusActivityModule.class})
    abstract org.owntracks.android.ui.status.StatusActivity bindStatusActivity();

    @PerActivity
    @ContributesAndroidInjector(modules = {WelcomeActivityModule.class})
    abstract org.owntracks.android.ui.welcome.WelcomeActivity bindWelcomeActivity();

    @PerActivity
    @ContributesAndroidInjector(modules = {RegionsActivityModule.class})
    abstract org.owntracks.android.ui.regions.RegionsActivity bindRegionsActivity();

    @PerActivity
    @ContributesAndroidInjector(modules = {RegionActivityModule.class})
    abstract org.owntracks.android.ui.region.RegionActivity bindRegionActivity();


}
