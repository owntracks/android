package org.owntracks.android.ui.map

import android.content.Context
import android.graphics.Canvas
import android.util.AttributeSet
import android.view.View
import android.widget.TextView
import timber.log.Timber

/**
 * Auto resizing text view with listener.
 *
 * Derived from https://stackoverflow.com/a/52445825/352740
 *
 * @constructor
 *
 * @param context
 */
class AutoResizingTextViewWithListener : TextView {
    constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int) : super(
        context,
        attrs,
        defStyleAttr
    )

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs)
    constructor(context: Context) : super(context)

    private var listener: OnTextSizeChangedListener? = null
    private var previousTextSize = 0f
    override fun onDraw(canvas: Canvas?) {
        super.onDraw(canvas)
        Timber.tag("PARP").i("DRAW $this $previousTextSize, ${this.textSize}")
        if (previousTextSize != this.textSize) {
            previousTextSize = this.textSize
            Timber.tag("PARP").i("firing listener $listener")
            listener?.onTextSizeChanged(this, previousTextSize)
        }
    }

    fun withListener(listener: OnTextSizeChangedListener) {
        this.listener = listener
    }

    interface OnTextSizeChangedListener {
        fun onTextSizeChanged(view: View, newSize: Float)
    }
}
