package org.owntracks.android.ui.contacts;

import android.databinding.ObservableList;
import android.view.View;

import org.jetbrains.annotations.NotNull;
import org.owntracks.android.BR;
import org.owntracks.android.R;
import org.owntracks.android.model.FusedContact;
import org.owntracks.android.ui.base.BaseAdapter;
import org.owntracks.android.ui.base.BaseAdapterItemView;


class ContactsAdapter extends BaseAdapter<FusedContact> {
    ContactsAdapter(ObservableList items, ClickListener clickListener) {
        super(BaseAdapterItemView.of(BR.contact, R.layout.ui_row_contact));
        setItems(items);
        setClickListener(clickListener);
    }

    interface ClickListener extends BaseAdapter.ClickListener<FusedContact> {
        void onClick(@NotNull FusedContact object , @NotNull View view, boolean longClick);
    }

}
