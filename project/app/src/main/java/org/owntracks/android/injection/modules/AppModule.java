package org.owntracks.android.injection.modules;

import android.app.Application;
import android.content.Context;
import android.content.res.Resources;

import org.greenrobot.eventbus.EventBus;
import org.owntracks.android.injection.qualifier.AppContext;
import org.owntracks.android.injection.scopes.PerApplication;
import org.owntracks.android.services.MessageProcessor;
import org.owntracks.android.services.Scheduler;

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
    EventBus provideEventbus() {
        return EventBus.builder().addIndex(new org.owntracks.android.EventBusIndex()).sendNoSubscriberEvent(false).logNoSubscriberMessages(false).build();
    }

    @Provides
    @PerApplication
    Scheduler provideScheduler() {
        return new Scheduler();
    }

    @Provides
    @PerApplication
    MessageProcessor provideMessageProcessor() {
        return new MessageProcessor();
    }

}
