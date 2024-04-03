package org.owntracks.android.location.geofencing

import android.content.Intent
import android.location.Location

data class GeofencingEvent(
    val errorCode: Int?,
    val geofenceTransition: Int?,
    val triggeringGeofences: List<Geofence>?,
    val triggeringLocation: Location?
) {
  fun hasError(): Boolean = true

  // OSS doesn't support triggering geofences with events, so we just return a stub with an error
  // code set
  companion object {
    @JvmStatic
    fun fromIntent(@Suppress("UNUSED_PARAMETER") intent: Intent): GeofencingEvent {
      return GeofencingEvent(1, null, null, null)
    }
  }
}
