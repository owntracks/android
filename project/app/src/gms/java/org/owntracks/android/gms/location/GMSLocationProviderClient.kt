package org.owntracks.android.gms.location

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.location.Location
import android.location.LocationManager
import android.os.Looper
import android.provider.Settings
import androidx.core.location.LocationManagerCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationAvailability
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.tasks.Tasks
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import org.owntracks.android.R
import org.owntracks.android.location.LocationCallback
import org.owntracks.android.location.LocationProviderClient
import org.owntracks.android.location.LocationRequest
import timber.log.Timber


class GMSLocationProviderClient(private val fusedLocationProviderClient: FusedLocationProviderClient, private val context: Context) : LocationProviderClient {
    private var userHasDeclinedLocationSettingsRequest = false
    private val callbackMap = mutableMapOf<LocationCallback, com.google.android.gms.location.LocationCallback>()

    override fun requestLocationUpdates(locationRequest: LocationRequest, clientCallBack: LocationCallback) {
        requestLocationUpdates(locationRequest, clientCallBack, null)
    }

    override fun requestLocationUpdates(
        locationRequest: LocationRequest,
        clientCallBack: LocationCallback,
        looper: Looper?
    ) {
        removeLocationUpdates(clientCallBack)
        if (context is Activity) {
            val locationService =
                context.getSystemService(Context.LOCATION_SERVICE) as LocationManager?
            locationService?.run {
                val locationEnabled = LocationManagerCompat.isLocationEnabled(this)
                if (!locationEnabled && !userHasDeclinedLocationSettingsRequest) {
                    MaterialAlertDialogBuilder(context)
                        .setTitle("Device location disabled")
                        .setMessage(
                            "Device location is not currently enabled, so OwnTracks cannot fetch your current location." +
                                    "\n\n" +
                                    "Open the Location Settings page and enable location to allow OwnTracks to track your current location."
                        )
                        .setIcon(R.drawable.ic_baseline_location_disabled_24)
                        .setPositiveButton(
                            "Open Location Settings"
                        ) { _, _ ->
                            context.startActivity(
                                Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
                            )
                        }
                        .setNegativeButton("Cancel") { _, _ ->
                            userHasDeclinedLocationSettingsRequest = true
                        }
                        .show()
                } else {
                    actuallyRequestLocationUpdates(locationRequest, clientCallBack, looper)
                }
            }
        } else {
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