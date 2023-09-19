package org.owntracks.android.ui.contacts

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import org.owntracks.android.data.repos.ContactsRepo
import org.owntracks.android.data.repos.ContactsRepoChange
import org.owntracks.android.geocoding.GeocoderProvider
import org.owntracks.android.model.Contact

@HiltViewModel
class ContactsViewModel @Inject constructor(
    private val contactsRepo: ContactsRepo,
    private val geocoderProvider: GeocoderProvider
) : ViewModel() {
    fun refreshGeocode(contact: Contact) {
        contact.geocodeLocation(geocoderProvider, viewModelScope)
    }

    val contacts = contactsRepo.all
    val contactUpdatedEvent: Flow<ContactsRepoChange>
        get() = contactsRepo.repoChangedEvent
    val coroutineScope: CoroutineScope
        get() = viewModelScope
}
