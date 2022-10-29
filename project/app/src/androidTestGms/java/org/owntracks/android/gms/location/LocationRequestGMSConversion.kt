package org.owntracks.android.gms.location

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import com.google.android.gms.location.Priority
import java.util.concurrent.TimeUnit
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.owntracks.android.location.LocationRequest

@RunWith(AndroidJUnit4::class)
@SmallTest
class LocationRequestGMSConversion {

    @Test
    fun canConvertLocationRequestToGMS() =
        LocationRequest(
            fastestInterval = 1_000,
            smallestDisplacement = 50f,
            numUpdates = 50,
            expirationDuration = TimeUnit.MINUTES.toMillis(2),
            priority = LocationRequest.PRIORITY_HIGH_ACCURACY,
            interval = 30_000
        )
            .toGMSLocationRequest()
            .run {
                assertEquals(1_000, minUpdateIntervalMillis)
                assertEquals(30_000, intervalMillis)
                assertEquals(50, maxUpdates)
                assertEquals(TimeUnit.MINUTES.toMillis(2), durationMillis)
                assertEquals(Priority.PRIORITY_HIGH_ACCURACY, priority)
                assertEquals(50f, minUpdateDistanceMeters)
            }

    @Test
    fun canConvertLocationRequestToGMSWithoutFastestIntervalSet() {
        LocationRequest(
            smallestDisplacement = 50f,
            numUpdates = 50,
            expirationDuration = TimeUnit.MINUTES.toMillis(2),
            priority = LocationRequest.PRIORITY_HIGH_ACCURACY,
            interval = 30_000
        )
            .toGMSLocationRequest()
            .run {
                assertEquals(30_000, minUpdateIntervalMillis)
                assertEquals(50, maxUpdates)
                assertEquals(Priority.PRIORITY_HIGH_ACCURACY, priority)
                assertEquals(TimeUnit.MINUTES.toMillis(2), durationMillis)
                assertEquals(50f, minUpdateDistanceMeters)
            }
    }
}
