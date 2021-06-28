package org.owntracks.android.geocoding


import timber.log.Timber
import java.math.BigDecimal
import java.math.RoundingMode

abstract class CachingGeocoder : Geocoder {
    private val cache = GeocoderLRUCache(40)

    @Synchronized
    override fun reverse(latitude: Double, longitude: Double): GeocodeResult {
        val result = cache.computeAndOnlyStoreNonErrors(
            Pair(
                latitude.toBigDecimal().setScale(4, RoundingMode.HALF_EVEN),
                longitude.toBigDecimal().setScale(4, RoundingMode.HALF_EVEN)
            ),
            ::doLookup
        )
        Timber.d("Geocode cache: hits=${cache.hitCount()}, misses=${cache.missCount()}")
        return result
    }

    protected abstract fun doLookup(latitude: BigDecimal, longitude: BigDecimal): GeocodeResult
}

