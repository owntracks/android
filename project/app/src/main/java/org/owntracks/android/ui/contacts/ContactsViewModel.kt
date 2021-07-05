package org.owntracks.android.ui.contacts

import android.os.Bundle
import androidx.lifecycle.LiveData
import dagger.hilt.android.scopes.ActivityScoped
import org.owntracks.android.data.repos.ContactsRepo
import org.owntracks.android.model.FusedContact
import org.owntracks.android.ui.base.viewmodel.BaseViewModel
import javax.inject.Inject

@ActivityScoped
class ContactsViewModel @Inject constructor(private val contactsRepo: ContactsRepo) : BaseViewModel<ContactsMvvm.View?>(), ContactsMvvm.ViewModel<ContactsMvvm.View?> {
    override fun attachView(savedInstanceState: Bundle?, view: ContactsMvvm.View?) {
        super.attachView(savedInstanceState, view!!)
    }

    override val contacts: LiveData<MutableMap<String, FusedContact>>
        get() = contactsRepo.all
}