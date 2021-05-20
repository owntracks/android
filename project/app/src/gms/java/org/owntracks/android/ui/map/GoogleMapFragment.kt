package org.owntracks.android.ui.map

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.GoogleMap.OnCameraMoveStartedListener.REASON_GESTURE
import com.google.android.gms.maps.MapsInitializer
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import dagger.hilt.android.AndroidEntryPoint
import org.owntracks.android.R
import org.owntracks.android.data.repos.LocationRepo
import org.owntracks.android.databinding.GoogleMapFragmentBinding
import org.owntracks.android.gms.location.toGMSLatLng
import org.owntracks.android.gms.location.toGMSLocationSource
import org.owntracks.android.location.LocationSource
import timber.log.Timber
import java.util.*

@AndroidEntryPoint
class GoogleMapFragment internal constructor() : MapFragment(), OnMapReadyCallback {
    constructor(locationSource: LocationSource, locationRepo: LocationRepo?) : this() {
        this.locationSource = locationSource
        this.locationRepo = locationRepo
    }

    private var locationRepo: LocationRepo? = null
    private var locationSource: LocationSource? = null
    private var googleMap: GoogleMap? = null
    private var binding: GoogleMapFragmentBinding? = null
    private val markers: MutableMap<String, Marker?> = HashMap()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = DataBindingUtil.inflate(inflater, R.layout.google_map_fragment, container, false)
        MapsInitializer.initialize(requireContext())
        val mapView = this.binding!!.googleMapView
        mapView.onCreate(savedInstanceState)
        mapView.getMapAsync(this)
        return binding!!.root
    }

    override fun onMapReady(googleMap: GoogleMap) {
        this.googleMap = googleMap
        if ((requireActivity() as MapActivity).checkAndRequestLocationPermissions()) {
            initMap()
        }
        ((requireActivity()) as MapActivity).onMapReady()
    }

    @SuppressLint("MissingPermission")
    private fun initMap() {
        this.googleMap?.run {
            isIndoorEnabled = false
            isMyLocationEnabled = true
            if (locationSource == null) {
                Timber.e("No location source set, falling back to Google internal")
            } else {
                setLocationSource(locationSource!!.toGMSLocationSource())
            }
            uiSettings.isMyLocationButtonEnabled = false
            uiSettings.setAllGesturesEnabled(true)
            if (activity is MapActivity) {
                if (locationRepo == null) {
                    locationRepo = (activity as MapActivity).locationRepo
                }
                if (locationSource == null) {
                    locationSource = (activity as MapActivity).mapLocationSource
                }
            }

            if (locationRepo?.currentLocation != null) {
                moveCamera(CameraUpdateFactory.newLatLngZoom(LatLng(locationRepo!!.currentLocation!!.latitude, locationRepo!!.currentLocation!!.longitude), ZOOM_LEVEL_STREET))
            } else {
                moveCamera(CameraUpdateFactory.newLatLngZoom(LatLng(MapActivity.STARTING_LATITUDE, MapActivity.STARTING_LONGITUDE), ZOOM_LEVEL_STREET))
            }

            setOnMarkerClickListener {
                it.tag?.run { (activity as MapActivity).onMarkerClicked(this as String) }
                true
            }

            setOnMapClickListener { (activity as MapActivity).onMapClick() }
            setOnCameraMoveStartedListener { reason ->
                if (reason == REASON_GESTURE) {
                    (activity as MapActivity).onMapClick()
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

    override fun updateMarker(id: String, latLng: org.owntracks.android.location.LatLng) {
        val marker = markers[id]
        if (marker != null && marker.tag != null) {
            marker.position = latLng.toGMSLatLng()
        } else {
            // If a marker has been removed, its tag will be null. Doing anything with it will make it explode
            if (marker != null) {
                markers.remove(id)
                marker.remove()
            }
            markers[id] = googleMap!!.addMarker(MarkerOptions().position(latLng.toGMSLatLng()).anchor(0.5f, 0.5f).visible(false)).also { it?.tag = id }
        }
    }

    override fun setMarkerImage(id: String, bitmap: Bitmap) {
        markers[id]?.run {
            setIcon(BitmapDescriptorFactory.fromBitmap(bitmap))
            isVisible = true
        }
    }

    override fun locationPermissionGranted() {
        initMap()
    }

    override fun removeMarker(id: String) {
        markers[id]?.remove()
    }

    override fun onResume() {
        super.onResume()
        binding?.googleMapView?.onResume()
    }

    override fun onLowMemory() {
        binding?.googleMapView?.onLowMemory()
        super.onLowMemory()
    }

    override fun onPause() {
        binding?.googleMapView?.onPause()
        super.onPause()
    }

    override fun onDestroy() {
        binding?.googleMapView?.onDestroy()
        super.onDestroy()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        binding?.googleMapView?.onSaveInstanceState(outState)
        super.onSaveInstanceState(outState)
    }

    override fun onStart() {
        super.onStart()
        binding?.googleMapView?.onStart()
    }

    override fun onStop() {
        binding?.googleMapView?.onStop()
        super.onStop()
    }

    companion object {
        private const val ZOOM_LEVEL_STREET: Float = 15f
    }
}

