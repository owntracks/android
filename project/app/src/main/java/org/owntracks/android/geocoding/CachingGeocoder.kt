package org.owntracks.android.geocoding


import androidx.collection.LruCache
import timber.log.Timber
import java.math.BigDecimal
import java.math.RoundingMode

abstract class CachingGeocoder : Geocoder {
    private val cache = GeocoderLRUCache(40)

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

class GeocoderLRUCache(maxSize: Int) : LruCache<Pair<BigDecimal, BigDecimal>, GeocodeResult>(maxSize) {
    fun computeAndOnlyStoreNonErrors(key: Pair<BigDecimal, BigDecimal>, resolverFunction: ((BigDecimal, BigDecimal) -> GeocodeResult)): GeocodeResult {
        if (this[key] != null) {
            return this[key]!!
        }
        val result = resolverFunction(key.first, key.second)
        if (result is GeocodeResult.Formatted || result is GeocodeResult.Empty) {
            this.put(key, result)
        }
        return result
    }
}
