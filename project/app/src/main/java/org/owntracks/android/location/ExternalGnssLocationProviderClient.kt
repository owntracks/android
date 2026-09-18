package org.owntracks.android.location

import android.location.Location
import android.os.Looper
import androidx.annotation.RequiresPermission
import org.owntracks.android.location.external.ExternalGnssController

/**
 * [LocationProviderClient] that wraps an existing provider (AOSP or GMS) and augments it with fixes
 * coming from an external USB GNSS receiver managed by [ExternalGnssController].
 *
 * All location requests are transparently forwarded to the [delegate]; in addition, every
 * [LocationCallback] is registered with the controller so it also receives synthetic
 * [android.location.Location] objects built from external NMEA fixes.
 */
class ExternalGnssLocationProviderClient(
    private val delegate: LocationProviderClient,
    private val controller: ExternalGnssController,
) : LocationProviderClient() {

  @RequiresPermission(
      anyOf =
          ["android.permission.ACCESS_FINE_LOCATION", "android.permission.ACCESS_COARSE_LOCATION"])
  override fun singleHighAccuracyLocation(clientCallBack: LocationCallback, looper: Looper) {
    delegate.singleHighAccuracyLocation(clientCallBack, looper)
  }

  @RequiresPermission(
      anyOf =
          ["android.permission.ACCESS_FINE_LOCATION", "android.permission.ACCESS_COARSE_LOCATION"])
  override fun actuallyRequestLocationUpdates(
      locationRequest: LocationRequest,
      clientCallBack: LocationCallback,
      looper: Looper
  ) {
    // Forward to the real provider (uses the public entry point because the "actually" method is
    // protected on the delegate).
    delegate.requestLocationUpdates(locationRequest, clientCallBack, looper)
    controller.registerCallback(clientCallBack)
  }

  override fun removeLocationUpdates(clientCallBack: LocationCallback) {
    delegate.removeLocationUpdates(clientCallBack)
    controller.unregisterCallback(clientCallBack)
  }

  override fun flushLocations() {
    delegate.flushLocations()
  }

  override fun getLastLocation(): Location? {
    return controller.getLastExternalLocation() ?: delegate.getLastLocation()
  }
}
