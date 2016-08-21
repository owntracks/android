package org.owntracks.android.injection.modules;

import android.app.Application;
import android.content.Context;
import android.content.res.Resources;

import org.greenrobot.eventbus.EventBus;
import org.owntracks.android.App;
import org.owntracks.android.BuildConfig;
import org.owntracks.android.injection.qualifier.AppContext;
import org.owntracks.android.injection.scopes.PerApplication;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;
import io.realm.Realm;
import io.realm.RealmConfiguration;
import timber.log.Timber;

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
    EventBus provideEventbus() {
        Timber.v("loading bus with index");
        return EventBus.builder().addIndex(new org.owntracks.android.EventBusIndex()).build();
    }


    @Provides
    @PerApplication
    static RealmConfiguration provideRealmConfiguration(@AppContext Context context) {
        RealmConfiguration.Builder builder = new RealmConfiguration.Builder(context);
        if(BuildConfig.DEBUG) { builder = builder.deleteRealmIfMigrationNeeded(); }
        return builder.build();
    }

    @Provides
    static Realm provideRealm(RealmConfiguration realmConfiguration) {
        return Realm.getInstance(realmConfiguration);
    }

}
