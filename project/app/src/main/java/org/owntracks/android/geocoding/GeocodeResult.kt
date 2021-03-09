package org.owntracks.android.geocoding

import java.time.Instant

sealed class GeocodeResult {
    data class Formatted(val text: String) : GeocodeResult()
    object Empty : GeocodeResult()
    data class RateLimited(val until: Instant) : GeocodeResult()
    object Disabled : GeocodeResult()
    object IPAddressRejected: GeocodeResult()
    object Unavailable: GeocodeResult()
    data class Error(val message: String) : GeocodeResult()
}