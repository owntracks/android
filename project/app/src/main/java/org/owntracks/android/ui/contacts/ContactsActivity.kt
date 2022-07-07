package org.owntracks.android.ui.contacts

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.activity.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import dagger.hilt.android.AndroidEntryPoint
import org.owntracks.android.R
import org.owntracks.android.databinding.UiContactsBinding
import org.owntracks.android.model.FusedContact
import org.owntracks.android.ui.base.BaseActivity
import org.owntracks.android.ui.base.BaseAdapter
import org.owntracks.android.ui.base.viewmodel.NoOpViewModel
import org.owntracks.android.ui.map.MapActivity

@AndroidEntryPoint
class ContactsActivity :
    BaseActivity<UiContactsBinding?, NoOpViewModel>(),
    BaseAdapter.ClickListener<FusedContact?> {
    private val vm: ContactsViewModel by viewModels()
    private lateinit var contactsAdapter: ContactsAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        contactsAdapter = ContactsAdapter(this, vm.coroutineScope)
        bindAndAttachContentView(R.layout.ui_contacts, savedInstanceState)
        setHasEventBus(false)
        setSupportToolbar(binding!!.appbar.toolbar)
        setDrawer(binding!!.appbar.toolbar)
        vm.contacts.observe({ this.lifecycle }, { contacts: Map<String, FusedContact> ->
            contactsAdapter.setContactList(contacts.values)
            vm.refreshGeocodes()
        })
        binding?.recyclerView?.run {
            layoutManager = LinearLayoutManager(this@ContactsActivity)
            adapter = contactsAdapter
        }
    }

    override fun onClick(fusedContact: FusedContact, view: View, longClick: Boolean) {
        val bundle = Bundle()
        bundle.putString(MapActivity.BUNDLE_KEY_CONTACT_ID, fusedContact.id)
        val intent = Intent(this, MapActivity::class.java)
        intent.putExtra("_args", bundle)
        startActivity(intent)
    }
}
