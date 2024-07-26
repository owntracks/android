package org.owntracks.android.location.geofencing

import org.owntracks.android.location.equalsDelta

data class Longitude(private val longitude: Double) {
  val value: Double
    get() =
        (longitude.mod(360.0)).let {
          when {
            it <= 180 -> it
            else -> it - 360
          }
        }

  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (other is Longitude && this.value.equalsDelta(other.value)) return true
    return false
  }

  override fun hashCode(): Int {
    return longitude.hashCode()
  }
}
