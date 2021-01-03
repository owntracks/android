package org.owntracks.android.location


open class LocationCallback {
    open fun onLocationResult(locationResult: LocationResult) {}
    open fun onLocationAvailability(locationAvailability: LocationAvailability) {}
}