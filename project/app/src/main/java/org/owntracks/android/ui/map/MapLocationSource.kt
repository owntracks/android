package org.owntracks.android.ui.map

import android.os.Looper
import org.owntracks.android.location.*
import java.util.concurrent.TimeUnit

class MapLocationSource internal constructor(private val locationProviderClient: LocationProviderClient, private val locationUpdateCallback: LocationCallback) : LocationSource {
    override fun activate(onLocationChangedListener: LocationSource.OnLocationChangedListener) {
        locationProviderClient.requestLocationUpdates(
                LocationRequest(fastestInterval = 0, smallestDisplacement = 0f, priority = LocationRequest.PRIORITY_HIGH_ACCURACY, interval = TimeUnit.SECONDS.toMillis(1)),
                object : LocationCallback {
                    override fun onLocationResult(locationResult: LocationResult) {
                        onLocationChangedListener.onLocationChanged(locationResult.lastLocation)
                        locationUpdateCallback.onLocationResult(locationResult)
                    }

                    override fun onLocationAvailability(locationAvailability: LocationAvailability) {
                        locationUpdateCallback.onLocationAvailability(locationAvailability)
                    }
                },
                Looper.getMainLooper()
        )
    }

    override fun deactivate() {
        locationProviderClient.removeLocationUpdates(locationUpdateCallback)
    }
}