package org.owntracks.android.geocoding

import androidx.collection.LruCache
import java.math.BigDecimal

class GeocoderLRUCache(maxSize: Int) :
    LruCache<Pair<BigDecimal, BigDecimal>, GeocodeResult>(maxSize) {
  fun computeAndOnlyStoreNonErrors(
      key: Pair<BigDecimal, BigDecimal>,
      resolverFunction: ((BigDecimal, BigDecimal) -> GeocodeResult)
  ): GeocodeResult {
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
