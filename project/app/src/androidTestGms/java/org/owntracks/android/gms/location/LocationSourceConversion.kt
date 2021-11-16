package org.owntracks.android.gms.location

import android.location.Location
import android.os.Looper
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.owntracks.android.location.*
import org.owntracks.android.ui.map.MapLocationSource

@RunWith(AndroidJUnit4::class)
@SmallTest
class LocationSourceConversion {

    @Test
    fun canConvertLocationSourceToGMS() {
        var activateCalled = false
        var gmsActivateCalled = false
        var deactivateCalled = false
        val locationProviderClient = NoopLocationProviderClient()
        val locationUpdateCallback = NoopLocationUpdateCallback()
        val locationSource =
            object : MapLocationSource(locationProviderClient, locationUpdateCallback) {
                override fun activate(onLocationChangedListener: OnLocationChangedListener) {
                    activateCalled = true
                    onLocationChangedListener.onLocationChanged(Location("test"))
                }

                override fun reactivate() {
                }

                override fun deactivate() {
                    deactivateCalled = true
                }

                override fun getLastKnownLocation(): Location? = null
            }

        val gmsLocationSource = locationSource.toGMSLocationSource()
        gmsLocationSource.activate { gmsActivateCalled = true }
        gmsLocationSource.deactivate()
        assertTrue("Activate was called", activateCalled)
        assertTrue("GMS activation was called", gmsActivateCalled)
        assertTrue("Deactivate called", deactivateCalled)
    }

    class NoopLocationProviderClient : LocationProviderClient() {
        override fun actuallyRequestLocationUpdates(
            locationRequest: LocationRequest,
            clientCallBack: LocationCallback,
            looper: Looper?
        ) {
        }

        override fun removeLocationUpdates(clientCallBack: LocationCallback) {}
        override fun flushLocations() {}
        override fun getLastLocation(): Location? = null
    }

    class NoopLocationUpdateCallback : LocationCallback {
        override fun onLocationResult(locationResult: LocationResult) {}
        override fun onLocationAvailability(locationAvailability: LocationAvailability) {}
    }
}