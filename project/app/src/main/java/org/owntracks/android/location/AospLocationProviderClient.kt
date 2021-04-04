package org.owntracks.android.location

import android.annotation.SuppressLint
import android.content.Context
import android.location.LocationListener
import android.location.LocationManager
import android.os.Looper
import timber.log.Timber

class AospLocationProviderClient(val context: Context) : LocationProviderClient {
    private val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
    private val callbackMap = mutableMapOf<LocationCallback, LocationListener>()

    @SuppressLint("MissingPermission")
    override fun requestLocationUpdates(locationRequest: LocationRequest, clientCallBack: LocationCallback) {
        val listener = LocationListener { location -> clientCallBack.onLocationResult(LocationResult(location)) }
        locationManager.requestLocationUpdates("gps", locationRequest.interval, locationRequest.smallestDisplacement, listener)
        callbackMap[clientCallBack] = listener
    }

    @SuppressLint("MissingPermission")
    override fun requestLocationUpdates(locationRequest: LocationRequest, clientCallBack: LocationCallback, looper: Looper?) {
        val listener = LocationListener { location -> clientCallBack.onLocationResult(LocationResult(location)) }
        locationManager.requestLocationUpdates("gps", locationRequest.interval, locationRequest.smallestDisplacement, listener, looper)
        callbackMap[clientCallBack] = listener
    }

    override fun removeLocationUpdates(clientCallBack: LocationCallback) {
        callbackMap[clientCallBack]?.run {
            locationManager.removeUpdates(this)
            callbackMap.remove(clientCallBack)
        }
    }

    override fun flushLocations() {
        Timber.d("Flush locations noop on AOSP")
    }
}