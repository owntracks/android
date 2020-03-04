package org.owntracks.android.support;

public class GeocoderNone implements Geocoder {
    GeocoderNone() {

    }

    public String reverse(double latitude, double longitude) {
        return (latitude + " : " + longitude);
    }

}
