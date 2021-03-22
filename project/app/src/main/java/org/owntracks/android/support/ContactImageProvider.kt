package org.owntracks.android.support

import android.content.Context
import android.graphics.*
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.util.Base64
import android.widget.ImageView
import androidx.collection.ArrayMap
import androidx.databinding.BindingAdapter
import androidx.databinding.DataBindingComponent
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.Marker
import kotlinx.coroutines.*
import org.owntracks.android.injection.qualifier.AppContext
import org.owntracks.android.model.FusedContact
import org.owntracks.android.support.widgets.TextDrawable
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

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

@Singleton
class ContactImageProvider @Inject constructor(@AppContext context: Context): DataBindingComponent {
    private val faceDimensions = (48 * (context.resources.displayMetrics.densityDpi / 160f)).toInt()
    fun invalidateCacheLevelCard(key: String?) {
        memoryCache.clearLevelCard(key)
    }

    fun setMarkerAsync(marker: Marker, contact: FusedContact?) {
        GlobalScope.launch(Dispatchers.Main) {
            contact?.let {
                val bitmap = BitmapDescriptorFactory.fromBitmap(getBitmapFromCache(it))
                marker.setIcon(bitmap)
                marker.isVisible = true
            }
        }
    }

    fun setImageViewAsync(imageView: ImageView, contact: FusedContact?) {
        GlobalScope.launch(Dispatchers.Main) {
            contact?.let {
                imageView.setImageBitmap(getBitmapFromCache(it))
            }
        }
    }

    private suspend fun getBitmapFromCache(contact: FusedContact?): Bitmap? {
        return withContext(Dispatchers.IO) {
            var bitmap: Bitmap?
            if (contact == null) return@withContext null
            if (contact.hasCard()) {
                bitmap = memoryCache.getLevelCard(contact.id)
                if (bitmap != null) {
                    return@withContext bitmap
                }
                if (contact.messageCard.hasFace()) {
                    val imageAsBytes = Base64.decode(contact.messageCard.face!!.toByteArray(), Base64.DEFAULT)
                    val b = BitmapFactory.decodeByteArray(imageAsBytes, 0, imageAsBytes.size)
                    if (b == null) {
                        Timber.e("Decoding card bitmap failed")
                        val fallbackBitmap = Bitmap.createBitmap(faceDimensions, faceDimensions, Bitmap.Config.ARGB_8888)
                        val canvas = Canvas(fallbackBitmap)
                        val paint = Paint()
                        paint.color = -0x1
                        canvas.drawRect(0f, 0f, faceDimensions.toFloat(), faceDimensions.toFloat(), paint)
                        bitmap = getRoundedShape(fallbackBitmap)
                    } else {
                        bitmap = getRoundedShape(Bitmap.createScaledBitmap(b, faceDimensions, faceDimensions, true))
                        memoryCache.putLevelCard(contact.id, bitmap)
                    }
                    return@withContext bitmap
                }
            }
            var td = memoryCache.getLevelTid(contact.id)
            // if cache doesn't contain a bitmap for a contact or if the cached bitmap was for an old tid, create a new one and cache it
            if (td == null || !td.isBitmapFor(contact.trackerId)) {
                td = TidBitmap(contact.trackerId, drawableToBitmap(TextDrawable.builder().buildRoundRect(contact.trackerId, TextDrawable.ColorGenerator.MATERIAL.getColor(contact.id), faceDimensions)))
                memoryCache.putLevelTid(contact.id, td)
            }
            return@withContext td.bitmap
        }

    }

    fun invalidateCache() {
        memoryCache.clear()
    }

    private fun getRoundedShape(bitmap: Bitmap): Bitmap {
        val output = Bitmap.createBitmap(bitmap.width, bitmap.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(output)
        val color = -0xbdbdbe
        val paint = Paint()
        val rect = Rect(0, 0, bitmap.width, bitmap.height)
        val rectF = RectF(rect)
        val roundPx = bitmap.width.toFloat()
        paint.isAntiAlias = true
        canvas.drawARGB(0, 0, 0, 0)
        paint.color = color
        canvas.drawRoundRect(rectF, roundPx, roundPx, paint)
        paint.xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC_IN)
        canvas.drawBitmap(bitmap, rect, rect, paint)
        return output
    }

    private fun drawableToBitmap(drawable: Drawable): Bitmap {
        if (drawable is BitmapDrawable) {
            return drawable.bitmap
        }
        var width = drawable.intrinsicWidth
        width = if (width > 0) width else faceDimensions
        var height = drawable.intrinsicHeight
        height = if (height > 0) height else faceDimensions
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        drawable.setBounds(0, 0, canvas.width, canvas.height)
        drawable.draw(canvas)
        return bitmap
    }

    @BindingAdapter("imageProvider", "contact")
    fun displayFaceInViewAsync(view: ImageView, imageProvider: Int?, c: FusedContact?) {
        setImageViewAsync(view, c);
    }

    companion object {
        private val memoryCache: ContactBitmapMemoryCache = ContactBitmapMemoryCache()
    }

    override fun getContactImageProvider(): ContactImageProvider {
        return this
    }
}