package org.owntracks.android.testutils

import android.content.Context
import android.location.Location
import android.location.LocationManager
import android.location.provider.ProviderProperties.ACCURACY_FINE
import android.location.provider.ProviderProperties.POWER_USAGE_HIGH
import android.os.SystemClock
import androidx.core.location.LocationCompat
import java.util.concurrent.TimeUnit
import timber.log.Timber

open class GPSMockDeviceLocation : MockDeviceLocation {
  private var locationManager: LocationManager? = null

  private val locationProvidersToMock =
      listOf(LocationManager.GPS_PROVIDER, LocationManager.NETWORK_PROVIDER)

  override fun initializeMockLocationProvider(context: Context) {
    Timber.d("Initialize mock location Provider")
    this.locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
    setPackageAsMockLocationProvider(context)
    locationManager?.run {
      locationProvidersToMock.forEach { provider ->
        addTestProvider(
            provider, false, false, false, false, true, true, true, POWER_USAGE_HIGH, ACCURACY_FINE)
        Timber.d("Enabling location Test Provider called $provider")
        setTestProviderEnabled(provider, true)
      }
    }
  }

  override fun setMockLocation(latitude: Double, longitude: Double, accuracy: Float) {
    locationProvidersToMock.forEach { provider ->
      val location =
          Location(provider).apply {
            this.latitude = latitude
            this.longitude = longitude
            this.accuracy = accuracy
            time = System.currentTimeMillis()
            altitude = 50.0
            bearing = 0f
            speed = 0f
            elapsedRealtimeNanos = TimeUnit.MILLISECONDS.toNanos(SystemClock.uptimeMillis())
            LocationCompat.setBearingAccuracyDegrees(this, 0f)
            LocationCompat.setSpeedAccuracyMetersPerSecond(this, 1f)
          }
      Timber.i("Setting mock GPS location for $provider to $location")
      locationManager!!.setTestProviderLocation(provider, location)
    }
  }
}
