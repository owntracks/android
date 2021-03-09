package org.owntracks.android.geocoding

import android.content.Context
import android.location.Address
import org.owntracks.android.injection.qualifier.AppContext
import timber.log.Timber
import java.math.BigDecimal
import java.util.*

class GoogleGeocoder internal constructor(@AppContext context: Context?) : CachingGeocoder() {
    private val geocoder: android.location.Geocoder = android.location.Geocoder(context, Locale.getDefault())

    override fun reverse(latitude: Double, longitude: Double): GeocodeResult {
        return if (geocoderAvailable()) {
            super.reverse(latitude, longitude)
        } else {
            GeocodeResult.Unavailable
        }
    }

    override fun doLookup(latitude: BigDecimal, longitude: BigDecimal): GeocodeResult {
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
            GeocodeResult.Error(e.toString())
        }
    }

    private fun geocoderAvailable(): Boolean {
        return android.location.Geocoder.isPresent()
    }
}