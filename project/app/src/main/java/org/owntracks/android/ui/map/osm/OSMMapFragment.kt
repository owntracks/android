package org.owntracks.android.ui.map.osm

import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.MotionEvent.ACTION_BUTTON_RELEASE
import android.view.View
import android.view.ViewGroup
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.CustomZoomButtonsController
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.TilesOverlay
import org.osmdroid.views.overlay.mylocation.IMyLocationProvider
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay
import org.owntracks.android.R
import org.owntracks.android.data.repos.LocationRepo
import org.owntracks.android.databinding.OsmMapFragmentBinding
import org.owntracks.android.location.LatLng
import org.owntracks.android.location.LocationProviderClient
import org.owntracks.android.location.toGeoPoint
import org.owntracks.android.location.toOSMLocationSource
import org.owntracks.android.support.ContactImageBindingAdapter
import org.owntracks.android.ui.map.MapActivity
import org.owntracks.android.ui.map.MapActivity.Companion.STARTING_LATITUDE
import org.owntracks.android.ui.map.MapActivity.Companion.STARTING_LONGITUDE
import org.owntracks.android.ui.map.MapFragment
import org.owntracks.android.ui.map.MapLocationSource
import timber.log.Timber

class OSMMapFragment internal constructor(
    private val locationRepo: LocationRepo,
    private val locationProviderClient: LocationProviderClient,
    contactImageBindingAdapter: ContactImageBindingAdapter
) : MapFragment<OsmMapFragmentBinding>(contactImageBindingAdapter) {
    private var locationSource: IMyLocationProvider? = null
    private var mapView: MapView? = null
    override val layout: Int
        get() = R.layout.osm_map_fragment

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        locationSource = MapLocationSource(
            locationProviderClient,
            viewModel.mapLocationUpdateCallback
        ).toOSMLocationSource()
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
            if (locationRepo.currentLocation != null) {
                controller.setCenter(
                    GeoPoint(
                        locationRepo.currentLocation!!.latitude,
                        locationRepo.currentLocation!!.longitude
                    )
                )
            } else {
                controller.setCenter(GeoPoint(STARTING_LATITUDE, STARTING_LONGITUDE))
            }
            // Make sure we don't add to the overlays
            if (!overlays.any { it is MyLocationNewOverlay && it.mMyLocationProvider == locationSource }) {
                overlays.add(
                    MyLocationNewOverlay(
                        locationSource,
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
            } else {
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

    companion object {
        private const val ZOOM_STREET_LEVEL: Double = 16.0
    }
}