package org.owntracks.android.location

import android.annotation.SuppressLint
import android.content.Context
import android.os.Looper
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider
import org.osmdroid.views.overlay.mylocation.IMyLocationConsumer
import timber.log.Timber

class AospLocationProviderClient(val context: Context) : LocationProviderClient {
    private val callbackMap = mutableMapOf<LocationCallback, IMyLocationConsumer>()
    private val gpsMyLocationProvider = GpsMyLocationProvider(context)

    @SuppressLint("MissingPermission")
    override fun requestLocationUpdates(locationRequest: LocationRequest, clientCallBack: LocationCallback) {
        val listener = IMyLocationConsumer { location, _ -> clientCallBack.onLocationResult(LocationResult(location)) }
        gpsMyLocationProvider.startLocationProvider(listener)
        callbackMap[clientCallBack] = listener
    }

    @SuppressLint("MissingPermission")
    override fun requestLocationUpdates(locationRequest: LocationRequest, clientCallBack: LocationCallback, looper: Looper?) {
        requestLocationUpdates(locationRequest, clientCallBack)
    }

    override fun removeLocationUpdates(clientCallBack: LocationCallback) {
        callbackMap[clientCallBack]?.run {
            gpsMyLocationProvider.stopLocationProvider()
            callbackMap.remove(clientCallBack)
            callbackMap.forEach { entry -> gpsMyLocationProvider.startLocationProvider(entry.value) }
        }
    }

    override fun flushLocations() {
        Timber.d("Flush locations noop on AOSP")
    }

    init {
        Timber.i("Using AOSP as a location provider")
    }
}