package org.owntracks.android.data.waypoints

import android.location.Location
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.time.Instant

@Entity(indices = [Index(value = ["tst"], unique = true)])
data class WaypointModel(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    var description: String = "",
    var geofenceLatitude: Double = 0.0,
    var geofenceLongitude: Double = 0.0,
    var geofenceRadius: Int = 0,
    var lastTriggered: Instant = Instant.MIN,
    var lastTransition: Int = 0,
    val tst: Instant = Instant.now()

) {
    fun getLocation(): Location = Location("waypoint").apply {
        latitude = geofenceLatitude
        longitude = geofenceLongitude
        accuracy = geofenceRadius.toFloat()
    }

    fun isUnknown(): Boolean = lastTransition == 0
}
