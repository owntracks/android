package org.owntracks.android.ui.map.picker

import android.annotation.SuppressLint
import android.content.Context
import android.view.View
import org.osmdroid.events.MapListener
import org.osmdroid.events.ScrollEvent
import org.osmdroid.events.ZoomEvent
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.views.CustomZoomButtonsController
import org.osmdroid.views.MapView
import org.owntracks.android.location.LatLng
import org.owntracks.android.location.toGeoPoint
import org.owntracks.android.preferences.Preferences
import org.owntracks.android.ui.map.MapLayerStyle
import org.owntracks.android.ui.map.osm.configureOsmdroid
import org.owntracks.android.ui.map.osm.setTilesForUiMode

/** A [PickerMap] showing OpenStreetMap */
class OsmPickerMap(context: Context, preferences: Preferences) : PickerMap {
  private val mapView = run {
    configureOsmdroid(context)
    MapView(context)
  }
      .apply {
        setTileSource(
            if (preferences.mapLayerStyle == MapLayerStyle.OpenStreetMapWikimedia) {
              TileSourceFactory.WIKIMEDIA
            } else {
              TileSourceFactory.MAPNIK
            }
        )
        setTilesForUiMode(context.resources.configuration)
        setMultiTouchControls(true)
        zoomController.setVisibility(CustomZoomButtonsController.Visibility.NEVER)
        minZoomLevel = PickerMap.MIN_ZOOM_LEVEL
        maxZoomLevel = PickerMap.MAX_ZOOM_LEVEL
        tilesScaleFactor = preferences.osmTileScaleFactor
        controller.setZoom(PickerMap.MIN_ZOOM_LEVEL)
      }

  override val view: View = mapView

  override val center: LatLng
    get() = mapView.mapCenter.run { LatLng(latitude, longitude) }

  override val zoom: Double
    get() = mapView.zoomLevelDouble

  override fun moveTo(latLng: LatLng, zoom: Double) {
    mapView.controller.setZoom(zoom)
    mapView.controller.setCenter(latLng.toGeoPoint())
  }

  override fun setOnMoveListener(onMove: () -> Unit) {
    mapView.addMapListener(
        object : MapListener {
          override fun onScroll(event: ScrollEvent?): Boolean {
            onMove()
            return false
          }

          override fun onZoom(event: ZoomEvent?): Boolean = false
        }
    )
  }

  @SuppressLint("ClickableViewAccessibility")
  override fun setOnUserMoveListener(onUserMove: () -> Unit) {
    mapView.setOnTouchListener { _, _ ->
      onUserMove()
      false
    }
  }

  override fun onResume() = mapView.onResume()

  override fun onPause() = mapView.onPause()
}
