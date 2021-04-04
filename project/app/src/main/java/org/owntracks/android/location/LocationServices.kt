package org.owntracks.android.location

import android.content.Context
import org.owntracks.android.BuildConfig.FLAVOR
import org.owntracks.android.gms.location.GMSLocationProviderClient
import org.owntracks.android.gms.location.geofencing.GMSGeofencingClient
import org.owntracks.android.location.geofencing.GeofencingClient
import org.owntracks.android.services.BackgroundService

object LocationServices {
    fun getGeofencingClient(backgroundService: BackgroundService): GeofencingClient {
        return when (FLAVOR) {
            "gms" -> GMSGeofencingClient.create(backgroundService)
            else -> NoopGeofencingClient()
        }
    }

    fun getLocationProviderClient(context: Context): LocationProviderClient {
        return when (FLAVOR) {
            "gms" -> GMSLocationProviderClient.create(context)
            else -> AospLocationProviderClient(context)
        }
    }
}