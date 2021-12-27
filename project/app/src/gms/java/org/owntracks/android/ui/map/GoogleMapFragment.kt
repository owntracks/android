package org.owntracks.android.ui.map

import android.annotation.SuppressLint
import android.content.res.Configuration
import android.graphics.Bitmap
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.GoogleMap.OnCameraMoveStartedListener.REASON_GESTURE
import com.google.android.gms.maps.MapsInitializer
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.model.*
import org.owntracks.android.R
import org.owntracks.android.data.repos.LocationRepo
import org.owntracks.android.databinding.GoogleMapFragmentBinding
import org.owntracks.android.gms.location.toGMSLatLng
import org.owntracks.android.gms.location.toGMSLocationSource
import org.owntracks.android.location.LocationProviderClient
import org.owntracks.android.support.ContactImageBindingAdapter
import timber.log.Timber
import java.util.*

class GoogleMapFragment internal constructor(
    private val locationRepo: LocationRepo,
    private val locationProviderClient: LocationProviderClient,
    contactImageBindingAdapter: ContactImageBindingAdapter
) : MapFragment<GoogleMapFragmentBinding>(contactImageBindingAdapter), OnMapReadyCallback {
    private var locationSource: com.google.android.gms.maps.LocationSource? = null
    private var googleMap: GoogleMap? = null
    override val layout: Int
        get() = R.layout.google_map_fragment
    private val markers: MutableMap<String, Marker> = HashMap()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        val root = super.onCreateView(inflater, container, savedInstanceState)
        locationSource = MapLocationSource(
            locationProviderClient,
            viewModel.mapLocationUpdateCallback
        ).toGMSLocationSource()
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
        MapsInitializer.initialize(requireContext())
        this.googleMap?.run {
            val myLocationEnabled =
                (requireActivity() as MapActivity).checkAndRequestMyLocationCapability(false)
            Timber.d("GoogleMapFragment initMap hasLocationCapability=$myLocationEnabled")
            isIndoorEnabled = false
            isMyLocationEnabled = myLocationEnabled
            uiSettings.isMyLocationButtonEnabled = false
            uiSettings.setAllGesturesEnabled(true)

            setLocationSource(locationSource)

            setMapStyle()

            if (locationRepo.currentLocation != null) {
                moveCamera(
                    CameraUpdateFactory.newLatLngZoom(
                        LatLng(
                            locationRepo.currentLocation!!.latitude,
                            locationRepo.currentLocation!!.longitude
                        ), ZOOM_LEVEL_STREET
                    )
                )
            } else {
                moveCamera(
                    CameraUpdateFactory.newLatLngZoom(
                        LatLng(
                            MapActivity.STARTING_LATITUDE,
                            MapActivity.STARTING_LONGITUDE
                        ), ZOOM_LEVEL_STREET
                    )
                )
            }

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
            markers.values.removeIf { it.tag == null }
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
        googleMap?.setLocationSource(locationSource)
        binding.googleMapView.onResume()
        setMapStyle()
    }

    override fun onLowMemory() {
        binding.googleMapView.onLowMemory()
        super.onLowMemory()
    }

    override fun onPause() {
        binding.googleMapView.onPause()
        locationSource?.deactivate()
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
}

