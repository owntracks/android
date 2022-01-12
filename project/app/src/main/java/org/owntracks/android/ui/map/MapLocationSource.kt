package org.owntracks.android.ui.map

import android.location.Location
import android.os.Looper
import org.owntracks.android.location.*
import timber.log.Timber
import java.util.*
import java.util.concurrent.TimeUnit

/**
 * A LocationSource for a map that activates a high accuracy location request and provides an API
 * similar to that of the [com.google.android.gms.maps.LocationSource] (activate / deactivate etc.)
 *
 * This sets up a callback for a location update request that both calls the location changed listener
 * that's been provided by a client, but can also call another supplied [LocationCallback]
 *
 * @property locationProviderClient the client used to request high accuracy location updates
 * @property locationUpdateCallback an additional [LocationCallback] to be called on location update
 */
open class MapLocationSource internal constructor(
    private val locationProviderClient: LocationProviderClient,
    private val locationUpdateCallback: LocationCallback
) {
    private var cachedOnLocationChangedListener: OnLocationChangedListener? = null
    private var callbackWrapper: LocationCallback? = null
    private var lastKnownLocation: Location? = null
    open fun activate(onLocationChangedListener: OnLocationChangedListener) {
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

    open fun reactivate() {
        Timber.d("Reactivating MapLocationSource with locationChangedListener=${cachedOnLocationChangedListener?.hashCode() ?: "none"}")
        cachedOnLocationChangedListener?.run(this::activate)
    }

    open fun deactivate() {
        Timber.d("Deactivating mapLocationSource with locationChangedListener=${cachedOnLocationChangedListener?.hashCode() ?: "none"}")
        callbackWrapper?.run(locationProviderClient::removeLocationUpdates)
    }

    open fun getLastKnownLocation(): Location? = lastKnownLocation
}