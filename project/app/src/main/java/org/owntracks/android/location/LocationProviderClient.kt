package org.owntracks.android.location

import android.os.Looper

interface LocationProviderClient {
    fun requestLocationUpdates(locationRequest: LocationRequest, clientCallBack: LocationCallback)
    fun requestLocationUpdates(locationRequest: LocationRequest, clientCallBack: LocationCallback, looper: Looper?)
    fun removeLocationUpdates(clientCallBack: LocationCallback)
    fun flushLocations()
}