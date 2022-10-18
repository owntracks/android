package org.owntracks.android.gms.location.geofencing

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.Context
import com.google.android.gms.location.LocationServices
import org.owntracks.android.location.geofencing.GeofencingClient
import org.owntracks.android.location.geofencing.GeofencingRequest

class GMSGeofencingClient(private val geofencingClient: com.google.android.gms.location.GeofencingClient) :
    GeofencingClient {
    override fun removeGeofences(GeofencePendingIntent: PendingIntent) {
        this.geofencingClient.removeGeofences(GeofencePendingIntent)
    }

    @SuppressLint("MissingPermission")
    override fun addGeofences(request: GeofencingRequest, GeofencePendingIntent: PendingIntent) {
        this.geofencingClient.addGeofences(request.toGMSGeofencingRequest(), GeofencePendingIntent)
    }

    companion object {
        fun create(context: Context): GeofencingClient {
            return GMSGeofencingClient(LocationServices.getGeofencingClient(context))
        }
    }
}
