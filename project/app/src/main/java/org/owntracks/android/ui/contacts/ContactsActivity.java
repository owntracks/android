package org.owntracks.android.ui.contacts;

import android.databinding.ObservableArrayList;
import android.databinding.ObservableList;
import android.os.Bundle;
import android.support.annotation.MainThread;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.widget.LinearLayoutManager;
import android.view.View;

import org.owntracks.android.R;
import org.owntracks.android.databinding.UiContactsBinding;
import org.owntracks.android.model.FusedContact;
import org.owntracks.android.support.ContactsComparator;
import org.owntracks.android.ui.base.BaseActivity;

import java.util.Collections;


public class ContactsActivity extends BaseActivity<UiContactsBinding, ContactsMvvm.ViewModel> implements ContactsMvvm.View, org.owntracks.android.ui.contacts.ContactsAdapter.ClickListener {
    private ObservableList<FusedContact> mList;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mList = new ObservableArrayList<>();
        bindAndAttachContentView(R.layout.ui_contacts, savedInstanceState);
        setSupportToolbar(binding.toolbar);
        setDrawer(binding.toolbar);

        binding.recyclerView.setLayoutManager(new LinearLayoutManager(this));
        binding.recyclerView.setAdapter(new ContactsAdapter(mList, this));
    }

    @Override
    public void onClick(@NonNull FusedContact object, @NonNull View view, boolean longClick) {
        viewModel.onContactClick(object);
    }
    @Override
    public void onResume() {
        super.onResume();
        mList.clear();
        mList.addAll(viewModel.getContacts());
    }

    @Override
    public void removeContact(FusedContact c) {
        mList.remove(c);
    }

    @Override
    @MainThread
    public void addContact(FusedContact c) {
        mList.add(c);
        Collections.sort(mList);
    }

    @Override
    @MainThread
    public void updateContact(FusedContact c) {
        Collections.sort(mList);
    }
}
