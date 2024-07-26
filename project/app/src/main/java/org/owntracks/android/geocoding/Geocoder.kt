package org.owntracks.android.geocoding

import org.owntracks.android.location.LatLng

internal interface Geocoder {
  suspend fun reverse(latLng: LatLng): GeocodeResult
}
