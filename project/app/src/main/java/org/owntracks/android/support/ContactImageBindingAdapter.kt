package org.owntracks.android.support

import android.content.Context
import android.graphics.*
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.util.Base64
import android.widget.ImageView
import androidx.databinding.BindingAdapter
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.*
import org.owntracks.android.model.FusedContact
import org.owntracks.android.support.widgets.TextDrawable
import timber.log.Timber
import javax.inject.Inject

class ContactImageBindingAdapter @Inject constructor(
    @ApplicationContext context: Context,
    private val memoryCache: ContactBitmapAndNameMemoryCache
) {
    @BindingAdapter(value = ["contact"])
    fun ImageView.displayFaceInViewAsync(c: FusedContact?) {
        c?.also { contact ->
            GlobalScope.launch(Dispatchers.Main) {
                setImageBitmap(getBitmapFromCache(contact))
            }
        }
    }

    private val faceDimensions = (48 * (context.resources.displayMetrics.densityDpi / 160f)).toInt()

    suspend fun getBitmapFromCache(contact: FusedContact): Bitmap {
        return withContext(Dispatchers.IO) {
            val contactBitMapAndName = memoryCache[contact.id]

            if (contactBitMapAndName != null && contactBitMapAndName is ContactBitmapAndName.CardBitmap && contactBitMapAndName.bitmap != null) {
                return@withContext contactBitMapAndName.bitmap
            }
            contact.messageCard?.run {
                face?.also { face ->
                    val imageAsBytes =
                        Base64.decode(face.toByteArray(), Base64.DEFAULT)
                    val b = BitmapFactory.decodeByteArray(imageAsBytes, 0, imageAsBytes.size)
                    val bitmap: Bitmap
                    if (b == null) {
                        Timber.e("Decoding card bitmap failed")
                        val fallbackBitmap = Bitmap.createBitmap(
                            faceDimensions,
                            faceDimensions,
                            Bitmap.Config.ARGB_8888
                        )
                        val canvas = Canvas(fallbackBitmap)
                        val paint = Paint()
                        paint.color = -0x1
                        canvas.drawRect(
                            0f,
                            0f,
                            faceDimensions.toFloat(),
                            faceDimensions.toFloat(),
                            paint
                        )
                        bitmap = getRoundedShape(fallbackBitmap)
                    } else {
                        bitmap = getRoundedShape(
                            Bitmap.createScaledBitmap(
                                b,
                                faceDimensions,
                                faceDimensions,
                                true
                            )
                        )
                        memoryCache.put(
                            contact.id,
                            ContactBitmapAndName.CardBitmap(name, bitmap)
                        )
                    }
                    return@withContext bitmap
                }
            }
            if (contactBitMapAndName !is ContactBitmapAndName.TrackerIdBitmap || contactBitMapAndName.trackerId != contact.trackerId) {
                val bitmap = drawableToBitmap(
                    TextDrawable
                        .Builder()
                        .buildRoundRect(
                            contact.trackerId,
                            TextDrawable.ColorGenerator.MATERIAL.getColor(contact.id),
                            faceDimensions
                        )
                )
                memoryCache.put(
                    contact.id,
                    ContactBitmapAndName.TrackerIdBitmap(contact.trackerId, bitmap)
                )

                return@withContext bitmap
            } else {
                return@withContext contactBitMapAndName.bitmap
            }
        }

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
}