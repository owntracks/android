package org.owntracks.android.ui.map

import android.location.Location
import android.os.Looper
import org.owntracks.android.location.*
import timber.log.Timber
import java.util.concurrent.TimeUnit

class MapLocationSource internal constructor(
    private val locationProviderClient: LocationProviderClient,
    private val locationUpdateCallback: LocationCallback
) : LocationSource {
    private lateinit var cachedOnLocationChangedListener: LocationSource.OnLocationChangedListener
    private lateinit var callbackWrapper: LocationCallback
    private var lastKnownLocation: Location? = null
    override fun activate(onLocationChangedListener: LocationSource.OnLocationChangedListener) {
        Timber.d("Activating mapLocationSource with client=${locationProviderClient}")
        cachedOnLocationChangedListener = onLocationChangedListener
        callbackWrapper = object : LocationCallback {
            override fun onLocationResult(locationResult: LocationResult) {
                Timber.d("MapLocationSource received locationResult $locationResult")
                lastKnownLocation = locationResult.lastLocation
                onLocationChangedListener.onLocationChanged(locationResult.lastLocation)
                locationUpdateCallback.onLocationResult(locationResult)
            }

            override fun onLocationAvailability(locationAvailability: LocationAvailability) {
                Timber.d("MapLocationSource calling onLocationAvailability with $locationAvailability")
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

    override fun reactivate() {
        if (this::cachedOnLocationChangedListener.isInitialized) {
            Timber.d("Reactivating MapLocationSource with cached locationChangedListener=${cachedOnLocationChangedListener.hashCode()}")
            activate(this.cachedOnLocationChangedListener)
        }
    }

    override fun deactivate() {
        Timber.d("Deactivating mapLocationSource with client=$locationProviderClient")
        if (this::callbackWrapper.isInitialized) {
            locationProviderClient.removeLocationUpdates(callbackWrapper)
        }
    }

    override fun getLastKnownLocation(): Location? = lastKnownLocation
}