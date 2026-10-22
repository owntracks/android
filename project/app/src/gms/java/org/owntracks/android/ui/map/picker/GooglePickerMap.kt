package org.owntracks.android.ui.map.picker

import android.content.Context
import android.content.res.Configuration
import android.os.Bundle
import android.view.View
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.MapView
import com.google.android.gms.maps.model.MapStyleOptions
import org.owntracks.android.R
import org.owntracks.android.gms.location.toGMSLatLng
import org.owntracks.android.location.LatLng
import org.owntracks.android.ui.map.MapLayerStyle

/** A [PickerMap] showing Google Maps, in the given [mapLayerStyle] */
class GooglePickerMap(private val context: Context, private val mapLayerStyle: MapLayerStyle) :
    PickerMap {
  private val mapView = MapView(context)
  private var googleMap: GoogleMap? = null

  /** Where to move to once the map is ready, as it's loaded asynchronously */
  private var pendingMove: Pair<LatLng, Double>? = null
  private var onMove: () -> Unit = {}
  private var onUserMove: () -> Unit = {}

  override val view: View = mapView

  override val center: LatLng
    get() =
        googleMap?.cameraPosition?.target?.run { LatLng(latitude, longitude) }
            ?: pendingMove?.first
            ?: LatLng(0.0, 0.0)

  override val zoom: Double
    get() =
        googleMap?.cameraPosition?.zoom?.toDouble()
            ?: pendingMove?.second
            ?: PickerMap.MIN_ZOOM_LEVEL

  override fun moveTo(latLng: LatLng, zoom: Double) {
    googleMap?.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng.toGMSLatLng(), zoom.toFloat()))
        ?: run { pendingMove = latLng to zoom }
  }

  override fun setOnMoveListener(onMove: () -> Unit) {
    this.onMove = onMove
  }

  override fun setOnUserMoveListener(onUserMove: () -> Unit) {
    this.onUserMove = onUserMove
  }

  override fun onCreate(savedInstanceState: Bundle?) {
    mapView.onCreate(savedInstanceState?.getBundle(MAP_VIEW_STATE))
    mapView.getMapAsync { map ->
      googleMap = map.apply {
        // As the main map has them
        mapType =
            when (mapLayerStyle) {
              MapLayerStyle.GoogleMapHybrid -> GoogleMap.MAP_TYPE_HYBRID
              MapLayerStyle.GoogleMapSatellite -> GoogleMap.MAP_TYPE_SATELLITE
              MapLayerStyle.GoogleMapTerrain -> GoogleMap.MAP_TYPE_TERRAIN
              else -> GoogleMap.MAP_TYPE_NORMAL
            }
        if (
            context.resources.configuration.uiMode.and(Configuration.UI_MODE_NIGHT_MASK) ==
                Configuration.UI_MODE_NIGHT_YES
        ) {
          setMapStyle(MapStyleOptions.loadRawResourceStyle(context, R.raw.google_maps_night_theme))
        }
        uiSettings.isCompassEnabled = false
        uiSettings.isRotateGesturesEnabled = false
        uiSettings.isTiltGesturesEnabled = false
        setMinZoomPreference(PickerMap.MIN_ZOOM_LEVEL.toFloat())
        setOnCameraMoveListener { onMove() }
        setOnCameraMoveStartedListener { reason ->
          if (reason == GoogleMap.OnCameraMoveStartedListener.REASON_GESTURE) onUserMove()
        }
      }
      pendingMove?.let { (latLng, zoom) -> moveTo(latLng, zoom) }
      pendingMove = null
      onMove()
    }
  }

  override fun onResume() = mapView.onResume()

  override fun onPause() = mapView.onPause()

  override fun onDestroy() = mapView.onDestroy()

  override fun onSaveInstanceState(outState: Bundle) {
    outState.putBundle(MAP_VIEW_STATE, Bundle().also(mapView::onSaveInstanceState))
  }

  companion object {
    private const val MAP_VIEW_STATE = "googleMapView"
  }
}
