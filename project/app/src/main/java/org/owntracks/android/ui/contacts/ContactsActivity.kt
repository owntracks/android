package org.owntracks.android.ui.contacts

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.activity.viewModels
import androidx.annotation.VisibleForTesting
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.test.espresso.idling.CountingIdlingResource
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import javax.inject.Named
import kotlinx.coroutines.launch
import org.owntracks.android.R
import org.owntracks.android.data.repos.ContactsRepoChange
import org.owntracks.android.databinding.UiContactsBinding
import org.owntracks.android.model.Contact
import org.owntracks.android.support.DrawerProvider
import org.owntracks.android.ui.map.MapActivity
import org.owntracks.android.ui.mixins.ServiceStarter
import timber.log.Timber

@AndroidEntryPoint
class ContactsActivity :
    AppCompatActivity(), AdapterClickListener<Contact>, ServiceStarter by ServiceStarter.Impl() {
  @Inject lateinit var drawerProvider: DrawerProvider

  @Inject
  @Named("contactsActivityIdlingResource")
  @VisibleForTesting
  lateinit var contactsCountingIdlingResource: CountingIdlingResource

  private val viewModel: ContactsViewModel by viewModels()
  private lateinit var contactsAdapter: ContactsAdapter

  override fun onCreate(savedInstanceState: Bundle?) {
    startService(this)
    super.onCreate(savedInstanceState)
    contactsAdapter = ContactsAdapter(this, viewModel.coroutineScope)
    val binding =
        DataBindingUtil.setContentView<UiContactsBinding>(this, R.layout.ui_contacts).apply {
          vm = viewModel
          appbar.toolbar.run {
            setSupportActionBar(this)
            drawerProvider.attach(this)
          }
          contactsRecyclerView.run {
            layoutManager = LinearLayoutManager(this@ContactsActivity)
            adapter = contactsAdapter
          }
        }

    contactsAdapter.setContactList(viewModel.contacts.values)

    // Trigger a geocode refresh on startup, because future refreshes will only be triggered on
    // update events
    viewModel.contacts.values.forEach(viewModel::refreshGeocode)

    // Observe changes to the contacts repo in our lifecycle and forward it onto the
    // [ContactsAdapter], optionally
    // updating the geocode for the contact.
    lifecycleScope.launch {
      viewModel.contactUpdatedEvent.collect {
        Timber.v("Received contactUpdatedEvent $it")
        when (it) {
          is ContactsRepoChange.ContactAdded -> {
            contactsAdapter.addContact(it.contact)
            viewModel.refreshGeocode(it.contact)
          }
          is ContactsRepoChange.ContactRemoved -> contactsAdapter.removeContact(it.contact)
          is ContactsRepoChange.ContactLocationUpdated -> {
            contactsAdapter.updateContact(it.contact)
            viewModel.refreshGeocode(it.contact)
          }
          is ContactsRepoChange.ContactCardUpdated -> contactsAdapter.updateContact(it.contact)
          is ContactsRepoChange.AllCleared -> contactsAdapter.clearAll()
        }
        binding.run {
          placeholder.visibility = if (viewModel.contacts.isEmpty()) View.VISIBLE else View.GONE
          contactsRecyclerView.visibility =
              if (viewModel.contacts.isEmpty()) View.GONE else View.VISIBLE
        }

        contactsCountingIdlingResource.run { if (!isIdleNow) decrement() }
      }
    }
  }

  override fun onClick(item: Contact, view: View, longClick: Boolean) {
    startActivity(
        Intent(this, MapActivity::class.java)
            .putExtra(
                "_args", Bundle().apply { putString(MapActivity.BUNDLE_KEY_CONTACT_ID, item.id) }))
  }
}
