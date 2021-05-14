package org.owntracks.android.geocoding

import android.location.Address
import org.owntracks.android.services.MessageProcessorEndpointHttp
import org.threeten.bp.Instant
import org.threeten.bp.temporal.ChronoUnit
import timber.log.Timber
import java.math.BigDecimal


class NominatimGeocoder : CachingGeocoder() {
    private val geocoder = org.osmdroid.bonuspack.location.GeocoderNominatim(MessageProcessorEndpointHttp.USERAGENT)
    private var tripResetTimestamp: Instant = Instant.MIN

    override fun doLookup(latitude: BigDecimal, longitude: BigDecimal): GeocodeResult {
        if (tripResetTimestamp > Instant.now()) {
            Timber.w("Rate-limited, not querying")
            return GeocodeResult.RateLimited(tripResetTimestamp)
        }
        val prevTripTimeStamp = tripResetTimestamp
        tripResetTimestamp = Instant.now().plus(1, ChronoUnit.MINUTES)
        val addresses: List<Address>?
        return try {
            addresses = geocoder.getFromLocation(latitude.toDouble(), longitude.toDouble(), 1)
            if (addresses != null && addresses.isNotEmpty()) {
                val g = StringBuilder()
                val a = addresses[0]
                if (a.thoroughfare != null) {
                    g.append(a.thoroughfare)
                    if (a.subThoroughfare != null) {
                        g.append(" ")
                        g.append(a.subThoroughfare)
                    }
                    g.append(", ")
                }
                if (a.locality != null) {
                    if (a.postalCode != null) {
                        g.append(a.postalCode)
                        g.append(" ")
                    }
                    g.append(a.locality)
                    g.append(", ")
                }
                if (a.subAdminArea != null) {
                    g.append(a.subAdminArea)
                    g.append(", ")
                }
                if (a.adminArea != null) {
                    g.append(a.adminArea)
                }
                val formatted = if (g.toString().isNotEmpty()) {
                    var address = g.toString()
                    if (address.endsWith(", ")) {
                        address = address.substring(0, address.length - 2)
                    }
                    address
                } else {
                    return GeocodeResult.Empty
                }

                Timber.d("Resolved $latitude,$longitude to $formatted")
                GeocodeResult.Formatted(formatted)
            } else {
                GeocodeResult.Empty
            }
        } catch (e: Exception) {
            GeocodeResult.Error(e.toString(), prevTripTimeStamp)
        }
    }
}