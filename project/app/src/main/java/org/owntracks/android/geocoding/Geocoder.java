package org.owntracks.android.geocoding;

interface Geocoder {
    GeocodeResult reverse(double latitude, double longitude);
}
