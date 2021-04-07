package org.owntracks.android.location

import org.osmdroid.util.GeoPoint


class LatLng(latitude: Double, longitude: Double) {
    private val actualLatitude: Double = when {
        latitude % 360 <= 90 -> {
            latitude % 360
        }
        latitude % 360 <= 270 -> {
            180 + (-1 * (latitude % 360))
        }
        else -> -360 + (latitude % 360)
    }
    private val actualLongitude: Double = ((longitude + 180) % 360) - 180

    val latitude: Double
        get() = actualLatitude
    val longitude
        get() = actualLongitude


}

fun LatLng.toGeoPoint(): GeoPoint {
    return GeoPoint(this.latitude, this.longitude)
}


