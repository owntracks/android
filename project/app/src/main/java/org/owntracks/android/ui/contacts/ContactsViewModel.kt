package org.owntracks.android.ui.contacts

import android.os.Bundle
import androidx.lifecycle.MutableLiveData
import org.owntracks.android.data.repos.ContactsRepo
import org.owntracks.android.injection.scopes.PerActivity
import org.owntracks.android.model.FusedContact
import org.owntracks.android.ui.base.viewmodel.BaseViewModel
import javax.inject.Inject

@PerActivity
class ContactsViewModel @Inject constructor(private val contactsRepo: ContactsRepo) : BaseViewModel<ContactsMvvm.View?>(), ContactsMvvm.ViewModel<ContactsMvvm.View?> {
    override fun attachView(savedInstanceState: Bundle?, view: ContactsMvvm.View?) {
        super.attachView(savedInstanceState, view!!)
    }

    override val contacts: MutableLiveData<MutableMap<String, FusedContact>>
        get() = contactsRepo.all
}