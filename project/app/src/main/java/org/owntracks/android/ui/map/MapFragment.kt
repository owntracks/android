package org.owntracks.android.ui.map

import android.graphics.Bitmap
import android.os.Bundle
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.databinding.ViewDataBinding
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.flowWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import org.owntracks.android.R
import org.owntracks.android.data.repos.ContactsRepoChange
import org.owntracks.android.data.waypoints.WaypointModel
import org.owntracks.android.location.LatLng
import org.owntracks.android.model.Contact
import org.owntracks.android.support.ContactImageBindingAdapter
import timber.log.Timber

abstract class MapFragment<V : ViewDataBinding>
internal constructor(private val contactImageBindingAdapter: ContactImageBindingAdapter) :
    Fragment() {
    protected abstract val layout: Int
    protected lateinit var binding: V

    abstract fun updateCamera(latLng: LatLng)

    abstract fun updateMarkerOnMap(id: String, latLng: LatLng, image: Bitmap)

    abstract fun removeMarkerFromMap(id: String)

    abstract fun currentMarkersOnMap(): Set<String>

    abstract fun initMap()

    abstract fun drawRegions(regions: Set<WaypointModel>)

    abstract fun setMapLayerType(mapLayerStyle: MapLayerStyle)

    protected val viewModel: MapViewModel by activityViewModels()

    protected fun getRegionColor(): Int {
        val typedValue = TypedValue()
        requireContext().theme.resolveAttribute(R.attr.colorRegion, typedValue, true)
        return typedValue.data
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding =
            DataBindingUtil.inflate<V>(inflater, layout, container, false).apply {
                lifecycleOwner = this@MapFragment
            }

        viewModel.apply {
            mapCenter.observe(viewLifecycleOwner, this@MapFragment::updateCamera)
            updateAllMarkers(allContacts.values.toSet())
            lifecycleScope.launch {
                viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                    contactUpdatedEvent.collect {
                        when (it) {
                            ContactsRepoChange.AllCleared -> {
                                updateAllMarkers(emptySet())
                            }
                            is ContactsRepoChange.ContactAdded -> {
                                updateMarkerForContact(it.contact)
                            }
                            is ContactsRepoChange.ContactLocationUpdated -> {
                                updateMarkerForContact(it.contact)
                                if (viewMode == MapViewModel.ViewMode.Contact(true) &&
                                    currentContact.value == it.contact
                                ) {
                                    it.contact.latLng?.run(this@MapFragment::updateCamera)
                                }
                            }
                            is ContactsRepoChange.ContactCardUpdated -> {
                                updateMarkerForContact(it.contact)
                            }
                            is ContactsRepoChange.ContactRemoved -> {
                                removeMarkerFromMap(it.contact.id)
                            }
                        }
                    }
                    waypoints
                        .flowWithLifecycle(viewLifecycleOwner.lifecycle, Lifecycle.State.STARTED)
                        .collect {
                                regions ->
                            drawRegions(regions.toSet())
                        }
                }
            }
            mapLayerStyle.observe(viewLifecycleOwner, this@MapFragment::setMapLayerType)
            onMapReady()
        }
        return binding.root
    }

    internal fun updateAllMarkers(contacts: Set<Contact>) {
        currentMarkersOnMap().subtract(contacts.map { it.id }.toSet()).forEach(::removeMarkerFromMap)
        contacts.forEach(::updateMarkerForContact)
    }

    private fun updateMarkerForContact(contact: Contact) {
        if (contact.latLng == null) {
            Timber.w("unable to update marker for $contact. no location")
            return
        }
        Timber.v("updating marker for contact: ${contact.id}")
        lifecycleScope.launch {
            contactImageBindingAdapter.run {
                updateMarkerOnMap(contact.id, contact.latLng!!, getBitmapFromCache(contact))
            }
            if (contact == viewModel.currentContact.value) {
                viewModel.refreshGeocodeForContact(contact)
            }
        }
    }

    fun onMapClick() {
        viewModel.onMapClick()
    }

    fun onMarkerClicked(id: String) {
        viewModel.onMarkerClick(id)
    }
}
