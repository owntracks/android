package org.owntracks.android.support

import android.graphics.Bitmap
import androidx.collection.LruCache
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ContactBitmapAndNameMemoryCache @Inject constructor() :
    LruCache<String, ContactBitmapAndName>(500)

sealed class ContactBitmapAndName {
  data class CardBitmap(val name: String?, val bitmap: Bitmap?) : ContactBitmapAndName()

  data class TrackerIdBitmap(val trackerId: String, val bitmap: Bitmap) : ContactBitmapAndName()
}
