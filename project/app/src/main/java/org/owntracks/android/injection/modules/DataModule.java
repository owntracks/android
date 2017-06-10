package org.owntracks.android.injection.modules;

import android.content.Context;

import org.owntracks.android.data.repos.ContactsRepo;
import org.owntracks.android.data.repos.MemoryContactsRepo;
import org.owntracks.android.db.Dao;
import org.owntracks.android.injection.qualifier.AppContext;
import org.owntracks.android.injection.scopes.PerApplication;
import org.owntracks.android.services.Scheduler;
import org.owntracks.android.support.EncryptionProvider;
import org.owntracks.android.support.Parser;
import org.owntracks.android.support.Preferences;

import dagger.Binds;
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
public abstract class DataModule {

    @Binds
    abstract ContactsRepo bindContactsRepo(MemoryContactsRepo memoryContactsRepo);

    @Provides
    @PerApplication
    static Dao provideDao(@AppContext Context context, Preferences preferences) {
        return new Dao(context, preferences);
    }

}
