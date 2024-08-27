package org.owntracks.android.ui.map

import org.owntracks.android.location.LatLng

/**
 * Represents a camera view on a map. Location and zoom level. Zoom uses standrard (OSM) zoom units
 *
 * @property latLng location of the view
 * @property zoom zoom level of the view
 * @property rotation rotation angle of the view
 * @constructor Create empty Map location and zoom level
 */
data class MapLocationZoomLevelAndRotation(
    val latLng: LatLng,
    val zoom: Double,
    val rotation: Float = 0f
)
