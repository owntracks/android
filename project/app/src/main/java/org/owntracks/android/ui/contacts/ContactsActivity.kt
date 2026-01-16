package org.owntracks.android.ui.contacts

import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import javax.inject.Named
import org.owntracks.android.data.repos.ContactsRepoChange
import org.owntracks.android.model.Contact
import org.owntracks.android.preferences.Preferences
import org.owntracks.android.support.ContactImageBindingAdapter
import org.owntracks.android.test.ThresholdIdlingResourceInterface
import org.owntracks.android.ui.map.MapActivity
import org.owntracks.android.ui.mixins.ServiceStarter
import org.owntracks.android.ui.navigation.Destination
import org.owntracks.android.ui.navigation.toActivityClass
import org.owntracks.android.ui.theme.OwnTracksTheme
import timber.log.Timber

@AndroidEntryPoint
class ContactsActivity :
    AppCompatActivity(),
    ServiceStarter by ServiceStarter.Impl() {

    @Inject
    @Named("contactsActivityIdlingResource")
    lateinit var contactsCountingIdlingResource: ThresholdIdlingResourceInterface

    @Inject
    lateinit var contactImageBindingAdapter: ContactImageBindingAdapter

    @Inject
    lateinit var preferences: Preferences

    private val viewModel: ContactsViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        startService(this)
        super.onCreate(savedInstanceState)

        setContent {
            OwnTracksTheme(dynamicColor = preferences.dynamicColorsEnabled) {
                // Convert contacts map to a mutable state list sorted by timestamp
                val contactsList = remember {
                    mutableStateListOf<Contact>().apply {
                        addAll(viewModel.contacts.values.sortedByDescending { it.locationTimestamp })
                    }
                }

                // Observe contact changes
                androidx.compose.runtime.LaunchedEffect(Unit) {
                    viewModel.contactUpdatedEvent.collect { change ->
                        Timber.v("Received contactUpdatedEvent $change")
                        when (change) {
                            is ContactsRepoChange.ContactAdded -> {
                                contactsList.add(change.contact)
                                contactsList.sortByDescending { it.locationTimestamp }
                                viewModel.refreshGeocode(change.contact)
                            }
                            is ContactsRepoChange.ContactRemoved -> {
                                contactsList.removeAll { it.id == change.contact.id }
                            }
                            is ContactsRepoChange.ContactLocationUpdated -> {
                                val index = contactsList.indexOfFirst { it.id == change.contact.id }
                                if (index >= 0) {
                                    contactsList[index] = change.contact
                                    contactsList.sortByDescending { it.locationTimestamp }
                                }
                                viewModel.refreshGeocode(change.contact)
                            }
                            is ContactsRepoChange.ContactCardUpdated -> {
                                val index = contactsList.indexOfFirst { it.id == change.contact.id }
                                if (index >= 0) {
                                    contactsList[index] = change.contact
                                }
                            }
                            is ContactsRepoChange.AllCleared -> {
                                contactsList.clear()
                            }
                        }
                        contactsCountingIdlingResource.run { if (!isIdleNow) decrement() }
                    }
                }

                // Trigger geocode refresh on startup
                androidx.compose.runtime.LaunchedEffect(Unit) {
                    viewModel.contacts.values.forEach(viewModel::refreshGeocode)
                }

                ContactsScreen(
                    contacts = contactsList,
                    contactImageBindingAdapter = contactImageBindingAdapter,
                    onNavigate = { destination ->
                        navigateToDestination(destination)
                    },
                    onContactClick = { contact ->
                        startActivity(
                            Intent(this@ContactsActivity, MapActivity::class.java)
                                .putExtra(
                                    "_args",
                                    Bundle().apply {
                                        putString(MapActivity.BUNDLE_KEY_CONTACT_ID, contact.id)
                                    }
                                )
                        )
                    },
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }

    private fun navigateToDestination(destination: Destination) {
        val activityClass = destination.toActivityClass() ?: return
        if (this.javaClass != activityClass) {
            startActivity(Intent(this, activityClass))
        }
    }
}
