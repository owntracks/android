package org.owntracks.android.location

import android.app.PendingIntent
import org.owntracks.android.location.geofencing.GeofencingClient
import org.owntracks.android.location.geofencing.GeofencingRequest

class NoopGeofencingClient : GeofencingClient {
    override fun removeGeofences(GeofencePendingIntent: PendingIntent) {
    }

    override fun addGeofences(request: GeofencingRequest, GeofencePendingIntent: PendingIntent) {
    }
}
