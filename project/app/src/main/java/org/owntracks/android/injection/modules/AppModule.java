package org.owntracks.android.injection.modules;

import android.app.Application;
import android.content.Context;
import android.content.res.Resources;
import android.os.Build;

import org.greenrobot.eventbus.EventBus;
import org.owntracks.android.data.repos.ContactsRepo;
import org.owntracks.android.injection.qualifier.AppContext;
import org.owntracks.android.injection.scopes.PerApplication;
import org.owntracks.android.services.MessageProcessor;
import org.owntracks.android.services.Scheduler;
import org.owntracks.android.support.ContactImageProvider;
import org.owntracks.android.support.EncryptionProvider;
import org.owntracks.android.support.GeocodingProvider;
import org.owntracks.android.support.NotificationProvider;
import org.owntracks.android.support.Parser;
import org.owntracks.android.support.Preferences;

import java.util.Locale;

import dagger.Module;
import dagger.Provides;

/* Copyright 2016 Patrick Löwenstein
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License. */
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
    static MessageProcessor provideMessageProcessor(EventBus eventBus, ContactsRepo repo) {
        return new MessageProcessor(eventBus, repo);
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
    static EncryptionProvider provideEncryptionProvider() {
        return new EncryptionProvider();
    }


    @Provides
    @PerApplication
    static GeocodingProvider provideGeocodingProvider(@AppContext Context context) { return new GeocodingProvider(context); }

    @Provides
    @PerApplication
    static ContactImageProvider provideContactImageProvider(@AppContext Context context) { return new ContactImageProvider(context); }

    @Provides
    @PerApplication
    static Preferences providePreferences(@AppContext Context context) { return new Preferences(context); }

    @Provides
    @PerApplication
    static NotificationProvider provideNotificationProvider(@AppContext Context context, Resources resources, Locale locale) { return new NotificationProvider(context, resources, locale); }

}
