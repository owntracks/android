package org.owntracks.android.location

import org.junit.Assert.assertEquals
import org.junit.Test

class TestLatLng {
    @Test
    fun `given a LatLng, when converting to a GeoPoint, then the resulting latlng values are the same as the input`() {
        val lat = 58.3584
        val long = 2.4857
        val latLng = LatLng(lat, long)
        val geopoint = latLng.toGeoPoint()
        assertEquals(lat, geopoint.latitude, 0.0001)
        assertEquals(long, geopoint.longitude, 0.0001)
    }

    @Test
    fun `given invalid northern latitude and overflow longitude values, when creating a LatLng, the resulting object wraps around`() {
        val lat = 110.0
        val long = 370.0
        val latLng = LatLng(lat, long)
        assertEquals(70.0, latLng.latitude, 0.0)
        assertEquals(10.0, latLng.longitude, 0.0)
    }

    @Test
    fun `given invalid southern latitude and overflow longitude values, when creating a LatLng, the resulting object wraps around`() {
        val lat = 200.0
        val long = 330.0
        val latLng = LatLng(lat, long)
        assertEquals(-20.0, latLng.latitude, 0.0)
        assertEquals(-30.0, latLng.longitude, 0.0)
    }

    @Test
    fun `given southern latitude and longitude values, when creating a LatLng, the resulting object wraps around`() {
        val lat = 271.0
        val long = 330.0
        val latLng = LatLng(lat, long)
        assertEquals(-89.0, latLng.latitude, 0.0)
        assertEquals(-30.0, latLng.longitude, 0.0)
    }

    @Test
    fun `given overflow northern latitude and longitude values, when creating a LatLng, the resulting object wraps around`() {
        val lat = 375.0
        val long = 370.0
        val latLng = LatLng(lat, long)
        assertEquals(15.0, latLng.latitude, 0.0)
        assertEquals(10.0, latLng.longitude, 0.0)
    }
}