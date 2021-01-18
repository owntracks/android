package org.owntracks.android.ui.contacts;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.MutableLiveData;

import org.owntracks.android.data.repos.ContactsRepo;
import org.owntracks.android.injection.scopes.PerActivity;
import org.owntracks.android.model.FusedContact;
import org.owntracks.android.ui.base.viewmodel.BaseViewModel;

import java.util.Map;

import javax.inject.Inject;


@PerActivity
public class ContactsViewModel extends BaseViewModel<ContactsMvvm.View> implements ContactsMvvm.ViewModel<ContactsMvvm.View> {

    private final ContactsRepo contactsRepo;

    @Inject
    public ContactsViewModel(ContactsRepo contactsRepo) {
        this.contactsRepo = contactsRepo;
    }

    public void attachView(@Nullable Bundle savedInstanceState, @NonNull ContactsMvvm.View view) {
        super.attachView(savedInstanceState, view);
    }

    @Override
    public MutableLiveData<Map<String, FusedContact>> getContacts() {
        return contactsRepo.getAll();
    }
}
