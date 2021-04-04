package org.owntracks.android.ui.map.osm

import android.graphics.Bitmap
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.preference.PreferenceManager
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.views.CustomZoomButtonsController
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay
import org.owntracks.android.R
import org.owntracks.android.databinding.OsmMapFragmentBinding
import org.owntracks.android.location.LatLng
import org.owntracks.android.location.toGeoPoint
import org.owntracks.android.ui.map.MapActivity
import org.owntracks.android.ui.map.MapFragment
import timber.log.Timber

class OSMMapFragment : MapFragment() {
    private var mapView: MapView? = null
    private var binding: OsmMapFragmentBinding? = null
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        Configuration.getInstance().load(context, PreferenceManager.getDefaultSharedPreferences(context))
        binding = DataBindingUtil.inflate(inflater, R.layout.osm_map_fragment, container, false)
        mapView = this.binding!!.osmMapView.apply {
            setTileSource(TileSourceFactory.MAPNIK)
            zoomController.setVisibility(CustomZoomButtonsController.Visibility.SHOW_AND_FADEOUT)
            controller.setZoom(9.0)

            overlays.add(MyLocationNewOverlay(this))

            setMultiTouchControls(true)
            setOnClickListener {
                Timber.i("CLCIKY")
                (activity as MapActivity).onMapClick()
            }
            setOnTouchListener { v, _ ->
                v.performClick()
                false
            }
            setOnDragListener { _, _ ->
                (activity as MapActivity).onMapClick()
                false
            }
        }


        return binding!!.root
    }

    override fun clearMarkers() {
        mapView?.overlays?.clear()
    }

    override fun updateCamera(latLng: LatLng) {
        mapView?.controller?.run {
            setCenter(latLng.toGeoPoint())
            setZoom(ZOOM_STREET_LEVEL)
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
                    setOnClickListener { (activity as MapActivity).onMarkerClicked(id) }
                    setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                })
            }
        }
    }

    override fun removeMarker(id: String) {
    }

    override fun setMarkerImage(id: String, bitmap: Bitmap) {

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