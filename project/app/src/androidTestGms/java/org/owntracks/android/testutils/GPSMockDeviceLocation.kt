package org.owntracks.android.testutils

import android.content.Context
import android.location.Location
import android.os.SystemClock
import com.google.android.gms.location.LocationServices
import com.google.android.gms.tasks.Tasks
import timber.log.Timber

class GPSMockDeviceLocation : MockDeviceLocation {
  private lateinit var context: Context

  override fun initializeMockLocationProvider(context: Context) {
    setPackageAsMockLocationProvider(context)
    this.context = context
  }

  override fun setMockLocation(latitude: Double, longitude: Double, accuracy: Float) {
    LocationServices.getFusedLocationProviderClient(context).apply {
      Tasks.await(setMockMode(true))
      setMockLocation(
          Location("")
              .apply {
                this.latitude = latitude
                this.longitude = longitude
                this.accuracy = accuracy
                this.time = System.currentTimeMillis()
                this.elapsedRealtimeNanos = SystemClock.elapsedRealtimeNanos()
              }
              .also { Timber.v("Setting mock location to $it") })
    }
  }
}
