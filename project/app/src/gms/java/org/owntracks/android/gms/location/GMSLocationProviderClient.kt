package org.owntracks.android.gms.location

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import android.os.Looper
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.tasks.Tasks
import org.owntracks.android.location.LocationCallback
import org.owntracks.android.location.LocationProviderClient
import org.owntracks.android.location.LocationRequest
import timber.log.Timber
import java.util.concurrent.ExecutionException

/**
 * An implementation of [LocationProviderClient] that uses a [FusedLocationProviderClient] to request
 * loctaion updates
 *
 * @property fusedLocationProviderClient instance of Google location client to use to request updates
 */
class GMSLocationProviderClient(
    private val fusedLocationProviderClient: FusedLocationProviderClient
) : LocationProviderClient() {

    private val callbackMap =
        mutableMapOf<Int, com.google.android.gms.location.LocationCallback>()

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
        Timber.d("Requesting location updates priority=${locationRequest.priority}, interval=${locationRequest.interval} clientCallback=${clientCallBack.hashCode()}")
        if (looper == null) {
            Timber.e("No looper provided, can't request GMS location updates")
            return
        }
        val gmsCallBack = GMSLocationCallback(clientCallBack)
        callbackMap[clientCallBack.hashCode()] = gmsCallBack
        val gmsLocationRequest = locationRequest.toGMSLocationRequest()
        Timber.d("transformed location request is $gmsLocationRequest")
        fusedLocationProviderClient.requestLocationUpdates(
            gmsLocationRequest,
            gmsCallBack,
            looper
        ).addOnCompleteListener {
            Timber.d("GMS Background location update request completed: Success=${it.isSuccessful} Cancelled=${it.isCanceled}")
        }
    }

    override fun removeLocationUpdates(clientCallBack: LocationCallback) {
        callbackMap[clientCallBack.hashCode()]?.run {
            Timber.d("Removing location updates clientcallback=${clientCallBack.hashCode()}")
            fusedLocationProviderClient.removeLocationUpdates(this)
            callbackMap.remove(clientCallBack.hashCode())
        }
    }

    override fun flushLocations() {
        fusedLocationProviderClient.flushLocations()
    }

    @SuppressLint("MissingPermission")
    override fun getLastLocation(): Location? {
        return try {
            Tasks.await(fusedLocationProviderClient.lastLocation)
        } catch (e: ExecutionException) {
            Timber.e(e, "Error fetching last location from GMS client")
            null
        } catch (e: InterruptedException) {
            Timber.e(e, "Error fetching last location from GMS client")
            null
        }
    }

    companion object {
        fun create(context: Context): GMSLocationProviderClient {
            return GMSLocationProviderClient(LocationServices.getFusedLocationProviderClient(context))
        }
    }
}
