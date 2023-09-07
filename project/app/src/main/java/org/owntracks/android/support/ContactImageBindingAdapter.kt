package org.owntracks.android.support

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.util.Base64
import android.widget.ImageView
import androidx.databinding.BindingAdapter
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.owntracks.android.model.Contact
import org.owntracks.android.support.widgets.TextDrawable
import timber.log.Timber

class ContactImageBindingAdapter @Inject constructor(
    @ApplicationContext context: Context,
    private val memoryCache: ContactBitmapAndNameMemoryCache
) {
    @BindingAdapter(value = ["contact", "coroutineScope"])
    fun ImageView.displayFaceInViewAsync(contact: Contact?, scope: CoroutineScope) {
        contact?.also {
            scope.launch(Dispatchers.Main) {
                setImageBitmap(getBitmapFromCache(it))
            }
        }
    }

    private val faceDimensions = (48 * (context.resources.displayMetrics.densityDpi / 160f)).toInt()

    suspend fun getBitmapFromCache(contact: Contact): Bitmap {
        Timber.v("Getting face bitmap for ${contact.id}")
        return withContext(Dispatchers.IO) {
            val contactBitMapAndName = memoryCache[contact.id]

            if (contactBitMapAndName != null &&
                contactBitMapAndName is ContactBitmapAndName.CardBitmap &&
                contactBitMapAndName.bitmap != null
            ) {
                return@withContext contactBitMapAndName.bitmap
            }

            return@withContext contact.messageCard?.run {
                face?.run {
                    Base64.decode(toByteArray(), Base64.DEFAULT)
                }?.run {
                    BitmapFactory.decodeByteArray(this, 0, size)
                }?.run {
                    getRoundedShape(
                        Bitmap.createScaledBitmap(
                            this,
                            faceDimensions,
                            faceDimensions,
                            true
                        )
                    )
                }?.also { bitmap ->
                    memoryCache.put(
                        contact.id,
                        ContactBitmapAndName.CardBitmap(name, bitmap)
                    )
                }
            } ?: run {
                memoryCache[contact.id]?.run {
                    if (this is ContactBitmapAndName.TrackerIdBitmap && this.trackerId == contact.trackerId) {
                        this.bitmap
                    } else {
                        null
                    }
                } ?: run {
                    getFallbackBitmap(contact.trackerId, contact.id).also { bitmap ->
                        memoryCache.put(
                            contact.id,
                            ContactBitmapAndName.TrackerIdBitmap(contact.trackerId, bitmap)
                        )
                    }
                }
            }
        }
    }

    private fun getFallbackBitmap(text: String, colorKey: String): Bitmap =
        drawableToBitmap(
            TextDrawable
                .Builder()
                .buildRoundRect(
                    text,
                    TextDrawable.ColorGenerator.MATERIAL.getColor(colorKey),
                    faceDimensions
                )
        )

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
}
