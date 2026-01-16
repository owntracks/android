package org.owntracks.android.ui.map

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.toArgb
import com.google.maps.android.compose.Circle
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import org.owntracks.android.data.waypoints.WaypointModel
import org.owntracks.android.gms.location.toGMSLatLng
import org.owntracks.android.location.toLatLng

/**
 * Renders waypoint regions on the Google Map as circles with markers. Each waypoint is displayed as
 * a filled circle (geofence area) with a marker at the center showing the description.
 *
 * @param waypoints Collection of waypoints to display
 * @param showRegions Whether to show regions on the map (from preferences)
 */
@Composable
fun RegionOverlays(waypoints: Collection<WaypointModel>, showRegions: Boolean) {
  if (!showRegions) return

  val regionColor = rememberRegionColor()

  waypoints.forEach { waypoint -> RegionOverlay(waypoint = waypoint, fillColor = regionColor.toArgb()) }
}

/**
 * Renders a single waypoint region as a circle with a marker.
 *
 * @param waypoint The waypoint to display
 * @param fillColor The fill color for the circle (with alpha)
 */
@Composable
private fun RegionOverlay(waypoint: WaypointModel, fillColor: Int) {
  val position = waypoint.getLocation().toLatLng().toGMSLatLng()

  // Draw the geofence circle
  Circle(
      center = position,
      radius = waypoint.geofenceRadius.toDouble(),
      fillColor = androidx.compose.ui.graphics.Color(fillColor),
      strokeWidth = 1f,
      strokeColor = androidx.compose.ui.graphics.Color(fillColor).copy(alpha = 0.8f))

  // Draw the marker at the center with the description
  Marker(
      state = MarkerState(position = position),
      title = waypoint.description,
      anchor = androidx.compose.ui.geometry.Offset(0.5f, 1.0f))
}
