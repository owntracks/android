package org.owntracks.android.model;

import android.databinding.ObservableArrayList;
import android.databinding.ObservableList;

import org.owntracks.android.R;
import org.owntracks.android.BR;


import me.tatarka.bindingcollectionadapter.ItemView;


public class ContactsViewModel{

    public final ObservableList<FusedContact> items = new ObservableArrayList<>();
    public final ItemView itemView = ItemView.of(BR.item, R.layout.row_contact_binding);

}
