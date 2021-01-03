package org.owntracks.android.location

import android.content.Context
import com.google.android.gms.location.LocationServices
import org.owntracks.android.location.gms.GMSLocationProviderClient

class LocationServices {
    companion object {
        fun getLocationProviderClient(context: Context): LocationProviderClient {
            return GMSLocationProviderClient(LocationServices.getFusedLocationProviderClient(context))
        }
    }
}