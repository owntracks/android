package org.owntracks.android.geocoding

internal interface Geocoder {
  suspend fun reverse(latitude: Double, longitude: Double): GeocodeResult
}
