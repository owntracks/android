package org.owntracks.android.location

import org.osmdroid.util.GeoPoint

data class LatLng(val latitude: Double, val longitude: Double)

fun LatLng.toGeoPoint(): GeoPoint {
    return GeoPoint(this.latitude, this.longitude)
}


