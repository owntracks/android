package org.owntracks.android.geocoding

class GeocoderNone internal constructor() : Geocoder {
  override suspend fun reverse(latitude: Double, longitude: Double): GeocodeResult {
    return GeocodeResult.Empty
  }
}
