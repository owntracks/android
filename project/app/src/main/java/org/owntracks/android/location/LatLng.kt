package org.owntracks.android.location

import android.location.Location
import java.math.BigDecimal
import java.math.RoundingMode
import kotlin.math.abs
import org.osmdroid.util.GeoPoint
import org.owntracks.android.location.geofencing.Latitude
import org.owntracks.android.location.geofencing.Longitude
import org.owntracks.android.model.messages.MessageLocation
import org.owntracks.android.model.messages.MessageTransition

class LatLng(private val _latitude: Latitude, private val _longitude: Longitude) {
  constructor(latitude: Double, longitude: Double) : this(Latitude(latitude), Longitude(longitude))

  val latitude: Double
    get() = _latitude.value

  val longitude
    get() = _longitude.value

  override fun toString(): String {
    return "LatLng $latitude, $longitude"
  }

  fun toDisplayString(): String = "${latitude.roundForDisplay()}, ${longitude.roundForDisplay()}"

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

fun Double.equalsDelta(other: Double) = abs(this / other - 1) < 0.000001

fun Location.toLatLng() = LatLng(latitude, longitude)

fun MessageLocation.toLatLng() = LatLng(latitude, longitude)

fun MessageTransition.toLatLng() = LatLng(latitude, longitude)

fun Double.roundForDisplay(): String = BigDecimal(this).setScale(4, RoundingMode.HALF_UP).toString()
