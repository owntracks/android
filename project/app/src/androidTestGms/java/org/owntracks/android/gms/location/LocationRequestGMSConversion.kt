package org.owntracks.android.gms.location

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.owntracks.android.location.LocationRequest
import java.util.concurrent.TimeUnit

@RunWith(AndroidJUnit4::class)
@SmallTest
class LocationRequestGMSConversion {

    @Test
    fun canConvertLocationRequestToGMS() {
        val locationRequest = LocationRequest(
            fastestInterval = 1_000,
            smallestDisplacement = 50f,
            numUpdates = 50,
            expirationDuration = TimeUnit.MINUTES.toMillis(2),
            priority = LocationRequest.PRIORITY_HIGH_ACCURACY,
            interval = 30_000
        )

        val gmsLocationRequest = locationRequest.toGMSLocationRequest()
        assertEquals(1000, gmsLocationRequest.fastestInterval)
        assertEquals(30_000, gmsLocationRequest.interval)
        assertEquals(50, gmsLocationRequest.numUpdates)
        assertEquals(
            com.google.android.gms.location.LocationRequest.PRIORITY_HIGH_ACCURACY,
            gmsLocationRequest.priority
        )
        assertEquals(50f, gmsLocationRequest.smallestDisplacement)
        assertTrue(gmsLocationRequest.isFastestIntervalExplicitlySet)
    }

    @Test
    fun canConvertLocationRequestToGMSWithoutFastestIntervalSet() {
        val locationRequest = LocationRequest(
            smallestDisplacement = 50f,
            numUpdates = 50,
            expirationDuration = TimeUnit.MINUTES.toMillis(2),
            priority = LocationRequest.PRIORITY_HIGH_ACCURACY,
            interval = 30_000
        )

        val gmsLocationRequest = locationRequest.toGMSLocationRequest()
        assertFalse(gmsLocationRequest.isFastestIntervalExplicitlySet)
    }
}
