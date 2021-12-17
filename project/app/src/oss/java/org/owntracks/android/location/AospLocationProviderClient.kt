package org.owntracks.android.location

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import android.os.Looper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
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
        looper: Looper?
    ) {
        GlobalScope.launch {
            withContext(Dispatchers.Main) {
                gpsMyLocationProvider.stopLocationProvider()
                val listener = IMyLocationConsumer { location, _ ->
                    clientCallBack.onLocationResult(LocationResult(location))
                }
                gpsMyLocationProvider.clearLocationSources()
                when (locationRequest.priority) {

                    PRIORITY_HIGH_ACCURACY -> {
                        gpsMyLocationProvider.addLocationSource("gps")
                        gpsMyLocationProvider.addLocationSource("network")
                        gpsMyLocationProvider.addLocationSource("passive")
                    }
                    PRIORITY_BALANCED_POWER_ACCURACY -> {
                        gpsMyLocationProvider.addLocationSource("gps")
                        gpsMyLocationProvider.addLocationSource("network")
                        gpsMyLocationProvider.addLocationSource("passive")
                    }
                    else -> {
                        gpsMyLocationProvider.addLocationSource("network")
                        gpsMyLocationProvider.addLocationSource("passive")
                    }
                }
                gpsMyLocationProvider.locationUpdateMinTime = locationRequest.interval ?: 30_000
                gpsMyLocationProvider.locationUpdateMinDistance =
                    locationRequest.smallestDisplacement
                        ?: 10f
                gpsMyLocationProvider.startLocationProvider(listener)
                callbackMap[clientCallBack] = listener
            }
        }
    }

    override fun removeLocationUpdates(clientCallBack: LocationCallback) {
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