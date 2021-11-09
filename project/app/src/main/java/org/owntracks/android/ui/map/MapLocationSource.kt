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
    private var cachedOnLocationChangedListener: LocationSource.OnLocationChangedListener? = null
    private var callbackWrapper: LocationCallback? = null
    private var lastKnownLocation: Location? = null
    override fun activate(onLocationChangedListener: LocationSource.OnLocationChangedListener) {
        Timber.d("Activating mapLocationSource with locationChangedListener=${onLocationChangedListener.hashCode()}")
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
        callbackWrapper?.run {
            locationProviderClient.requestLocationUpdates(
                LocationRequest(
                    smallestDisplacement = 1f,
                    priority = LocationRequest.PRIORITY_HIGH_ACCURACY,
                    interval = TimeUnit.SECONDS.toMillis(2)
                ),
                this,
                Looper.getMainLooper()
            )
        }
    }

    override fun reactivate() {
        Timber.d("Reactivating MapLocationSource with locationChangedListener=${cachedOnLocationChangedListener?.hashCode() ?: "none"}")
        cachedOnLocationChangedListener?.run(this::activate)
    }

    override fun deactivate() {
        Timber.d("Deactivating mapLocationSource with locationChangedListener=${cachedOnLocationChangedListener?.hashCode() ?: "none"}")
        callbackWrapper?.run(locationProviderClient::removeLocationUpdates)
    }

    override fun getLastKnownLocation(): Location? = lastKnownLocation
}