package org.owntracks.android.ui.contacts

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import javax.inject.Named
import kotlinx.coroutines.launch
import org.owntracks.android.R
import org.owntracks.android.data.repos.ContactsRepoChange
import org.owntracks.android.databinding.UiContactsBinding
import org.owntracks.android.model.Contact
import org.owntracks.android.test.ThresholdIdlingResourceInterface
import org.owntracks.android.ui.DrawerProvider
import org.owntracks.android.ui.map.MapActivity
import org.owntracks.android.ui.mixins.ServiceStarter
import timber.log.Timber

@AndroidEntryPoint
class ContactsActivity :
    AppCompatActivity(), AdapterClickListener<Contact>, ServiceStarter by ServiceStarter.Impl() {
  @Inject lateinit var drawerProvider: DrawerProvider

  @Inject
  @Named("contactsActivityIdlingResource")
  lateinit var contactsCountingIdlingResource: ThresholdIdlingResourceInterface

  private val viewModel: ContactsViewModel by viewModels()
  private lateinit var contactsAdapter: ContactsAdapter

  override fun onCreate(savedInstanceState: Bundle?) {
    enableEdgeToEdge()
    startService(this)
    super.onCreate(savedInstanceState)
    contactsAdapter = ContactsAdapter(this, viewModel.coroutineScope)
    val binding =
        DataBindingUtil.setContentView<UiContactsBinding>(this, R.layout.ui_contacts).apply {
          vm = viewModel
          appbar.toolbar.run {
            setSupportActionBar(this)
            drawerProvider.attach(this, drawerLayout, navigationView)
          }
          contactsRecyclerView.run {
            layoutManager = LinearLayoutManager(this@ContactsActivity)
            adapter = contactsAdapter
          }

          // Handle window insets for edge-to-edge
          ViewCompat.setOnApplyWindowInsetsListener(drawerLayout) { view, windowInsets ->
            val insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars())

            appbar.root.updatePadding(top = insets.top)
            navigationView.updatePadding(top = insets.top, bottom = insets.bottom)

            WindowInsetsCompat.CONSUMED
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
