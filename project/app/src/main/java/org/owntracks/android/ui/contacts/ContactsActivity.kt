package org.owntracks.android.ui.contacts

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import androidx.recyclerview.widget.LinearLayoutManager
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import org.owntracks.android.R
import org.owntracks.android.data.repos.ContactsRepoChange
import org.owntracks.android.databinding.UiContactsBinding
import org.owntracks.android.model.Contact
import org.owntracks.android.support.DrawerProvider
import org.owntracks.android.ui.map.MapActivity

@AndroidEntryPoint
class ContactsActivity :
    AppCompatActivity(),
    AdapterClickListener<Contact> {
    @Inject
    lateinit var drawerProvider: DrawerProvider

    private val vm: ContactsViewModel by viewModels()
    private lateinit var contactsAdapter: ContactsAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        contactsAdapter = ContactsAdapter(this, vm.coroutineScope)
        DataBindingUtil.setContentView<UiContactsBinding>(this, R.layout.ui_contacts)
            .apply {
                appbar.toolbar.run {
                    setSupportActionBar(this)
                    drawerProvider.attach(this)
                }
                contactsRecyclerView.run {
                    layoutManager = LinearLayoutManager(this@ContactsActivity)
                    adapter = contactsAdapter
                }
            }

        contactsAdapter.setContactList(vm.contacts.values)
        vm.contactUpdatedEvent.observe(this) {
            when (it) {
                is ContactsRepoChange.ContactAdded -> {
                    contactsAdapter.addContact(it.contact)
                    vm.refreshGeocode(it.contact)
                }
                is ContactsRepoChange.ContactRemoved -> contactsAdapter.removeContact(it.contact)
                is ContactsRepoChange.ContactLocationUpdated -> {
                    contactsAdapter.updateContact(it.contact)
                    vm.refreshGeocode(it.contact)
                }
                is ContactsRepoChange.ContactCardUpdated -> contactsAdapter.updateContact(it.contact)
                is ContactsRepoChange.AllCleared -> contactsAdapter.clearAll()
            }
        }
    }

    override fun onClick(item: Contact, view: View, longClick: Boolean) {
        startActivity(
            Intent(this, MapActivity::class.java).putExtra(
                "_args",
                Bundle().apply { putString(MapActivity.BUNDLE_KEY_CONTACT_ID, item.id) }
            )
        )
    }
}
