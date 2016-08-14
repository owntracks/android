package org.owntracks.android.ui.contacts;

import android.content.Context;

import org.owntracks.android.data.repos.ContactsRepo;
import org.owntracks.android.injection.qualifier.AppContext;
import org.owntracks.android.injection.scopes.PerActivity;
import org.owntracks.android.ui.base.view.MvvmView;
import org.owntracks.android.ui.base.viewmodel.BaseViewModel;

import javax.inject.Inject;

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

@PerActivity
public class ContactsViewModel<V extends MvvmView> extends BaseViewModel<V> implements ContactsMvvm.ViewModel<V> {


    @Inject
    public ContactsViewModel(@AppContext Context context) {

    }

    @Override
    public String getFusedName() {
        return null;
    }

    @Override
    public String getFusedLocation() {
        return null;
    }
}
