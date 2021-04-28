package org.owntracks.android.ui.map.osm

import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.MotionEvent.ACTION_BUTTON_RELEASE
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.preference.PreferenceManager
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.CustomZoomButtonsController
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay
import org.owntracks.android.R
import org.owntracks.android.databinding.OsmMapFragmentBinding
import org.owntracks.android.location.LatLng
import org.owntracks.android.location.toGeoPoint
import org.owntracks.android.ui.map.MapActivity
import org.owntracks.android.ui.map.MapActivity.Companion.STARTING_LATITUDE
import org.owntracks.android.ui.map.MapActivity.Companion.STARTING_LONGITUDE
import org.owntracks.android.ui.map.MapFragment
import timber.log.Timber

class OSMMapFragment : MapFragment() {
    private var mapView: MapView? = null
    private var binding: OsmMapFragmentBinding? = null
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        Configuration.getInstance().load(context, PreferenceManager.getDefaultSharedPreferences(context))
        binding = DataBindingUtil.inflate(inflater, R.layout.osm_map_fragment, container, false)
        ((requireActivity() as MapActivity).checkAndRequestLocationPermissions())
        mapView = this.binding!!.osmMapView.apply {
            setTileSource(TileSourceFactory.MAPNIK)
            zoomController.setVisibility(CustomZoomButtonsController.Visibility.SHOW_AND_FADEOUT)
            controller.setZoom(ZOOM_STREET_LEVEL)
            controller.setCenter(GeoPoint(STARTING_LATITUDE, STARTING_LONGITUDE))
            overlays.add(MyLocationNewOverlay(this))

            setMultiTouchControls(true)
            setOnClickListener {
                (activity as MapActivity).onMapClick()
            }
            setOnTouchListener { v, motionEvent ->
                if (motionEvent.action == ACTION_BUTTON_RELEASE) {
                    v.performClick()
                }
                (activity as MapActivity).onMapClick()
                false
            }
        }
        ((requireActivity()) as MapActivity).onMapReady()
        return binding!!.root
    }

    override fun clearMarkers() {
        mapView?.overlays?.clear()
    }

    override fun updateCamera(latLng: LatLng) {
        mapView?.controller?.run {
            setCenter(latLng.toGeoPoint())
        }
    }

    override fun updateMarker(id: String, latLng: LatLng) {
        mapView?.run {
            val existingMarker: Marker? = overlays.firstOrNull { it is Marker && it.id == id } as Marker?
            if (existingMarker != null) {
                existingMarker.position = latLng.toGeoPoint()
            } else {
                overlays.add(Marker(this).apply {
                    this.id = id
                    position = latLng.toGeoPoint()
                    infoWindow = null
                    setOnMarkerClickListener { marker, _ ->
                        (activity as MapActivity).onMarkerClicked(marker.id)
                        true
                    }
                    setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                })
            }
        }
    }

    override fun removeMarker(id: String) {
        mapView?.run {
            overlays.removeAll { it is Marker && it.id == id }
        }
    }

    override fun setMarkerImage(id: String, bitmap: Bitmap) {
        mapView?.run {
            overlays.firstOrNull { it is Marker && it.id == id }?.run {
                (this as Marker).icon = BitmapDrawable(resources, bitmap)
            }
        }
    }

    override fun locationPermissionGranted() {
        Timber.i("OSM Location permission granted")
    }

    override fun onResume() {
        mapView?.onResume()
        super.onResume()
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