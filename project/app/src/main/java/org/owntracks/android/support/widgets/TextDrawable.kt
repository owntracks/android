package org.owntracks.android.support.widgets

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ColorFilter
import android.graphics.Paint
import android.graphics.PixelFormat
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.drawable.ShapeDrawable
import android.graphics.drawable.shapes.OvalShape
import android.graphics.drawable.shapes.RectShape
import android.graphics.drawable.shapes.RoundRectShape
import java.util.Locale
import kotlin.math.abs
import kotlin.math.min

// Based on https://github.com/amulyakhare/TextDrawable with some fixes
class TextDrawable private constructor(builder: Builder) : ShapeDrawable(builder.shape) {
    private val textPaint: Paint
    private val borderPaint: Paint
    private val text: String
    private val shape: RectShape
    private val height: Int
    private val width: Int
    private val fontSize: Int
    private val radius: Float
    private val borderThickness: Int

    init {
        // shape properties
        shape = builder.shape
        height = builder.height
        width = builder.width
        radius = builder.radius

        // text and color
        text = if (builder.toUpperCase) builder.text.uppercase(Locale.getDefault()) else builder.text

        // text paint settings
        fontSize = builder.fontSize
        textPaint = Paint().apply {
            this.color = builder.textColor
            isAntiAlias = true
            isFakeBoldText = builder.isBold
            style = Paint.Style.FILL
            typeface = TYPEFACE
            textAlign = Paint.Align.CENTER
            strokeWidth = builder.borderThickness.toFloat()
        }

        // border paint settings
        borderThickness = builder.borderThickness
        borderPaint = Paint().apply {
            color = getDarkerShade(builder.color)
            style = Paint.Style.STROKE
            strokeWidth = borderThickness.toFloat()
        }
        paint.color = builder.color
    }

    private fun getDarkerShade(color: Int): Int {
        return Color.rgb(
            (SHADE_FACTOR * Color.red(color)).toInt(),
            (SHADE_FACTOR * Color.green(color)).toInt(),
            (SHADE_FACTOR * Color.blue(color)).toInt()
        )
    }

    override fun draw(canvas: Canvas) {
        super.draw(canvas)
        val r = bounds

        // draw border
        if (borderThickness > 0) {
            drawBorder(canvas)
        }
        val count = canvas.save()
        canvas.translate(r.left.toFloat(), r.top.toFloat())

        // draw text
        val width = if (width < 0) r.width() else width
        val height = if (height < 0) r.height() else height
        val fontSize = if (fontSize < 0) min(width, height) / 2 else fontSize
        textPaint.textSize = fontSize.toFloat()
        canvas.drawText(
            text,
            (width / 2).toFloat(),
            height.toFloat() / 2 - (textPaint.descent() + textPaint.ascent()) / 2,
            textPaint
        )
        canvas.restoreToCount(count)
    }

    private fun drawBorder(canvas: Canvas) {
        val rect = RectF(bounds)
        rect.inset((borderThickness / 2).toFloat(), (borderThickness / 2).toFloat())
        when (shape) {
            is OvalShape -> {
                canvas.drawOval(rect, borderPaint)
            }
            is RoundRectShape -> {
                canvas.drawRoundRect(rect, radius, radius, borderPaint)
            }
            else -> {
                canvas.drawRect(rect, borderPaint)
            }
        }
    }

    override fun setAlpha(alpha: Int) {
        textPaint.alpha = alpha
    }

    override fun setColorFilter(cf: ColorFilter?) {
        textPaint.colorFilter = cf
    }

    @Deprecated("Deprecated in Java", ReplaceWith("PixelFormat.TRANSLUCENT", "android.graphics.PixelFormat"))
    override fun getOpacity(): Int {
        return PixelFormat.TRANSLUCENT
    }

    override fun getIntrinsicWidth(): Int {
        return width
    }

    override fun getIntrinsicHeight(): Int {
        return height
    }

    class Builder : IShapeBuilder, IBuilder {
        internal var text = ""
        internal var color: Int
        internal var borderThickness: Int
        internal var width: Int
        internal var height: Int
        internal var shape: RectShape
        var textColor: Int
        internal var fontSize: Int
        internal var isBold: Boolean
        internal var toUpperCase: Boolean
        var radius = 0f

        init {
            color = Color.GRAY
            textColor = Color.WHITE
            borderThickness = 0
            width = -1
            height = -1
            shape = RectShape()
            fontSize = -1
            isBold = false
            toUpperCase = false
        }

        override fun roundRect(radius: Int): IBuilder {
            this.radius = radius.toFloat()
            val radii = floatArrayOf(
                radius.toFloat(),
                radius.toFloat(),
                radius.toFloat(),
                radius.toFloat(),
                radius.toFloat(),
                radius.toFloat(),
                radius.toFloat(),
                radius.toFloat()
            )
            shape = RoundRectShape(radii, null, null)
            return this
        }

        override fun buildRoundRect(text: String, color: Int, radius: Int): TextDrawable {
            roundRect(radius)
            return build(text, color)
        }

        override fun build(text: String, color: Int): TextDrawable {
            this.color = color
            this.text = text
            return TextDrawable(this)
        }
    }

    interface IBuilder {
        fun build(text: String, color: Int): TextDrawable
    }

    interface IShapeBuilder {
        fun roundRect(radius: Int): IBuilder
        fun buildRoundRect(text: String, color: Int, radius: Int): TextDrawable
    }

    class ColorGenerator private constructor(private val mColors: List<Int>) {
        companion object {
            var MATERIAL: ColorGenerator = create(
                listOf(
                    -0x1a8c8d,
                    -0xf9d6e,
                    -0x459738,
                    -0x6a8a33,
                    -0x867935,
                    -0x9b4a0a,
                    -0xb03c09,
                    -0xb22f1f,
                    -0xb24954,
                    -0x7e387c,
                    -0x512a7f,
                    -0x759b,
                    -0x2b1ea9,
                    -0x2ab1,
                    -0x48b3,
                    -0x5e7781,
                    -0x6f5b52
                )
            )

            fun create(colorList: List<Int>): ColorGenerator {
                return ColorGenerator(colorList)
            }
        }

        fun getColor(key: Any): Int {
            return mColors[abs(key.hashCode()) % mColors.size]
        }
    }

    companion object {
        private const val SHADE_FACTOR = 0.9f
        private val TYPEFACE = Typeface.create("sans-serif-light", Typeface.NORMAL)
    }
}
