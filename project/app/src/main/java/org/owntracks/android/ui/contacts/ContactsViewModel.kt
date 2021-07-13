package org.owntracks.android.ui.contacts

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import org.owntracks.android.data.repos.ContactsRepo
import org.owntracks.android.geocoding.GeocoderProvider
import org.owntracks.android.model.FusedContact
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class ContactsViewModel @Inject constructor(
        private val contactsRepo: ContactsRepo,
        private val geocoderProvider: GeocoderProvider
) : ViewModel() {
    private val mainScope = MainScope()
    fun refreshGeocodes() {
        Timber.d("Refreshing contacts geocodes")
        mainScope.launch {
            contactsRepo.all.value?.run {
                map { it.value.messageLocation }
                        .filterNotNull()
                        .iterator()
                        .forEach { geocoderProvider.resolve(it) }
            }
        }
    }

    val contacts: LiveData<MutableMap<String, FusedContact>>
        get() = contactsRepo.all
}