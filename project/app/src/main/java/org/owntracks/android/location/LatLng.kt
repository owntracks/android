package org.owntracks.android.location

import android.location.Location
import org.osmdroid.util.GeoPoint
import kotlin.math.abs


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


fun Double.equalsDelta(other: Double) = abs(this/other - 1) < 0.00000001

fun Location.toLatLng() = LatLng(latitude,longitude)