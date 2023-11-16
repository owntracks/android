package org.owntracks.android.gms.location

import com.google.android.gms.location.LocationAvailability
import com.google.android.gms.location.LocationResult
import org.owntracks.android.location.LocationCallback

/**
 * This is a wrapper around a [LocationCallback] instance that can be given to something that needs
 * a [com.google.android.gms.location.LocationCallback]. Once the thing that owns the [com.google.android.gms.location.LocationCallback]
 * has any of its methods triggered, it then passes that on to the methods of the [LocationCallback]
 *
 * @property clientCallBack the [LocationCallback] to wrap
 */
class GMSLocationCallback(private val clientCallBack: LocationCallback) :
    com.google.android.gms.location.LocationCallback() {
    override fun onLocationResult(locationResult: LocationResult) {
        super.onLocationResult(locationResult)
        locationResult.lastLocation?.apply {
            clientCallBack.onLocationResult(
                org.owntracks.android.location.LocationResult(
                    this
                )
            )
        } ?: run {
            clientCallBack.onLocationError()
        }
    }

    override fun onLocationAvailability(locationAvailability: LocationAvailability) {
        super.onLocationAvailability(locationAvailability)
        clientCallBack.onLocationAvailability(
            org.owntracks.android.location.LocationAvailability(
                locationAvailability.isLocationAvailable
            )
        )
    }
}
