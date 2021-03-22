package org.owntracks.android.ui.contacts

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.databinding.ViewDataBinding
import androidx.recyclerview.widget.RecyclerView
import org.owntracks.android.BR
import org.owntracks.android.R
import org.owntracks.android.model.FusedContact
import org.owntracks.android.support.ContactImageProvider
import org.owntracks.android.ui.base.BaseAdapter
import java.util.*

internal class ContactsAdapter(private val clickListener: BaseAdapter.ClickListener<FusedContact?>, val contactImageProvider: ContactImageProvider) : RecyclerView.Adapter<FusedContactViewHolder>() {
    private lateinit var contactList: List<FusedContact>
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FusedContactViewHolder {
        val binding = DataBindingUtil.inflate<ViewDataBinding>(LayoutInflater.from(parent.context), R.layout.ui_row_contact, parent, false,contactImageProvider)
        return FusedContactViewHolder(binding)
    }

    override fun onBindViewHolder(holder: FusedContactViewHolder, position: Int) {
        holder.bind(contactList[position], clickListener)
    }

    override fun getItemCount(): Int {
        return contactList.size
    }

    fun setContactList(contacts: Collection<FusedContact>) {
        contactList = ArrayList(contacts)
        notifyDataSetChanged()
    }
}

internal class FusedContactViewHolder(private val binding: ViewDataBinding) : RecyclerView.ViewHolder(binding.root) {
    fun bind(fusedContact: FusedContact?, clickListener: BaseAdapter.ClickListener<FusedContact?>) {
        binding.setVariable(BR.contact, fusedContact)
        binding.root.setOnClickListener { clickListener.onClick(fusedContact!!, binding.root, false) }
        binding.executePendingBindings()
    }
}