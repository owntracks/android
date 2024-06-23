package org.owntracks.android.gms.location

import com.google.android.gms.location.Priority
import org.owntracks.android.location.LocationRequest
import org.owntracks.android.location.LocatorPriority

fun LocationRequest.toGMSLocationRequest(): com.google.android.gms.location.LocationRequest {
  val gmsPriority =
      when (priority) {
        LocatorPriority.HighAccuracy -> Priority.PRIORITY_HIGH_ACCURACY
        LocatorPriority.BalancedPowerAccuracy -> Priority.PRIORITY_BALANCED_POWER_ACCURACY
        LocatorPriority.LowPower -> Priority.PRIORITY_LOW_POWER
        LocatorPriority.NoPower -> Priority.PRIORITY_LOW_POWER
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
