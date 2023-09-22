package org.owntracks.android.ui.contacts

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.databinding.ViewDataBinding
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.SortedList
import kotlinx.coroutines.CoroutineScope
import org.owntracks.android.BR
import org.owntracks.android.R
import org.owntracks.android.model.Contact

internal class ContactsAdapter(
    private val clickListener: AdapterClickListener<Contact>,
    private val coroutineScope: CoroutineScope
) :
    RecyclerView.Adapter<ContactsAdapter.ContactViewHolder>() {

    private val sortedListCallback = object : SortedList.Callback<Contact>() {
        override fun compare(o1: Contact, o2: Contact): Int = o2.tst.compareTo(o1.tst)

        override fun onInserted(position: Int, count: Int) { }

        override fun onRemoved(position: Int, count: Int) { }

        override fun onMoved(fromPosition: Int, toPosition: Int) { }

        override fun onChanged(position: Int, count: Int) { }

        override fun areItemsTheSame(item1: Contact, item2: Contact): Boolean = (item1.id == item2.id)

        override fun areContentsTheSame(oldItem: Contact, newItem: Contact): Boolean = (oldItem == newItem)
    }

    private val contactList: SortedList<Contact> = SortedList(
        Contact::class.java,
        sortedListCallback
    )

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ContactViewHolder {
        val binding = DataBindingUtil.inflate<ViewDataBinding>(
            LayoutInflater.from(parent.context),
            R.layout.ui_row_contact,
            parent,
            false
        )
        return ContactViewHolder(binding, coroutineScope)
    }

    override fun onBindViewHolder(holder: ContactViewHolder, position: Int) {
        holder.bind(contactList[position], clickListener)
    }

    override fun getItemCount(): Int {
        return contactList.size()
    }

    fun setContactList(contacts: Collection<Contact>) {
        contactList.run {
            beginBatchedUpdates()
            clear()
            addAll(contacts)
            endBatchedUpdates()
        }
    }

    fun addContact(contact: Contact) {
        contactList.add(contact)
        val index = contactList.indexOf(contact)
        notifyItemInserted(index)
    }

    fun removeContact(contact: Contact) {
        contactList.remove(contact)
    }

    fun updateContact(contact: Contact) {
        contactList.indexOf(contact).run { contactList.updateItemAt(this, contact) }
    }

    fun clearAll() {
        contactList.clear()
    }

    class ContactViewHolder(
        private val binding: ViewDataBinding,
        private val coroutineScope: CoroutineScope
    ) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(
            contact: Contact,
            clickListener: AdapterClickListener<Contact>
        ) {
            contact.run {
                binding.setVariable(BR.contact, this)
                binding.setVariable(BR.coroutineScope, coroutineScope)
                binding.root.setOnClickListener {
                    clickListener.onClick(
                        this,
                        binding.root,
                        false
                    )
                }
            }
            binding.executePendingBindings()
        }
    }
}
