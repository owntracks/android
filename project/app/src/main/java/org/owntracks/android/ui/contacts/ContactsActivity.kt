package org.owntracks.android.ui.contacts

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.recyclerview.widget.LinearLayoutManager
import dagger.hilt.android.AndroidEntryPoint
import org.owntracks.android.R
import org.owntracks.android.databinding.UiContactsBinding
import org.owntracks.android.geocoding.GeocoderProvider
import org.owntracks.android.model.FusedContact
import org.owntracks.android.model.messages.MessageLocation
import org.owntracks.android.ui.base.BaseActivity
import org.owntracks.android.ui.base.BaseAdapter
import org.owntracks.android.ui.map.MapActivity
import javax.inject.Inject

@AndroidEntryPoint
class ContactsActivity : BaseActivity<UiContactsBinding?, ContactsMvvm.ViewModel<*>?>(), ContactsMvvm.View, BaseAdapter.ClickListener<FusedContact?> {

    private lateinit var contactsAdapter: ContactsAdapter

    @JvmField
    @Inject
    var geocoderProvider: GeocoderProvider? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        contactsAdapter = ContactsAdapter(this)
        setHasEventBus(false)
        bindAndAttachContentView(R.layout.ui_contacts, savedInstanceState)
        setSupportToolbar(binding!!.toolbar)
        setDrawer(binding!!.toolbar)
        binding!!.vm!!.contacts.observe({ this.lifecycle }, { contacts: Map<String, FusedContact> ->
            contactsAdapter.setContactList(contacts.values)
            for (contact in contacts.values) {
                contact.messageLocation.removeObservers(this)
                contact.messageLocation.observe({ this.lifecycle }, { messageLocation: MessageLocation? -> geocoderProvider!!.resolve(messageLocation!!) })
            }
        })
        binding!!.recyclerView.layoutManager = LinearLayoutManager(this)
        binding!!.recyclerView.adapter = contactsAdapter
    }

    override fun onClick(fusedContact: FusedContact, view: View, longClick: Boolean) {
        val bundle = Bundle()
        bundle.putString(MapActivity.BUNDLE_KEY_CONTACT_ID, fusedContact.id)
        val intent = Intent(this, MapActivity::class.java)
        intent.putExtra("_args", bundle)
        startActivity(intent)
    }
}