package org.owntracks.android.geocoding

import android.content.Context
import android.location.Address
import java.math.BigDecimal
import java.util.*
import org.threeten.bp.Instant
import org.threeten.bp.temporal.ChronoUnit
import timber.log.Timber

class DeviceGeocoder internal constructor(context: Context) : CachingGeocoder() {
    private val geocoder: android.location.Geocoder =
        android.location.Geocoder(context, Locale.getDefault())
    private var tripResetTimestamp: Instant = Instant.MIN
    override fun reverse(latitude: Double, longitude: Double): GeocodeResult {
        return if (geocoderAvailable()) {
            super.reverse(latitude, longitude)
        } else {
            tripResetTimestamp = Instant.now().plus(1, ChronoUnit.MINUTES)
            GeocodeResult.Fault.Unavailable(tripResetTimestamp)
        }
    }

    override fun doLookup(latitude: BigDecimal, longitude: BigDecimal): GeocodeResult {
        if (tripResetTimestamp > Instant.now()) {
            Timber.w("Rate-limited, not querying until $tripResetTimestamp")
            return GeocodeResult.Fault.RateLimited(tripResetTimestamp)
        }
        val addresses: List<Address>?
        return try {
            addresses = geocoder.getFromLocation(latitude.toDouble(), longitude.toDouble(), 1)
            if (addresses != null && addresses.isNotEmpty()) {
                val g = StringBuilder()
                val a = addresses[0]
                if (a.getAddressLine(0) != null) g.append(a.getAddressLine(0))
                Timber.d("Resolved $latitude,$longitude to $g")
                GeocodeResult.Formatted(g.toString())
            } else {
                GeocodeResult.Empty
            }
        } catch (e: Exception) {
            tripResetTimestamp = Instant.now().plus(1, ChronoUnit.MINUTES)
            GeocodeResult.Fault.Error(e.toString(), tripResetTimestamp)
        }
    }

    private fun geocoderAvailable(): Boolean {
        return android.location.Geocoder.isPresent()
    }
}
