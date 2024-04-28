package org.owntracks.android.gms.location.geofencing

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith
import org.owntracks.android.location.geofencing.Geofence
import org.owntracks.android.location.geofencing.GeofencingRequest
import org.owntracks.android.location.geofencing.Latitude
import org.owntracks.android.location.geofencing.Longitude

@RunWith(AndroidJUnit4::class)
@SmallTest
class GeofencingRequestGMSConversion {

  @Test
  fun canConvertGeofenceRequestToGMS() {
    val geofences =
        listOf(
            Geofence(
                requestId = "first",
                expirationDuration = 1000L,
                circularLatitude = Latitude(51.0),
                circularLongitude = Longitude(0.0),
                circularRadius = 100f,
                transitionTypes = Geofence.GEOFENCE_TRANSITION_DWELL,
                loiteringDelay = 20),
            Geofence(
                requestId = "second",
                expirationDuration = 500000L,
                circularLatitude = Latitude(53.8573),
                circularLongitude = Longitude(4.83487),
                circularRadius = 20.5f,
                transitionTypes = Geofence.GEOFENCE_TRANSITION_ENTER))

    val geofencingRequest = GeofencingRequest(geofences = geofences, initialTrigger = 5)
    val gmsGeofencingRequest = geofencingRequest.toGMSGeofencingRequest()
    assertEquals(2, gmsGeofencingRequest.geofences.size)
    assertEquals(5, gmsGeofencingRequest.initialTrigger)
    assertNotNull(gmsGeofencingRequest.geofences.find { it.requestId == "first" })
    assertNotNull(gmsGeofencingRequest.geofences.find { it.requestId == "second" })
  }

  @Test
  fun canConvertGeofenceToGMS() {
    val geofence =
        Geofence(
            requestId = "first",
            expirationDuration = 1000L,
            circularLatitude = Latitude(51.0),
            circularLongitude = Longitude(0.0),
            circularRadius = 100f,
            transitionTypes = Geofence.GEOFENCE_TRANSITION_DWELL,
            loiteringDelay = 20)
    val gmsGeofence = geofence.toGMSGeofence()
    assertEquals("first", gmsGeofence.requestId)
  }

  @Test
  fun canConvertGeofenceWithWeirdlongitudeToGMS() {
    val geofence =
        Geofence(
            requestId = "first",
            expirationDuration = 1000L,
            circularLatitude = Latitude(51.0),
            circularLongitude = Longitude(1234.0),
            circularRadius = 100f,
            transitionTypes = Geofence.GEOFENCE_TRANSITION_DWELL,
            loiteringDelay = 20)
    val gmsGeofence = geofence.toGMSGeofence()
    assertEquals("first", gmsGeofence.requestId)
  }
}
