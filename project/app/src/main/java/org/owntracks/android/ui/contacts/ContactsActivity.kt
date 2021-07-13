package org.owntracks.android.ui.contacts

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import androidx.recyclerview.widget.LinearLayoutManager
import dagger.hilt.android.AndroidEntryPoint
import org.owntracks.android.R
import org.owntracks.android.databinding.UiContactsBinding
import org.owntracks.android.model.FusedContact
import org.owntracks.android.support.DrawerProvider
import org.owntracks.android.ui.base.BaseRecyclerViewAdapterWithClickHandler
import org.owntracks.android.ui.base.ClickHasBeenHandled
import org.owntracks.android.ui.map.MapActivity
import javax.inject.Inject

@AndroidEntryPoint
class ContactsActivity : AppCompatActivity(),
        BaseRecyclerViewAdapterWithClickHandler.ClickListener<FusedContact> {
    @Inject
    lateinit var drawerProvider: DrawerProvider

    private val viewModel: ContactsViewModel by viewModels()
    private lateinit var contactsAdapter: ContactsAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        contactsAdapter = ContactsAdapter(this)

        val binding: UiContactsBinding = DataBindingUtil.setContentView(this, R.layout.ui_contacts)
        binding.vm = viewModel
        binding.lifecycleOwner = this
        setSupportActionBar(binding.appbar.toolbar)
        drawerProvider.attach(binding.appbar.toolbar)

        binding.recyclerView.layoutManager = LinearLayoutManager(this)
        binding.recyclerView.adapter = contactsAdapter

        viewModel.contacts.observe({ this.lifecycle }, { contacts: Map<String, FusedContact> ->
            contactsAdapter.setData(contacts.values)
            viewModel.refreshGeocodes()
        })
    }

    override fun onClick(
            `object`: FusedContact,
            view: View,
            longClick: Boolean
    ): ClickHasBeenHandled {
        val bundle = Bundle()
        bundle.putString(MapActivity.BUNDLE_KEY_CONTACT_ID, `object`.id)
        val intent = Intent(this, MapActivity::class.java)
        intent.putExtra("_args", bundle)
        startActivity(intent)
        return true
    }
}