package org.owntracks.android.location

import android.location.Location
import kotlin.math.abs
import org.osmdroid.util.GeoPoint
import org.owntracks.android.location.geofencing.Latitude
import org.owntracks.android.location.geofencing.Longitude

class LatLng(private val _latitude: Latitude, private val _longitude: Longitude) {
    constructor(latitude: Double, longitude: Double) : this(Latitude(latitude), Longitude(longitude))
    val latitude: Double
        get() = _latitude.value
    val longitude
        get() = _longitude.value

    override fun toString(): String {
        return "LatLng $latitude, $longitude"
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is LatLng) return false
        return this.latitude.equalsDelta(other.latitude) && other.longitude.equalsDelta(this.longitude)
    }

    override fun hashCode(): Int {
        var result = latitude.hashCode()
        result = 31 * result + longitude.hashCode()
        return result
    }
}

fun LatLng.toGeoPoint(): GeoPoint {
    return GeoPoint(this.latitude, this.longitude)
}

fun Double.equalsDelta(other: Double) = abs(this / other - 1) < 0.00000001

fun Location.toLatLng() = LatLng(latitude, longitude)
