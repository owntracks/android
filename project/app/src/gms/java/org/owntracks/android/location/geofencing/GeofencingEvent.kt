package org.owntracks.android.location.geofencing

import android.content.Intent
import android.location.Location

data class GeofencingEvent(
    val errorCode: Int?,
    val geofenceTransition: Int?,
    val triggeringGeofences: List<Geofence>?,
    val triggeringLocation: Location?
) {
  fun hasError(): Boolean = errorCode != null && errorCode >= 0

  companion object {
    @JvmStatic
    fun fromIntent(intent: Intent): GeofencingEvent {
      val gmsGeofencingEvent = com.google.android.gms.location.GeofencingEvent.fromIntent(intent)
      return GeofencingEvent(
          gmsGeofencingEvent?.errorCode,
          gmsGeofencingEvent?.geofenceTransition,
          gmsGeofencingEvent?.triggeringGeofences?.map {
            Geofence(
                it.requestId,
                it.transitionTypes,
                it.notificationResponsiveness,
                Latitude(it.latitude),
                Longitude(it.longitude),
                it.radius,
                it.expirationTime,
                it.loiteringDelay)
          },
          gmsGeofencingEvent?.triggeringLocation)
    }
  }
}
