package org.owntracks.android.testutils

import android.content.Context
import android.location.Criteria
import android.location.Location
import android.location.LocationManager
import android.os.SystemClock
import java.util.concurrent.TimeUnit

open class GPSMockDeviceLocation : MockDeviceLocation {
    private var locationManager: LocationManager? = null

    private val locationProvidersToMock =
        listOf(LocationManager.GPS_PROVIDER, LocationManager.NETWORK_PROVIDER)

    override fun initializeMockLocationProvider(context: Context) {
        this.locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        setPackageAsMockLocationProvider(context)

        locationManager?.run {
            locationProvidersToMock.forEach { provider ->
                try {
                    removeTestProvider(provider)
                } catch (e: IllegalArgumentException) {
                }
                addTestProvider(
                    provider,
                    false,
                    false,
                    false,
                    false,
                    true,
                    true,
                    true,
                    Criteria.POWER_MEDIUM,
                    Criteria.ACCURACY_FINE
                )
                setTestProviderEnabled(provider, true)
            }
        }
    }

    override fun unInitializeMockLocationProvider() {
        locationProvidersToMock.forEach {
            locationManager?.run {
                setTestProviderEnabled(it, false)
                removeTestProvider(it)
            }
        }
    }

    override fun setMockLocation(
        latitude: Double,
        longitude: Double,
        accuracy: Float
    ) {
        listOf(LocationManager.GPS_PROVIDER, LocationManager.NETWORK_PROVIDER).forEach { provider ->
            val location = Location(provider).apply {
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
            locationManager?.setTestProviderLocation(
                provider,
                location
            )
        }
    }
}