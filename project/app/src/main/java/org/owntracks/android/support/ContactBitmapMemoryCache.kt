package org.owntracks.android.support

import android.graphics.Bitmap
import androidx.collection.ArrayMap

internal class ContactBitmapMemoryCache {
    private val cacheLevelCard: ArrayMap<String, Bitmap?> = ArrayMap()
    private val cacheLevelTid: ArrayMap<String, TidBitmap> = ArrayMap()

    @Synchronized
    fun putLevelCard(key: String, value: Bitmap?) {
        cacheLevelCard[key] = value
        cacheLevelTid.remove(key)
    }

    @Synchronized
    fun putLevelTid(key: String, value: TidBitmap) {
        cacheLevelTid[key] = value
    }

    @Synchronized
    fun getLevelCard(key: String?): Bitmap? {
        return cacheLevelCard[key]
    }

    @Synchronized
    fun getLevelTid(key: String?): TidBitmap? {
        return cacheLevelTid[key]
    }

    @Synchronized
    fun clear() {
        cacheLevelCard.clear()
        cacheLevelTid.clear()
    }

    @Synchronized
    fun clearLevelCard(key: String?) {
        cacheLevelCard.remove(key)
    }
}