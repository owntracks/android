package org.owntracks.android.ui.map

import org.owntracks.android.location.LatLng

/** Where to point the camera so that a set of points is in view. */
sealed interface FitTarget {
  /** Everything is in (practically) one place: center on it at [POINT_ZOOM]. */
  data class Point(val latLng: LatLng) : FitTarget

  /** Frame this box. */
  data class Bounds(val southWest: LatLng, val northEast: LatLng) : FitTarget

  companion object {
    /** Standard (OSM) zoom for a single point, about street level. */
    const val POINT_ZOOM = 15.0

    /**
     * Points spanning less than this, in degrees, count as one place (about 50 m of latitude).
     * Framing them as a box would zoom right in to the map's maximum.
     */
    private const val MIN_SPAN_DEGREES = 0.0005

    /** @return how to show all of [points], or null if there are none */
    fun of(points: Collection<LatLng>): FitTarget? {
      if (points.isEmpty()) return null
      val south = points.minOf { it.latitude.value }
      val north = points.maxOf { it.latitude.value }
      val west = points.minOf { it.longitude.value }
      val east = points.maxOf { it.longitude.value }
      return if (north - south < MIN_SPAN_DEGREES && east - west < MIN_SPAN_DEGREES) {
        Point(LatLng((south + north) / 2, (west + east) / 2))
      } else {
        Bounds(LatLng(south, west), LatLng(north, east))
      }
    }
  }
}
