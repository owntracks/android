package org.owntracks.android.ui.contacts

import androidx.databinding.ViewDataBinding
import org.owntracks.android.BR
import org.owntracks.android.R
import org.owntracks.android.model.FusedContact
import org.owntracks.android.ui.base.BaseRecyclerViewAdapterWithClickHandler
import org.owntracks.android.ui.base.BaseRecyclerViewHolder

class ContactsAdapter(clickListener: ClickListener<FusedContact>) :
        BaseRecyclerViewAdapterWithClickHandler<FusedContact, ContactsAdapter.FusedContactViewHolder>(
                clickListener,
                ::FusedContactViewHolder,
                R.layout.ui_row_contact
        ) {

    class FusedContactViewHolder(binding: ViewDataBinding) :
            BaseRecyclerViewHolder<FusedContact>(binding, BR.contact)
}