package org.owntracks.android.ui.contacts

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.databinding.ViewDataBinding
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.CoroutineScope
import org.owntracks.android.BR
import org.owntracks.android.R
import org.owntracks.android.model.FusedContact
import org.owntracks.android.ui.base.BaseAdapter

internal class ContactsAdapter(
    private val clickListener: BaseAdapter.ClickListener<FusedContact?>,
    private val coroutineScope: CoroutineScope
) :
    RecyclerView.Adapter<ContactsAdapter.FusedContactViewHolder>() {
    private lateinit var contactList: List<FusedContact>
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FusedContactViewHolder {
        val binding = DataBindingUtil.inflate<ViewDataBinding>(
            LayoutInflater.from(parent.context),
            R.layout.ui_row_contact,
            parent,
            false
        )
        return FusedContactViewHolder(binding, coroutineScope)
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

    class FusedContactViewHolder(
        private val binding: ViewDataBinding,
        private val coroutineScope: CoroutineScope
    ) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(
            fusedContact: FusedContact?,
            clickListener: BaseAdapter.ClickListener<FusedContact?>
        ) {
            fusedContact?.run {
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
