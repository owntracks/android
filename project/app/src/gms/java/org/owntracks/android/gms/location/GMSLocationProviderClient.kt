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

/**
 * An implementation of [LocationProviderClient] that uses a [FusedLocationProviderClient] to request
 * loctaion updates
 *
 * @property fusedLocationProviderClient instance of Google location client to use to request updates
 * @property contextClass class of the requester, used just for logging
 */
class GMSLocationProviderClient(
    private val fusedLocationProviderClient: FusedLocationProviderClient,
    private val contextClass: Class<Context>
) : LocationProviderClient() {

    private val callbackMap =
        mutableMapOf<LocationCallback, com.google.android.gms.location.LocationCallback>()

    /**
     * Converts the generic callback into a GMS-specific callback before invoking the location client
     * to make location requests. Called by the superclass after removing location updates for this
     * specific callback to prevent duplicate location requests being active for the same callback
     *
     * @param locationRequest a [LocationRequest] describing how often locations should be produced
     * @param clientCallBack a [LocationCallback] instance that's invoked on new locations / location availability
     * @param looper a handler on which to run the location requester loop
     */
    @SuppressLint("MissingPermission")
    override fun actuallyRequestLocationUpdates(
        locationRequest: LocationRequest,
        clientCallBack: LocationCallback,
        looper: Looper?
    ) {
        Timber.i("Requesting location updates priority=${locationRequest.priority}, interval=${locationRequest.interval} clientCallback=${clientCallBack.hashCode()}, requester=$contextClass")

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
            Timber.d("Removing location updates clientcallback=${clientCallBack.hashCode()}")
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