package org.owntracks.android.testutils

import android.content.Context
import android.location.Location
import android.location.LocationManager
import android.os.SystemClock
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.tasks.Tasks
import org.owntracks.android.testutils.MockDeviceLocation
import timber.log.Timber
import java.util.concurrent.TimeUnit

class FusedGMSockDeviceLocation : MockDeviceLocation {
    private var client: FusedLocationProviderClient? = null
    override fun initializeMockLocationProvider(context: Context) {
        setPackageAsMockLocationProvider(context)
        client = FusedLocationProviderClient(context)
        Timber.i("Setting GMS fused mock mode")
        client?.setMockMode(true)?.run(Tasks::await)
    }

    override fun unInitializeMockLocationProvider() {
        Timber.i("Unsetting GMS fused mock mode")
        client?.setMockMode(false)?.run(Tasks::await)
    }

    override fun setMockLocation(latitude: Double, longitude: Double, accuracy: Float) {
        val location = Location(LocationManager.GPS_PROVIDER).apply {
            this.latitude = latitude
            this.longitude = longitude
            this.accuracy = accuracy
            this.time = System.currentTimeMillis()
            this.altitude = 50.0
            this.bearing = 0f
            this.speed = 0f
            this.elapsedRealtimeNanos =
                TimeUnit.MILLISECONDS.toNanos(SystemClock.uptimeMillis())
            this.bearingAccuracyDegrees = 0f
            this.speedAccuracyMetersPerSecond = 1f
            this.elapsedRealtimeUncertaintyNanos = 10.0
        }
        Timber.i("Setting mock location to $latitude, $longitude")
        client?.setMockLocation(location)?.run(Tasks::await)
    }
}