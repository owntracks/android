package org.owntracks.android.geocoding

import java.math.BigDecimal
import java.math.RoundingMode
import org.owntracks.android.location.LatLng
import timber.log.Timber

abstract class CachingGeocoder : Geocoder {
  private val cache = GeocoderLRUCache(40)

  override suspend fun reverse(latLng: LatLng): GeocodeResult {
    val result =
        cache.computeAndOnlyStoreNonErrors(
            Pair(
                latLng.latitude.value.toBigDecimal().setScale(4, RoundingMode.HALF_EVEN),
                latLng.longitude.value.toBigDecimal().setScale(4, RoundingMode.HALF_EVEN)),
            ::doLookup)
    Timber.v("Geocode cache: hits=${cache.hitCount()}, misses=${cache.missCount()}")
    return result
  }

  protected abstract fun doLookup(latitude: BigDecimal, longitude: BigDecimal): GeocodeResult
}
