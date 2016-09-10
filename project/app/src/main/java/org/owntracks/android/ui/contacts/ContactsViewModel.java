package org.owntracks.android.ui.contacts;

import android.content.Context;
import android.databinding.ObservableList;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import org.owntracks.android.BR;
import org.owntracks.android.R;
import org.owntracks.android.data.repos.ContactsRepo;
import org.owntracks.android.injection.qualifier.AppContext;
import org.owntracks.android.injection.scopes.PerActivity;
import org.owntracks.android.model.FusedContact;
import org.owntracks.android.ui.base.viewmodel.BaseViewModel;
import org.owntracks.android.ui.map.MapActivity;

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
    public ObservableList<FusedContact> getRecyclerItems() {
        return contactsRepo.getAllAsList();
    }

    @Override
    public void onContactClick(FusedContact c) {
        Bundle b = new Bundle();
       // b.putString(MapActivity.BUNDLE_KEY_CONTACT_ID, );
       // navigator.get().startActivity(MapActivity.class, );


    }

    @Override
    public void onContactLongClick(FusedContact cast) {

    }
}
