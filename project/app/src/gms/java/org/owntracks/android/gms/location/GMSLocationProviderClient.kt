package org.owntracks.android.gms.location

import android.annotation.SuppressLint
import android.content.Context
import android.os.Looper
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationAvailability
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import org.owntracks.android.location.LocationCallback
import org.owntracks.android.location.LocationProviderClient
import org.owntracks.android.location.LocationRequest

class GMSLocationProviderClient(private val fusedLocationProviderClient: FusedLocationProviderClient) : LocationProviderClient {
    private val callbackMap = mutableMapOf<LocationCallback, com.google.android.gms.location.LocationCallback>()


    override fun requestLocationUpdates(locationRequest: LocationRequest, clientCallBack: LocationCallback) {
        requestLocationUpdates(locationRequest, clientCallBack, null)
    }

    @SuppressLint("MissingPermission")
    override fun requestLocationUpdates(locationRequest: LocationRequest, clientCallBack: LocationCallback, looper: Looper?) {
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
            fusedLocationProviderClient.removeLocationUpdates(this)
            callbackMap.remove(clientCallBack)
        }
    }

    override fun flushLocations() {
        fusedLocationProviderClient.flushLocations();
    }

    companion object {
        fun create(context: Context): GMSLocationProviderClient {
            return GMSLocationProviderClient(LocationServices.getFusedLocationProviderClient(context))
        }
    }
}