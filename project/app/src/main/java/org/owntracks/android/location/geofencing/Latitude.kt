package org.owntracks.android.location.geofencing

import org.owntracks.android.location.equalsDelta

data class Latitude(private val latitude: Double) {
  val value: Double
    get() =
        latitude.mod(360.0).let {
          when ( // Front side, upper, between 0 and 90, we just return the value
          it) {
            in (0.0..90.00) -> {
              it
            }
            // Front side, lower, we need to flip it round to the negative
            in (270.0..360.0) -> {
              it - 360.0
            }
            // Back side, we use mod for this and map the 90-270 range to 90-(-90)
            in (90.0..270.0) -> {
              180 - (it)
            }
            else -> {
              throw IllegalArgumentException("Invalid latitude: $latitude")
            }
          }
        }

  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (other is Latitude && this.value.equalsDelta(other.value)) return true
    return false
  }

  override fun hashCode(): Int {
    return latitude.hashCode()
  }
}
