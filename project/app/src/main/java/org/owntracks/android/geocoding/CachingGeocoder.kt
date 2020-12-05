package org.owntracks.android.geocoding


import androidx.collection.LruCache

abstract class CachingGeocoder : Geocoder {
    protected val cache = object : LruCache<Pair<Double, Double>, String?>(40) {
        override fun create(key: Pair<Double, Double>): String? {
            return doLookup(key.first, key.second)
        }
    }

    override fun reverse(latitude: Double, longitude: Double): String? {
        return cache[Pair(latitude, longitude)]
    }

    protected abstract fun doLookup(latitude: Double, longitude: Double): String?
}