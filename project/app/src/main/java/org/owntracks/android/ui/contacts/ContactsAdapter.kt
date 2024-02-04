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
import timber.log.Timber

internal class ContactsAdapter(
    private val clickListener: AdapterClickListener<Contact>,
    private val coroutineScope: CoroutineScope
) :
    RecyclerView.Adapter<ContactsAdapter.ContactViewHolder>() {

    private val sortedListCallback = object : SortedList.Callback<Contact>() {
        override fun compare(o1: Contact, o2: Contact): Int = o2.locationTimestamp.compareTo(o1.locationTimestamp)

        override fun onInserted(position: Int, count: Int) { notifyItemRangeInserted(position, count) }

        override fun onRemoved(position: Int, count: Int) { notifyItemRangeRemoved(position, count) }

        override fun onMoved(fromPosition: Int, toPosition: Int) { notifyItemMoved(fromPosition, toPosition) }

        override fun onChanged(position: Int, count: Int) { notifyItemRangeChanged(position, count) }

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
            addAll(contacts).also { Timber.v("Added ${contacts.count()} contacts") }
            endBatchedUpdates()
        }
    }

    fun addContact(contact: Contact) {
        contactList.add(contact).also { Timber.v("Added contact: ${contact.id}") }
    }

    fun removeContact(contact: Contact) {
        contactList.remove(contact).also { Timber.v("Removing contact: ${contact.id}") }
    }

    fun updateContact(contact: Contact) {
        contactList.indexOf(contact).run {
            if (this == SortedList.INVALID_POSITION) {
                Timber.v("Attempted to update contact ${contact.id} but it was not found in the adapter")
                return
            }
            contactList.updateItemAt(this, contact).also { Timber.v("Updated contact: $it at index $this") }
        }
    }

    fun clearAll() {
        contactList.size().run {
            Timber.d("Clearing $this contacts from adapter")
            contactList.clear()
        }
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
