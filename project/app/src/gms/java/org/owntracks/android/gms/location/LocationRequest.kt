package org.owntracks.android.gms.location

import com.google.android.gms.location.Priority
import org.owntracks.android.location.LocationRequest

fun LocationRequest.toGMSLocationRequest(): com.google.android.gms.location.LocationRequest {
    val gmsPriority = when (priority) {
        LocationRequest.PRIORITY_HIGH_ACCURACY -> Priority.PRIORITY_HIGH_ACCURACY
        LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY -> Priority.PRIORITY_BALANCED_POWER_ACCURACY
        LocationRequest.PRIORITY_LOW_POWER -> Priority.PRIORITY_LOW_POWER
        LocationRequest.PRIORITY_NO_POWER -> Priority.PRIORITY_LOW_POWER
        else -> Priority.PRIORITY_BALANCED_POWER_ACCURACY
    }
    val gmsLocationRequest = com.google.android.gms.location.LocationRequest
        .create()
        .setPriority(gmsPriority)
    interval?.run { gmsLocationRequest.interval = this }
    numUpdates?.run { gmsLocationRequest.numUpdates = this }
    expirationDuration?.run { gmsLocationRequest.setExpirationDuration(this) }
    smallestDisplacement?.run { gmsLocationRequest.smallestDisplacement = this }
    fastestInterval?.run { gmsLocationRequest.fastestInterval = this }
    waitForAccurateLocation?.run { gmsLocationRequest.isWaitForAccurateLocation = this }
    return gmsLocationRequest
}
