package org.owntracks.android.geocoding

import android.content.Context
import android.location.Address
import java.math.BigDecimal
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.Locale
import org.owntracks.android.location.LatLng
import timber.log.Timber

class DeviceGeocoder internal constructor(context: Context) : CachingGeocoder() {
  private val geocoder: android.location.Geocoder =
      android.location.Geocoder(context, Locale.getDefault())
  private var tripResetTimestamp: Instant = Instant.MIN

  override suspend fun reverse(latLng: LatLng): GeocodeResult {
    return if (geocoderAvailable()) {
      super.reverse(latLng)
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
      @Suppress("DEPRECATION") // The non-deprecated version needs API 33
      addresses = geocoder.getFromLocation(latitude.toDouble(), longitude.toDouble(), 1)
      if (!addresses.isNullOrEmpty()) {
        val g = StringBuilder()
        val a = addresses[0]
        if (a.getAddressLine(0) != null) g.append(a.getAddressLine(0))
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
