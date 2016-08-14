package org.owntracks.android.injection.modules;


import org.owntracks.android.injection.scopes.PerApplication;

import dagger.Module;
import dagger.Provides;
import okhttp3.OkHttpClient;

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
public class NetModule {

    //@Provides
    //@PerApplication
    //static Gson provideGson() {
    //    return new GsonBuilder()
    //            // Custom type adapters for models are not needed when using Gson, but this
    //            // type adapter is a good example if you want to write one yourself.
    //            .registerTypeAdapter(Country.class, CountryTypeAdapter.INSTANCE)
    //            // These type adapters for RealmLists are needed, since RealmString and RealmStringMapEntry
    //            // wrappers are not recognized by Gson in the default configuration.
    //            .registerTypeAdapter(new TypeToken<RealmList<RealmString>>(){}.getType(), RealmStringListTypeAdapter.INSTANCE)
    //            .registerTypeAdapter(new TypeToken<RealmList<RealmStringMapEntry>>(){}.getType(), RealmStringMapEntryListTypeAdapter.INSTANCE)
    //            .create();
    //}

    @Provides
    @PerApplication
    static OkHttpClient provideOkHttpClient() {
        return new OkHttpClient();
    }


}
