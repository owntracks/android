package org.owntracks.android.ui.contacts;

import android.os.Bundle;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;

import org.owntracks.android.R;
import org.owntracks.android.databinding.UiContactsBinding;
import org.owntracks.android.geocoding.GeocoderProvider;
import org.owntracks.android.model.FusedContact;
import org.owntracks.android.ui.base.BaseActivity;

import javax.inject.Inject;


public class ContactsActivity extends BaseActivity<UiContactsBinding, ContactsMvvm.ViewModel> implements ContactsMvvm.View, org.owntracks.android.ui.contacts.ContactsAdapter.ClickListener {
    @Inject
    ContactsAdapter contactsAdapter;

    @Inject
    GeocoderProvider geocoderProvider;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        bindAndAttachContentView(R.layout.ui_contacts, savedInstanceState);
        setSupportToolbar(binding.toolbar);
        setDrawer(binding.toolbar);
        binding.getVm().getContacts().observe(this, contacts -> {
            contactsAdapter.setContactList(contacts.values());
            for (FusedContact contact : contacts.values()) {
                contact.getMessageLocation().removeObservers(binding.getLifecycleOwner());
                contact.getMessageLocation().observe(binding.getLifecycleOwner(), messageLocation -> geocoderProvider.resolve(messageLocation));
            }
        });
        binding.recyclerView.setLayoutManager(new LinearLayoutManager(this));
        binding.recyclerView.setAdapter(contactsAdapter);
    }

    @Override
    public void onClick(@NonNull FusedContact object, @NonNull View view, boolean longClick) {
        viewModel.onContactClick(object);
    }


}
