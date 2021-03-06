package org.owntracks.android.geocoding

class GeocoderNone internal constructor() : Geocoder {
    override fun reverse(latitude: Double, longitude: Double): GeocodeResult {
        return GeocodeResult.Empty
    }
}