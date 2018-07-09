package org.owntracks.android.ui.contacts;

import android.content.Context;
import android.databinding.ObservableArrayList;
import android.databinding.ObservableList;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;
import org.owntracks.android.data.repos.ContactsRepo;
import org.owntracks.android.injection.qualifier.AppContext;
import org.owntracks.android.injection.scopes.PerActivity;
import org.owntracks.android.model.FusedContact;
import org.owntracks.android.support.Events;
import org.owntracks.android.ui.base.viewmodel.BaseViewModel;
import org.owntracks.android.ui.map.MapActivity;

import java.util.Collection;
import java.util.List;
import java.util.TreeMap;

import javax.inject.Inject;


@PerActivity
public class ContactsViewModel extends BaseViewModel<ContactsMvvm.View> implements ContactsMvvm.ViewModel<ContactsMvvm.View> {

    private final ContactsRepo contactsRepo;

    @Inject
    public ContactsViewModel(@AppContext Context context, ContactsRepo contactsRepo) {
        this.contactsRepo = contactsRepo;
    }

    public void attachView(@NonNull ContactsMvvm.View view, @Nullable Bundle savedInstanceState) {
        super.attachView(view, savedInstanceState);
    }

    @Override
    public Collection<FusedContact> getContacts() {
        return contactsRepo.getAllAsList();
    }

    @Override
    public void onContactClick(FusedContact c) {
        if(!c.hasLocation())
            return;

        Bundle b = new Bundle();
        b.putString(MapActivity.BUNDLE_KEY_CONTACT_ID, c.getId());
        navigator.get().startActivity(MapActivity.class, b);
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEvent(Events.FusedContactAdded c) {
        //TODO: add, sort
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEvent(Events.FusedContactRemoved c) {
        //TODO: remove
    }
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEvent(FusedContact c) {
        //TODO: Sort
    }

}
