package org.owntracks.android.geocoding

import org.threeten.bp.Instant

sealed class GeocodeResult {
    data class Formatted(val text: String) : GeocodeResult()
    object Empty : GeocodeResult()
    data class RateLimited(val until: Instant) : GeocodeResult()
    data class Disabled(val until: Instant) : GeocodeResult()
    data class IPAddressRejected(val until: Instant) : GeocodeResult()
    data class Unavailable(val until: Instant) : GeocodeResult()
    data class Error(val message: String, val until: Instant) : GeocodeResult()
}