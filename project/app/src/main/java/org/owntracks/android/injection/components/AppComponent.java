package org.owntracks.android.injection.components;

import android.content.Context;
import android.content.res.Resources;

import org.greenrobot.eventbus.EventBus;
import org.owntracks.android.data.repos.ContactsRepo;
import org.owntracks.android.injection.modules.AppModule;
import org.owntracks.android.injection.modules.DataModule;
import org.owntracks.android.injection.modules.NetModule;
import org.owntracks.android.injection.qualifier.AppContext;
import org.owntracks.android.injection.scopes.PerApplication;

import dagger.Component;
import io.realm.Realm;

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
@PerApplication
@Component(modules={AppModule.class, NetModule.class, DataModule.class})
public interface AppComponent {
    @AppContext Context context();
    Resources resources();

    Realm realm();
    ContactsRepo contactsRepo();
    EventBus eventBus();

}
