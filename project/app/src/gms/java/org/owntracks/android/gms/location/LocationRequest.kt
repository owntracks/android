package org.owntracks.android.gms.location

import org.owntracks.android.location.LocationRequest

fun LocationRequest.toGMSLocationRequest(): com.google.android.gms.location.LocationRequest {
    val gmsPriority = when (priority) {
        LocationRequest.PRIORITY_HIGH_ACCURACY -> com.google.android.gms.location.LocationRequest.PRIORITY_HIGH_ACCURACY
        LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY -> com.google.android.gms.location.LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY
        LocationRequest.PRIORITY_LOW_POWER -> com.google.android.gms.location.LocationRequest.PRIORITY_LOW_POWER
        LocationRequest.PRIORITY_NO_POWER -> com.google.android.gms.location.LocationRequest.PRIORITY_NO_POWER
        else -> com.google.android.gms.location.LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY
    }
    return com.google.android.gms.location.LocationRequest
            .create()
            .setPriority(gmsPriority)
            .setInterval(interval)
            .setFastestInterval(fastestInterval)
            .setNumUpdates(numUpdates)
            .setExpirationDuration(expirationDuration)
            .setSmallestDisplacement(smallestDisplacement)
}

