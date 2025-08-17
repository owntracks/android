package org.owntracks.android.ui.map.maplibre

import android.graphics.Bitmap
import org.owntracks.android.data.waypoints.WaypointModel
import org.owntracks.android.databinding.MaplibreFragmentBinding
import org.owntracks.android.location.LatLng
import org.owntracks.android.preferences.Preferences
import org.owntracks.android.support.ContactImageBindingAdapter
import org.owntracks.android.ui.map.MapFragment
import org.owntracks.android.ui.map.MapLayerStyle

class MapLibreFragment internal constructor(
  private val preferences: Preferences,
  contactImageBindingAdapter: ContactImageBindingAdapter
): MapFragment<MaplibreFragmentBinding>(contactImageBindingAdapter, preferences) {
  override val layout: Int
    get() = TODO("Not yet implemented")

  override fun updateCamera(latLng: LatLng) {
    TODO("Not yet implemented")
  }

  override fun updateMarkerOnMap(
    id: String,
    latLng: LatLng,
    image: Bitmap
  ) {
    TODO("Not yet implemented")
  }

  override fun removeMarkerFromMap(id: String) {
    TODO("Not yet implemented")
  }

  override fun currentMarkersOnMap(): Set<String> {
    TODO("Not yet implemented")
  }

  override fun initMap() {
    TODO("Not yet implemented")
  }

  override fun reDrawRegions(regions: Set<WaypointModel>) {
    TODO("Not yet implemented")
  }

  override fun addRegion(waypoint: WaypointModel) {
    TODO("Not yet implemented")
  }

  override fun deleteRegion(waypoint: WaypointModel) {
    TODO("Not yet implemented")
  }

  override fun updateRegion(waypoint: WaypointModel) {
    TODO("Not yet implemented")
  }

  override fun setMapLayerType(mapLayerStyle: MapLayerStyle) {
    TODO("Not yet implemented")
  }
}
