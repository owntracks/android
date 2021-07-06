package org.owntracks.android.ui.contacts

import android.os.Bundle
import androidx.lifecycle.LiveData
import dagger.hilt.android.scopes.ActivityScoped
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import org.owntracks.android.data.repos.ContactsRepo
import org.owntracks.android.geocoding.GeocoderProvider
import org.owntracks.android.model.FusedContact
import org.owntracks.android.ui.base.viewmodel.BaseViewModel
import timber.log.Timber
import javax.inject.Inject

@ActivityScoped
class ContactsViewModel @Inject constructor(
    private val contactsRepo: ContactsRepo, private val geocoderProvider: GeocoderProvider
) :
    BaseViewModel<ContactsMvvm.View?>(), ContactsMvvm.ViewModel<ContactsMvvm.View?> {
    private val mainScope = MainScope()
    override fun attachView(savedInstanceState: Bundle?, view: ContactsMvvm.View?) {
        super.attachView(savedInstanceState, view!!)
    }

    fun refreshGeocodes() {

        Timber.tag("399845").i("Refreshing contacts geocodes")
        mainScope.launch {
            contactsRepo.all.value?.forEach {
                it.value.messageLocation?.run { geocoderProvider.resolve(this) }
            }
        }
    }

    override val contacts: LiveData<MutableMap<String, FusedContact>>
        get() = contactsRepo.all
}