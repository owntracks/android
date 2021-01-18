package org.owntracks.android.ui.contacts;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.databinding.DataBindingUtil;
import androidx.databinding.ViewDataBinding;
import androidx.recyclerview.widget.RecyclerView;

import org.owntracks.android.BR;
import org.owntracks.android.R;
import org.owntracks.android.model.FusedContact;
import org.owntracks.android.ui.base.BaseAdapter;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.inject.Inject;


class ContactsAdapter extends RecyclerView.Adapter<FusedContactViewHolder> {
    @Inject
    public ContactsAdapter() {
    }

    List<FusedContact> contactList;

    @NonNull
    @Override
    public FusedContactViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ViewDataBinding binding = DataBindingUtil.inflate(LayoutInflater.from(parent.getContext()), R.layout.ui_row_contact, parent, false);
        return new FusedContactViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull FusedContactViewHolder holder, int position) {
        holder.bind(contactList.get(position));

    }

    @Override
    public int getItemCount() {
        return contactList.size();
    }

    public void setContactList(Collection<FusedContact> contacts) {
        contactList=new ArrayList<>(contacts);
        this.notifyDataSetChanged();
    }


    interface ClickListener extends BaseAdapter.ClickListener<FusedContact> {
        void onClick(@NonNull FusedContact object, @NonNull View view, boolean longClick);
    }
}

class FusedContactViewHolder extends RecyclerView.ViewHolder {
    private final ViewDataBinding binding;

    public FusedContactViewHolder(ViewDataBinding binding) {
        super(binding.getRoot());
        this.binding = binding;
    }

    public void bind(FusedContact fusedContact) {
        binding.setVariable(BR.contact, fusedContact);
        binding.executePendingBindings();
    }
}