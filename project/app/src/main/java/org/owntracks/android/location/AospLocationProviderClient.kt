package org.owntracks.android.location

import android.os.Looper

class AospLocationProviderClient : LocationProviderClient {
    override fun requestLocationUpdates(locationRequest: LocationRequest, clientCallBack: LocationCallback) {
        TODO("Not yet implemented")
    }

    override fun requestLocationUpdates(locationRequest: LocationRequest, clientCallBack: LocationCallback, looper: Looper?) {
        TODO("Not yet implemented")
    }

    override fun removeLocationUpdates(clientCallBack: LocationCallback) {
        TODO("Not yet implemented")
    }

    override fun flushLocations() {
        TODO("Not yet implemented")
    }
}