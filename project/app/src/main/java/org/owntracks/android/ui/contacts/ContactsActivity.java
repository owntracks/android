package org.owntracks.android.ui.contacts;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.widget.LinearLayoutManager;
import android.view.View;

import org.owntracks.android.R;
import org.owntracks.android.databinding.UiContactsBinding;
import org.owntracks.android.model.FusedContact;
import org.owntracks.android.ui.base.BaseActivity;

import timber.log.Timber;


public class ContactsActivity extends BaseActivity<UiContactsBinding, ContactsMvvm.ViewModel> implements ContactsMvvm.View, org.owntracks.android.ui.contacts.ContactsAdapter.ClickListener {

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        Timber.v("onCreate");
        super.onCreate(savedInstanceState);
        activityComponent().inject(this);
        bindAndAttachContentView(R.layout.ui_contacts, savedInstanceState);

        setHasEventBus(false);
        setSupportToolbar(binding.toolbar);
        setDrawer(binding.toolbar);

        binding.recyclerView.setLayoutManager(new LinearLayoutManager(this));
        binding.recyclerView.setAdapter(new ContactsAdapter(viewModel.getContacts(), this));
    }



    @Override
    public void onClick(@NonNull FusedContact object, @NonNull View view, boolean longClick) {
        viewModel.onContactClick(object);
    }
}
