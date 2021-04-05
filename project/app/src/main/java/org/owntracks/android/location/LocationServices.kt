package org.owntracks.android.location

import android.content.Context
import org.owntracks.android.BuildConfig.FLAVOR
import org.owntracks.android.gms.location.GMSLocationProviderClient
import org.owntracks.android.gms.location.geofencing.GMSGeofencingClient
import org.owntracks.android.location.geofencing.GeofencingClient
import org.owntracks.android.services.BackgroundService
import org.owntracks.android.support.Preferences
import org.owntracks.android.support.Preferences.Companion.EXPERIMENTAL_FEATURE_USE_AOSP_LOCATION_PROVIDER

object LocationServices {
    fun getGeofencingClient(backgroundService: BackgroundService): GeofencingClient {
        return when (FLAVOR) {
            "gms" -> GMSGeofencingClient.create(backgroundService)
            else -> NoopGeofencingClient()
        }
    }

    fun getLocationProviderClient(context: Context, preferences: Preferences): LocationProviderClient {
        return if (preferences.isExperimentalFeatureEnabled(EXPERIMENTAL_FEATURE_USE_AOSP_LOCATION_PROVIDER)) {
            AospLocationProviderClient(context)
        } else {
            when (FLAVOR) {
                "gms" -> GMSLocationProviderClient.create(context)
                else -> AospLocationProviderClient(context)
            }
        }
    }
}