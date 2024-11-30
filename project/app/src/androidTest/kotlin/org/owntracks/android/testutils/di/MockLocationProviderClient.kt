package org.owntracks.android.testutils.di

import android.location.Location
import android.os.Build
import android.os.Looper
import kotlinx.datetime.Clock
import org.owntracks.android.location.LocationAvailability
import org.owntracks.android.location.LocationCallback
import org.owntracks.android.location.LocationProviderClient
import org.owntracks.android.location.LocationRequest
import org.owntracks.android.location.LocationResult

class MockLocationProviderClient : LocationProviderClient() {
  private val callbacks = mutableSetOf<LocationCallback>()
  private var lastLocation: Location? = null

  fun setLocation(location: Location) {
    callbacks.forEach {
      if (lastLocation == null) {
        it.onLocationAvailability(LocationAvailability(true))
      }
      it.onLocationResult(LocationResult(location))
    }
    lastLocation = location
  }

  override fun singleHighAccuracyLocation(clientCallBack: LocationCallback, looper: Looper) {
    clientCallBack.onLocationResult(LocationResult(lastLocation!!))
  }

  override fun actuallyRequestLocationUpdates(
      locationRequest: LocationRequest,
      clientCallBack: LocationCallback,
      looper: Looper
  ) {
    callbacks.add(clientCallBack)
    lastLocation?.run {
      clientCallBack.onLocationAvailability(LocationAvailability(true))
      clientCallBack.onLocationResult(LocationResult(this))
    }
  }

  override fun removeLocationUpdates(clientCallBack: LocationCallback) {
    callbacks.remove(clientCallBack)
  }

  override fun flushLocations() {
    // No-op
  }

  override fun getLastLocation(): Location? = lastLocation
}

fun LocationProviderClient.setLocation(
    latitude: Double,
    longitude: Double,
    altitude: Double = 0.0,
    accuracy: Float = 5.0f,
    speed: Float = 0.0f
) {
  (this as MockLocationProviderClient).setLocation(
      Location("test").apply {
        this.latitude = latitude
        this.longitude = longitude
        this.time = Clock.System.now().toEpochMilliseconds()
        this.altitude = altitude
        this.accuracy = accuracy
        this.speed = speed
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
          this.isMock = true
        }
      })
}
