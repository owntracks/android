package org.owntracks.android.ui.map;

import android.content.Context;
import android.databinding.Bindable;
import android.os.Bundle;
import android.support.annotation.NonNull;

import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;
import org.owntracks.android.App;
import org.owntracks.android.BR;
import org.owntracks.android.data.repos.ContactsRepo;
import org.owntracks.android.injection.qualifier.AppContext;
import org.owntracks.android.injection.scopes.PerActivity;
import org.owntracks.android.messages.MessageClear;
import org.owntracks.android.model.FusedContact;
import org.owntracks.android.support.Events;
import org.owntracks.android.ui.base.viewmodel.BaseViewModel;

import java.util.List;

import javax.inject.Inject;

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

@PerActivity
public class MapViewModel extends BaseViewModel<MapMvvm.View> implements MapMvvm.ViewModel<MapMvvm.View> {
    private final ContactsRepo contactsRepo;
    private FusedContact activeContact;


    @Inject
    public MapViewModel(ContactsRepo contactsRepo) {
        Timber.v("onCreate");
        this.contactsRepo = contactsRepo;
    }

    @Override
    public void saveInstanceState(@NonNull Bundle outState) {
    }

    @Override
    public void restoreInstanceState(@NonNull Bundle savedInstanceState) {
    }

    @Override
    @Bindable
    public FusedContact getContact() {
        return activeContact;
    }

    @Override
    public List<FusedContact> getContacts() {
        return this.contactsRepo.getAll();
    }

    @Override
    public long getContactsRevision() {
        return contactsRepo.getRevision();
    }


    @Override
    public void restore(@NonNull String contactId) {
        Timber.v("restoring contact id:%s", contactId);
        activateContact(contactId, true);
    }

    @Override
    public void onMarkerClick(@NonNull String contactId) {
        activateContact(contactId, false);
    }


    @Override
    public void onMapClick() {
        clearContact();
    }

    @Override
    public void onBottomSheetLongClick() {
        getView().setModeContact(true);
    }


    private void activateContact(@NonNull String contactId, boolean center) {
        activeContact = contactsRepo.getById(contactId);
        if(activeContact == null) {
            Timber.e("contact %s could not be loaded from repo", contactId);
            getView().setModeDevice();
            return;
        }

        Timber.v("contactId:%s, obj:%s ", contactId, activeContact);

        notifyPropertyChanged(BR.contact);
        getView().setBottomSheetCollapsed();
        getView().setModeContact(center);

    }

    private void clearContact() {
        activeContact = null;
        notifyPropertyChanged(BR.contact);
        getView().setBottomSheetHidden();
    }


    private void clearMap() {
        getView().clearMarker();
    }

    @Override
    public void onBottomSheetClick() {
        getView().setBottomSheetExpanded();
    }

    @Override
    public void onMenuCenterDeviceClicked() {
        getView().setModeDevice();
    }

    @Override
    public void onClearContactClicked() {
        MessageClear m = new MessageClear();
        m.setTopic(activeContact.getId());
        App.getMessageProcessor().sendMessage(m);
    }


    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEvent(FusedContact c) {

        if(c != activeContact )
            getView().contactUpdate(c);
        else if(!c.isDeleted())
            getView().contactUpdateActive();
        else {
            if(c == activeContact) {
                clearContact();
                getView().setModeFree();
            }
            getView().contactRemove(c);
        }

    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEvent(Events.ModeChanged e) {
        clearMap();
        clearContact();
    }
}
