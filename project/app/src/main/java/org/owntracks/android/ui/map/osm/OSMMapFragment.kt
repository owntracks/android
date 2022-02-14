package org.owntracks.android.ui.map.osm

import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.location.Location
import android.os.Bundle
import android.view.LayoutInflater
import android.view.MotionEvent.ACTION_BUTTON_RELEASE
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.Observer
import androidx.preference.PreferenceManager
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.views.CustomZoomButtonsController
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.TilesOverlay
import org.osmdroid.views.overlay.mylocation.IMyLocationConsumer
import org.osmdroid.views.overlay.mylocation.IMyLocationProvider
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay
import org.owntracks.android.R
import org.owntracks.android.databinding.OsmMapFragmentBinding
import org.owntracks.android.location.LatLng
import org.owntracks.android.location.toGeoPoint
import org.owntracks.android.location.toLatLng
import org.owntracks.android.support.ContactImageBindingAdapter
import org.owntracks.android.ui.map.MapActivity
import org.owntracks.android.ui.map.MapFragment
import org.owntracks.android.ui.map.MapViewModel
import timber.log.Timber

class OSMMapFragment internal constructor(contactImageBindingAdapter: ContactImageBindingAdapter) :
    MapFragment<OsmMapFragmentBinding>(contactImageBindingAdapter) {
    override val layout: Int
        get() = R.layout.osm_map_fragment

    private var locationObserver: Observer<Location>? = null
    private val osmMapLocationSource: IMyLocationProvider = object : IMyLocationProvider {
        override fun startLocationProvider(myLocationConsumer: IMyLocationConsumer?): Boolean {
            val locationProvider: IMyLocationProvider = this
            locationObserver = Observer<Location> { location ->
                myLocationConsumer?.onLocationChanged(location, locationProvider)
                viewModel.setMapLocation(location.toLatLng())
                if (viewModel.viewMode == MapViewModel.ViewMode.Device) {
                    updateCamera(location.toLatLng())
                }
            }
            locationObserver?.run {
                viewModel.currentLocation.observe(viewLifecycleOwner, this)
            }
            return true
        }

        override fun stopLocationProvider() {
            locationObserver?.run(viewModel.currentLocation::removeObserver)
        }

        override fun getLastKnownLocation(): Location? {
            return viewModel.currentLocation.value
        }

        override fun destroy() {
            stopLocationProvider()
        }
    }

    private var mapView: MapView? = null


    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        Configuration.getInstance().apply {
            load(requireContext(), PreferenceManager.getDefaultSharedPreferences(requireContext()))
            osmdroidBasePath.resolve("tiles").run {
                if (exists()) {
                    deleteRecursively()
                }
            }
            osmdroidTileCache = requireContext().noBackupFilesDir.resolve("osmdroid/tiles")
        }
        return super.onCreateView(inflater, container, savedInstanceState)
    }

    private fun setMapStyle() {
        if (resources.configuration.uiMode.and(android.content.res.Configuration.UI_MODE_NIGHT_MASK) == android.content.res.Configuration.UI_MODE_NIGHT_YES) {
            mapView?.run {
                overlayManager.tilesOverlay.setColorFilter(TilesOverlay.INVERT_COLORS)
            }
        } else {
            mapView?.run {
                overlayManager.tilesOverlay.setColorFilter(null)
            }
        }
    }

    override fun initMap() {
        val myLocationEnabled =
            (requireActivity() as MapActivity).checkAndRequestMyLocationCapability(false)
        Timber.d("OSMMapFragment initMap locationEnabled=$myLocationEnabled")
        mapView = this.binding.osmMapView.apply {
            setTileSource(TileSourceFactory.MAPNIK)
            zoomController.setVisibility(CustomZoomButtonsController.Visibility.SHOW_AND_FADEOUT)
            controller.setZoom(ZOOM_STREET_LEVEL)
            controller.setCenter(viewModel.getMapLocation().toGeoPoint())
            // Make sure we don't add to the overlays
            if (!overlays.any { it is MyLocationNewOverlay && it.mMyLocationProvider == osmMapLocationSource }) {
                overlays.add(
                    MyLocationNewOverlay(
                        osmMapLocationSource,
                        this
                    ).apply {
                        setOnClickListener { onMapClick() }
                        setOnTouchListener { v, event ->
                            if (event.action == ACTION_BUTTON_RELEASE) {
                                v.performClick()
                            }
                            onMapClick()
                            false
                        }
                    })
            }

            setMultiTouchControls(true)
        }
        setMapStyle()
    }

    override fun clearMarkers() {
        mapView?.overlays?.clear()
    }

    override fun updateCamera(latLng: LatLng) {
        mapView?.controller?.run {
            setCenter(latLng.toGeoPoint())
        }
    }

    override fun updateMarkerOnMap(id: String, latLng: LatLng, image: Bitmap) {
        mapView?.run {
            val existingMarker: Marker? =
                overlays.firstOrNull { it is Marker && it.id == id } as Marker?
            if (existingMarker != null) {
                existingMarker.position = latLng.toGeoPoint()
            } else if (activity?.isDestroyed == false) {
                /*
                There's a race condition where in the time it takes to create all the markers, the
                activity has been destroyed. Creating a Marker requires (for some reason) the `mapView`
                to be attached to a non-destroyed activity somehow, so we check before creating the marker
                 */
                overlays.add(0, Marker(this).apply {
                    this.id = id
                    position = latLng.toGeoPoint()
                    infoWindow = null
                    setOnMarkerClickListener { marker, _ ->
                        onMarkerClicked(marker.id)
                        true
                    }
                    setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
                })
            }
            overlays.firstOrNull { it is Marker && it.id == id }?.run {
                (this as Marker).icon = BitmapDrawable(resources, image)
            }
        }
    }

    override fun removeMarkerFromMap(id: String) {
        mapView?.run {
            overlays.removeAll { it is Marker && it.id == id }
        }
    }

    override fun onResume() {
        super.onResume()
        mapView?.onResume()
        setMapStyle()
    }

    override fun onPause() {
        mapView?.onPause()
        super.onPause()
    }

    override fun onDetach() {
        mapView?.onDetach()
        super.onDetach()
    }

    override fun onStop() {
        mapView?.onDetach()
        super.onStop()
    }

    companion object {
        private const val ZOOM_STREET_LEVEL: Double = 16.0
    }
}