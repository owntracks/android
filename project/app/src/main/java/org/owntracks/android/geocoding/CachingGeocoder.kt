package org.owntracks.android.geocoding


import androidx.collection.LruCache
import timber.log.Timber
import java.math.BigDecimal
import java.math.RoundingMode

abstract class CachingGeocoder : Geocoder {
    private val cache = object : LruCache<Pair<BigDecimal, BigDecimal>, GeocodeResult>(40) {
        override fun create(key: Pair<BigDecimal, BigDecimal>): GeocodeResult {
            return doLookup(key.first, key.second)
        }
    }

    override fun reverse(latitude: Double, longitude: Double): GeocodeResult {
        val result = cache[Pair(latitude.toBigDecimal().setScale(4, RoundingMode.HALF_EVEN), longitude.toBigDecimal().setScale(4, RoundingMode.HALF_EVEN))]
        Timber.d("Geocode cache: hits=${cache.hitCount()}, misses=${cache.missCount()}")
        return result!!
    }

    protected abstract fun doLookup(latitude: BigDecimal, longitude: BigDecimal): GeocodeResult
}