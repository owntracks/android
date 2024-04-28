package org.owntracks.android.gms.location

import com.google.android.gms.location.Priority
import org.owntracks.android.location.LocationRequest

fun LocationRequest.toGMSLocationRequest(): com.google.android.gms.location.LocationRequest {
  val gmsPriority =
      when (priority) {
        LocationRequest.PRIORITY_HIGH_ACCURACY -> Priority.PRIORITY_HIGH_ACCURACY
        LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY ->
            Priority.PRIORITY_BALANCED_POWER_ACCURACY
        LocationRequest.PRIORITY_LOW_POWER -> Priority.PRIORITY_LOW_POWER
        LocationRequest.PRIORITY_NO_POWER -> Priority.PRIORITY_LOW_POWER
        else -> Priority.PRIORITY_BALANCED_POWER_ACCURACY
      }

  val gmsLocationRequestBuilder =
      com.google.android.gms.location.LocationRequest.Builder(gmsPriority, interval.toMillis())

  numUpdates?.run(gmsLocationRequestBuilder::setMaxUpdates)
  expirationDuration?.toMillis()?.run(gmsLocationRequestBuilder::setDurationMillis)
  smallestDisplacement?.run(gmsLocationRequestBuilder::setMinUpdateDistanceMeters)
  fastestInterval?.toMillis()?.run(gmsLocationRequestBuilder::setMinUpdateIntervalMillis)
  waitForAccurateLocation?.run(gmsLocationRequestBuilder::setWaitForAccurateLocation)

  return gmsLocationRequestBuilder.build()
}
