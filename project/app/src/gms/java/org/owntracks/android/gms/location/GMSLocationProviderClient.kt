package org.owntracks.android.gms.location

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import android.os.Looper
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationAvailability
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.tasks.Tasks
import org.owntracks.android.location.LocationCallback
import org.owntracks.android.location.LocationProviderClient
import org.owntracks.android.location.LocationRequest
import timber.log.Timber


class GMSLocationProviderClient(
    private val fusedLocationProviderClient: FusedLocationProviderClient,
    private val contextClass: Class<Context>
) : LocationProviderClient() {

    private val callbackMap =
        mutableMapOf<LocationCallback, com.google.android.gms.location.LocationCallback>()

    @SuppressLint("MissingPermission")
    override fun actuallyRequestLocationUpdates(
        locationRequest: LocationRequest,
        clientCallBack: LocationCallback,
        looper: Looper?
    ) {
        Timber.i("Requesting location updates $locationRequest ${clientCallBack.hashCode()} $contextClass")

        val gmsCallBack = object : com.google.android.gms.location.LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                clientCallBack.onLocationResult(
                    org.owntracks.android.location.LocationResult(
                        locationResult.lastLocation
                    )
                )
            }

            override fun onLocationAvailability(locationAvailability: LocationAvailability) {
                clientCallBack.onLocationAvailability(
                    org.owntracks.android.location.LocationAvailability(
                        locationAvailability.isLocationAvailable
                    )
                )
            }
        }
        callbackMap[clientCallBack] = gmsCallBack
        fusedLocationProviderClient.requestLocationUpdates(
            locationRequest.toGMSLocationRequest(),
            gmsCallBack,
            looper!!
        )
    }

    override fun removeLocationUpdates(clientCallBack: LocationCallback) {
        callbackMap[clientCallBack]?.run {
            Timber.d("Removing location updates ${clientCallBack.hashCode()}")
            fusedLocationProviderClient.removeLocationUpdates(this)
            callbackMap.remove(clientCallBack)
        }
    }

    override fun flushLocations() {
        fusedLocationProviderClient.flushLocations()
    }

    @SuppressLint("MissingPermission")
    override fun getLastLocation(): Location? {
        return Tasks.await(fusedLocationProviderClient.lastLocation)
    }

    companion object {
        fun create(context: Context): GMSLocationProviderClient {
            return GMSLocationProviderClient(
                LocationServices.getFusedLocationProviderClient(context),
                context.javaClass
            )
        }
    }

    init {
        Timber.i("Using Google Play Services as a location provider called from $contextClass")
    }
}