package org.owntracks.android.gms.location

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import android.os.Looper
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.*
import com.google.android.gms.tasks.Tasks
import org.owntracks.android.location.LocationCallback
import org.owntracks.android.location.LocationProviderClient
import org.owntracks.android.location.LocationRequest
import org.owntracks.android.ui.map.MapActivity
import timber.log.Timber


class GMSLocationProviderClient(private val fusedLocationProviderClient: FusedLocationProviderClient, private val context: Context) : LocationProviderClient {
    private var userHasDeclinedLocationSettingsRequest = false
    private val callbackMap = mutableMapOf<LocationCallback, com.google.android.gms.location.LocationCallback>()

    override fun requestLocationUpdates(locationRequest: LocationRequest, clientCallBack: LocationCallback) {
        requestLocationUpdates(locationRequest, clientCallBack, null)
    }

    override fun requestLocationUpdates(locationRequest: LocationRequest, clientCallBack: LocationCallback, looper: Looper?) {
        removeLocationUpdates(clientCallBack)

        val locationSettingsRequestBuilder = LocationSettingsRequest.Builder().addLocationRequest(locationRequest.toGMSLocationRequest())

        LocationServices.getSettingsClient(this.fusedLocationProviderClient.applicationContext)
                .checkLocationSettings(locationSettingsRequestBuilder.build())
                .addOnFailureListener {
                    when (it) {
                        is ApiException -> {
                            when (it.statusCode) {
                                LocationSettingsStatusCodes.RESOLUTION_REQUIRED -> {
                                    if (userHasDeclinedLocationSettingsRequest) {
                                        Timber.w("User previously declined to enable location for this context, so not asking again")
                                    } else if (this.context is MapActivity) {
                                        this.context.locationLifecycleObserver.resolveException(it as ResolvableApiException) { success ->
                                            if (success) {
                                                Timber.d("User enabled location")
                                                actuallyRequestLocationUpdates(locationRequest, clientCallBack, looper)
                                            } else {
                                                Timber.w("User did not enable location, not requesting location updates")
                                                userHasDeclinedLocationSettingsRequest = true
                                            }
                                        }
                                    }
                                }
                                else -> Timber.e("Unhandled error from SettingsClient $it")
                            }
                        }
                        else -> Timber.e("Unhandled error from SettingsClient $it")
                    }
                }
                .addOnSuccessListener {
                    Timber.d("GMS Location Settings check success")
                    actuallyRequestLocationUpdates(locationRequest, clientCallBack, looper)
                }
    }

    @SuppressLint("MissingPermission")
    private fun actuallyRequestLocationUpdates(locationRequest: LocationRequest, clientCallBack: LocationCallback, looper: Looper?) {
        Timber.i("Requesting location updates $locationRequest ${clientCallBack.hashCode()}")
        val gmsCallBack = object : com.google.android.gms.location.LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                clientCallBack.onLocationResult(org.owntracks.android.location.LocationResult(locationResult.lastLocation))
            }

            override fun onLocationAvailability(locationAvailability: LocationAvailability) {
                clientCallBack.onLocationAvailability(org.owntracks.android.location.LocationAvailability(locationAvailability.isLocationAvailable))
            }
        }
        callbackMap[clientCallBack] = gmsCallBack
        fusedLocationProviderClient.requestLocationUpdates(locationRequest.toGMSLocationRequest(), gmsCallBack, looper!!)
    }

    override fun removeLocationUpdates(clientCallBack: LocationCallback) {
        callbackMap[clientCallBack]?.run {
            Timber.i("Removing location updates ${clientCallBack.hashCode()}")
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
            return GMSLocationProviderClient(LocationServices.getFusedLocationProviderClient(context), context)
        }
    }

    init {
        Timber.i("Using Google Play Services as a location provider")
    }
}