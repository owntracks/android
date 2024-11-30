package org.owntracks.android.data.waypoints

import android.location.Location
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.time.Instant
import org.owntracks.android.location.geofencing.Geofence
import org.owntracks.android.location.geofencing.Latitude
import org.owntracks.android.location.geofencing.Longitude

@Entity(indices = [Index(value = ["tst"], unique = true)])
data class WaypointModel(
    // Needs to be a var so that we can update inserted models with the right ID
    @PrimaryKey(autoGenerate = true) var id: Long = 0,
    var description: String = "",
    var geofenceLatitude: Latitude = Latitude(0.0),
    var geofenceLongitude: Longitude = Longitude(0.0),
    var geofenceRadius: Int = 0,
    var lastTriggered: Instant? = null,
    var lastTransition: Int = 0,
    val tst: Instant = Instant.now()
) {
  fun getLocation(): Location =
      Location("waypoint").apply {
        latitude = geofenceLatitude.value
        longitude = geofenceLongitude.value
        accuracy = geofenceRadius.toFloat()
      }

  fun isUnknown(): Boolean = lastTransition == Geofence.GEOFENCE_TRANSITION_UNKNOWN
}
