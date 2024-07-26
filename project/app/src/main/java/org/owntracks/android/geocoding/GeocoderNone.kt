package org.owntracks.android.geocoding

import org.owntracks.android.location.LatLng

class GeocoderNone internal constructor() : Geocoder {
  override suspend fun reverse(latLng: LatLng): GeocodeResult {
    return GeocodeResult.Empty
  }
}
