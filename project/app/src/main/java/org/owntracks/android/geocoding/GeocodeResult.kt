package org.owntracks.android.geocoding

import kotlinx.datetime.Instant


sealed class GeocodeResult {
  data class Formatted(val text: String) : GeocodeResult()

  object Empty : GeocodeResult()

  sealed class Fault(open val until: Instant) : GeocodeResult() {
    data class Error(val message: String, override val until: Instant) : Fault(until)

    data class ExceptionError(val exception: Exception, override val until: Instant) : Fault(until)

    data class RateLimited(override val until: Instant) : Fault(until)

    data class Disabled(override val until: Instant) : Fault(until)

    data class IPAddressRejected(override val until: Instant) : Fault(until)

    data class Unavailable(override val until: Instant) : Fault(until)
  }
}
