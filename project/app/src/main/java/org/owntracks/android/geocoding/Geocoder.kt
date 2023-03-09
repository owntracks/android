package org.owntracks.android.geocoding

internal interface Geocoder {
    fun reverse(latitude: Double, longitude: Double): GeocodeResult
}
