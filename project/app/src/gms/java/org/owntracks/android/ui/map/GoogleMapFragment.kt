package org.owntracks.android.ui.map

import android.annotation.SuppressLint
import android.content.res.Configuration
import android.graphics.Bitmap
import android.location.Location
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.Observer
import com.google.android.gms.maps.*
import com.google.android.gms.maps.GoogleMap.OnCameraMoveStartedListener.REASON_GESTURE
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.MapStyleOptions
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import org.owntracks.android.R
import org.owntracks.android.databinding.GoogleMapFragmentBinding
import org.owntracks.android.gms.location.toGMSLatLng
import org.owntracks.android.location.toLatLng
import org.owntracks.android.support.ContactImageBindingAdapter
import timber.log.Timber

class GoogleMapFragment internal constructor(contactImageBindingAdapter: ContactImageBindingAdapter) :
    MapFragment<GoogleMapFragmentBinding>(contactImageBindingAdapter), OnMapReadyCallback,
    OnMapsSdkInitializedCallback {
    override val layout: Int
        get() = R.layout.google_map_fragment

    private var locationObserver: Observer<Location>? = null
    private val googleMapLocationSource: LocationSource by lazy {
        object : LocationSource {
            override fun activate(onLocationChangedListener: LocationSource.OnLocationChangedListener) {
                locationObserver = object : Observer<Location> {
                    override fun onChanged(location: Location) {
                        onLocationChangedListener.onLocationChanged(location)
                        viewModel.setMapLocation(location.toLatLng())
                        if (viewModel.viewMode == MapViewModel.ViewMode.Device) {
                            updateCamera(location.toLatLng())
                        }
                    }
                }
                locationObserver?.run {
                    viewModel.currentLocation.observe(viewLifecycleOwner, this)
                }
            }

            override fun deactivate() {
                locationObserver?.run(viewModel.currentLocation::removeObserver)
            }
        }
    }

    private var googleMap: GoogleMap? = null
    private val markers: MutableMap<String, Marker> = HashMap()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val root = super.onCreateView(inflater, container, savedInstanceState)
        binding.googleMapView.onCreate(savedInstanceState)
        binding.googleMapView.getMapAsync(this)
        return root
    }

    override fun onMapReady(googleMap: GoogleMap) {
        this.googleMap = googleMap
        initMap()
        viewModel.onMapReady()
    }

    private fun setMapStyle() {
        if (resources.configuration.uiMode.and(Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES) {
            googleMap?.setMapStyle(
                MapStyleOptions.loadRawResourceStyle(
                    requireContext(),
                    R.raw.google_maps_night_theme
                )
            )
        }
    }

    @SuppressLint("MissingPermission")
    override fun initMap() {
        MapsInitializer.initialize(requireContext(), MapsInitializer.Renderer.LATEST, this)
        this.googleMap?.run {
            val myLocationEnabled =
                (requireActivity() as MapActivity).checkAndRequestMyLocationCapability(false)
            Timber.d("GoogleMapFragment initMap hasLocationCapability=$myLocationEnabled")
            isIndoorEnabled = false
            isMyLocationEnabled = myLocationEnabled
            uiSettings.isMyLocationButtonEnabled = false
            uiSettings.setAllGesturesEnabled(true)

            setLocationSource(googleMapLocationSource)

            setMapStyle()

            moveCamera(
                CameraUpdateFactory.newLatLngZoom(
                    viewModel.getMapLocation().toGMSLatLng(),
                    ZOOM_LEVEL_STREET
                )
            )

            setOnMarkerClickListener {
                it.tag?.run { onMarkerClicked(this as String) }
                true
            }

            setOnMapClickListener { onMapClick() }
            setOnCameraMoveStartedListener { reason ->
                if (reason == REASON_GESTURE) {
                    onMapClick()
                }
            }
        }
    }

    override fun updateCamera(latLng: org.owntracks.android.location.LatLng) {
        googleMap?.moveCamera(CameraUpdateFactory.newLatLng(latLng.toGMSLatLng()))
    }

    override fun clearMarkers() {
        this.googleMap?.clear()
        markers.clear()
    }

    override fun updateMarkerOnMap(
        id: String,
        latLng: org.owntracks.android.location.LatLng,
        image: Bitmap
    ) {
        googleMap?.run { // If we don't have a google Map, we can't add markers to it
            // Remove null markers from the collection
            markers.values.removeAll { it.tag == null }
            markers.getOrPut(id) {
                addMarker(
                    MarkerOptions()
                        .position(latLng.toGMSLatLng())
                        .anchor(0.5f, 0.5f).visible(false)
                )!!.also { it.tag = id }
            }.run {
                position = latLng.toGMSLatLng()
                setIcon(BitmapDescriptorFactory.fromBitmap(image))
                isVisible = true
            }
        }
    }

    override fun removeMarkerFromMap(id: String) {
        markers[id]?.remove()
    }

    override fun onResume() {
        super.onResume()
        binding.googleMapView.onResume()
        setMapStyle()
    }

    override fun onLowMemory() {
        binding.googleMapView.onLowMemory()
        super.onLowMemory()
    }

    override fun onPause() {
        binding.googleMapView.onPause()
        super.onPause()
    }

    override fun onDestroy() {
        binding.googleMapView.onDestroy()
        super.onDestroy()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        binding.googleMapView.onSaveInstanceState(outState)
        super.onSaveInstanceState(outState)
    }

    override fun onStart() {
        super.onStart()
        binding.googleMapView.onStart()
    }

    override fun onStop() {
        binding.googleMapView.onStop()
        super.onStop()
    }

    companion object {
        private const val ZOOM_LEVEL_STREET: Float = 15f
    }

    override fun onMapsSdkInitialized(renderer: MapsInitializer.Renderer) {
        Timber.d("Maps SDK initialized with renderer: ${renderer.name}")
    }
}