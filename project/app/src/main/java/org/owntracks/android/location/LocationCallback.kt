package org.owntracks.android.location


interface LocationCallback {
    fun onLocationResult(locationResult: LocationResult)
    fun onLocationAvailability(locationAvailability: LocationAvailability)
}