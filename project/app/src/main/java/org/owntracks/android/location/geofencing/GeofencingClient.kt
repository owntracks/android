package org.owntracks.android.location.geofencing

import android.app.PendingIntent

interface GeofencingClient {
    fun removeGeofences(GeofencePendingIntent: PendingIntent)
    fun addGeofences(request: GeofencingRequest, GeofencePendingIntent: PendingIntent)
}
