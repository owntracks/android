package org.owntracks.android.ui.map

import android.os.Looper
import org.owntracks.android.location.*
import timber.log.Timber
import java.util.concurrent.TimeUnit

class MapLocationSource internal constructor(
    private val locationProviderClient: LocationProviderClient,
    private val locationUpdateCallback: LocationCallback
) : LocationSource {
    private lateinit var callbackWrapper: LocationCallback
    override fun activate(onLocationChangedListener: LocationSource.OnLocationChangedListener) {
        Timber.tag("873432").d("Activating mapLocationSource with client=${locationProviderClient}")
        callbackWrapper = object : LocationCallback {
            override fun onLocationResult(locationResult: LocationResult) {
                Timber.tag("873432").d("MapLocationSource recevied locationResult $locationResult")
                onLocationChangedListener.onLocationChanged(locationResult.lastLocation)
                locationUpdateCallback.onLocationResult(locationResult)
            }

            override fun onLocationAvailability(locationAvailability: LocationAvailability) {
                Timber.tag("873432")
                    .d("MapLocationSource calling onLocationAvailability with $locationAvailability")
                locationUpdateCallback.onLocationAvailability(locationAvailability)
            }
        }
        locationProviderClient.requestLocationUpdates(
            LocationRequest(
                smallestDisplacement = 1f,
                priority = LocationRequest.PRIORITY_HIGH_ACCURACY,
                interval = TimeUnit.SECONDS.toMillis(2)
            ),
            callbackWrapper,
            Looper.getMainLooper()
        )
    }

    override fun deactivate() {
        Timber.tag("873432").d("Deactivating mapLocationSource with client=$locationProviderClient")
        locationProviderClient.removeLocationUpdates(callbackWrapper)
    }
}