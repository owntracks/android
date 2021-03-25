package org.owntracks.android.gms.location

import android.content.Context
import com.google.android.gms.location.LocationServices
import org.owntracks.android.location.LocationProviderClient

class GMSLocationServices {
    companion object {
        fun getLocationProviderClient(context: Context): LocationProviderClient {
            return GMSLocationProviderClient(LocationServices.getFusedLocationProviderClient(context))
        }
    }
}