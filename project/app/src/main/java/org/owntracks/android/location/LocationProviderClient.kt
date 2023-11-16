package org.owntracks.android.location

import android.location.Location
import android.os.Looper

abstract class LocationProviderClient {
    fun requestLocationUpdates(
        locationRequest: LocationRequest,
        clientCallBack: LocationCallback,
        looper: Looper
    ) {
        removeLocationUpdates(clientCallBack)
        actuallyRequestLocationUpdates(locationRequest, clientCallBack, looper)
    }

    abstract fun singleHighAccuracyLocation(clientCallBack: LocationCallback, looper: Looper)

    protected abstract fun actuallyRequestLocationUpdates(
        locationRequest: LocationRequest,
        clientCallBack: LocationCallback,
        looper: Looper
    )

    abstract fun removeLocationUpdates(clientCallBack: LocationCallback)
    abstract fun flushLocations()
    abstract fun getLastLocation(): Location?
}
