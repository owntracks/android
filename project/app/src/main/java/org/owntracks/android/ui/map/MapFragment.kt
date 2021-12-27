package org.owntracks.android.ui.map

import android.graphics.Bitmap
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.databinding.ViewDataBinding
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.preference.PreferenceManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.osmdroid.config.Configuration
import org.owntracks.android.location.LatLng
import org.owntracks.android.model.FusedContact
import org.owntracks.android.support.ContactImageBindingAdapter
import timber.log.Timber

abstract class MapFragment<V : ViewDataBinding> internal constructor(
    private val contactImageBindingAdapter: ContactImageBindingAdapter
) : Fragment() {
    protected abstract val layout: Int
    protected lateinit var binding: V
    abstract fun clearMarkers()
    abstract fun updateCamera(latLng: LatLng)
    abstract fun updateMarkerOnMap(id: String, latLng: LatLng, image: Bitmap)
    abstract fun removeMarkerFromMap(id: String)
    abstract fun initMap()
    protected val viewModel: MapViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        Configuration.getInstance().apply {
            load(context, PreferenceManager.getDefaultSharedPreferences(context))
            osmdroidBasePath.resolve("tiles").run {
                if (exists()) {
                    deleteRecursively()
                }
            }
            osmdroidTileCache = requireContext().noBackupFilesDir.resolve("osmdroid/tiles")
        }
        binding = DataBindingUtil.inflate(inflater, layout, container, false)
        binding.lifecycleOwner = this

        initMap()
        viewModel.mapCenter.observe(viewLifecycleOwner, { latLng: LatLng ->
            updateCamera(latLng)
        })
        viewModel.allContacts.observe(viewLifecycleOwner, { contacts ->
            contacts.values.forEach { updateMarkerForContact(it) }
        })
        viewModel.myLocationEnabled.observe(viewLifecycleOwner, {
            initMap()
        })
        viewModel.onMapReady()
        return binding.root
    }

    private fun updateMarkerForContact(contact: FusedContact) {
        if (contact.latLng == null) {
            Timber.w("unable to update marker for $contact. no location")
            return
        }
        Timber.v("updating marker for contact: %s", contact.id)
        lifecycleScope.launch {
            contactImageBindingAdapter.run {
                updateMarkerOnMap(contact.id, contact.latLng!!, getBitmapFromCache(contact))
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