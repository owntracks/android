package org.owntracks.android.location

interface LocationCallback {
    fun onLocationResult(locationResult: LocationResult)
    fun onLocationError()
    fun onLocationAvailability(locationAvailability: LocationAvailability)
}
