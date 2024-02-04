package org.owntracks.android.geocoding

import java.math.BigDecimal
import java.math.RoundingMode
import timber.log.Timber

abstract class CachingGeocoder : Geocoder {
    private val cache = GeocoderLRUCache(40)

    override suspend fun reverse(latitude: Double, longitude: Double): GeocodeResult {
        val result = cache.computeAndOnlyStoreNonErrors(
            Pair(
                latitude.toBigDecimal().setScale(4, RoundingMode.HALF_EVEN),
                longitude.toBigDecimal().setScale(4, RoundingMode.HALF_EVEN)
            ),
            ::doLookup
        )
        Timber.v("Geocode cache: hits=${cache.hitCount()}, misses=${cache.missCount()}")
        return result
    }

    protected abstract fun doLookup(latitude: BigDecimal, longitude: BigDecimal): GeocodeResult
}
