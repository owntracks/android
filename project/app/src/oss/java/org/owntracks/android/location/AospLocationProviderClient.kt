package org.owntracks.android.location

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import android.os.Looper
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider
import org.osmdroid.views.overlay.mylocation.IMyLocationConsumer
import org.owntracks.android.location.LocationRequest.Companion.PRIORITY_BALANCED_POWER_ACCURACY
import org.owntracks.android.location.LocationRequest.Companion.PRIORITY_HIGH_ACCURACY
import timber.log.Timber

class AospLocationProviderClient(val context: Context) : LocationProviderClient() {
    private val callbackMap = mutableMapOf<LocationCallback, IMyLocationConsumer>()
    private val gpsMyLocationProvider = GpsMyLocationProvider(context)

    @SuppressLint("MissingPermission")
    override fun actuallyRequestLocationUpdates(
        locationRequest: LocationRequest,
        clientCallBack: LocationCallback,
        looper: Looper
    ) {
        val listener = IMyLocationConsumer { location, _ ->
            clientCallBack.onLocationResult(LocationResult(location))
        }
        val success = gpsMyLocationProvider.run {
            stopLocationProvider()
            clearLocationSources()
            when (locationRequest.priority) {
                PRIORITY_HIGH_ACCURACY -> {
                    addLocationSource("gps")
                    addLocationSource("network")
                    addLocationSource("passive")
                }
                PRIORITY_BALANCED_POWER_ACCURACY -> {
                    addLocationSource("gps")
                    addLocationSource("network")
                    addLocationSource("passive")
                }
                else -> {
                    addLocationSource("network")
                    addLocationSource("passive")
                }
            }
            locationUpdateMinTime = locationRequest.interval.toMillis()
            locationUpdateMinDistance = locationRequest.smallestDisplacement ?: 10f
            startLocationProvider(listener)
        }
        if (success) {
            callbackMap[clientCallBack] = listener
        }
    }

    override fun removeLocationUpdates(clientCallBack: LocationCallback) {
        Timber.v("removeLocationUpdates")
        callbackMap[clientCallBack]?.run {
            gpsMyLocationProvider.stopLocationProvider()
            callbackMap.remove(clientCallBack)
            callbackMap.forEach { entry -> gpsMyLocationProvider.startLocationProvider(entry.value) }
        }
    }

    override fun flushLocations() {
        Timber.i("Flush locations NOOP on AOSP location provider")
    }

    override fun getLastLocation(): Location? {
        return gpsMyLocationProvider.lastKnownLocation
    }

    init {
        Timber.i("Using AOSP as a location provider")
    }
}
