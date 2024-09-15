package org.owntracks.android.location

import android.location.Location
import android.os.Looper
import androidx.annotation.RequiresPermission

abstract class LocationProviderClient {
  /**
   * Request location updates is the API called by the app to start requesting location updates,
   * calling the given callback when the location changes. This is generic to the actual location
   * provider, so simply removes any existing location request for the given callback before
   * delegating the request to the actual provider.
   *
   * @param locationRequest
   * @param clientCallBack
   * @param looper
   */
  @RequiresPermission(
      anyOf =
          ["android.permission.ACCESS_FINE_LOCATION", "android.permission.ACCESS_COARSE_LOCATION"])
  fun requestLocationUpdates(
      locationRequest: LocationRequest,
      clientCallBack: LocationCallback,
      looper: Looper
  ) {
    removeLocationUpdates(clientCallBack)
    actuallyRequestLocationUpdates(locationRequest, clientCallBack, looper)
  }

  abstract fun singleHighAccuracyLocation(clientCallBack: LocationCallback, looper: Looper)

  protected abstract fun actuallyRequestLocationUpdates(
      locationRequest: LocationRequest,
      clientCallBack: LocationCallback,
      looper: Looper
  )

  abstract fun removeLocationUpdates(clientCallBack: LocationCallback)

  abstract fun flushLocations()

  abstract fun getLastLocation(): Location?
}
