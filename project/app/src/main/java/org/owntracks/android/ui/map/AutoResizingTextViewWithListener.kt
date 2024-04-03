package org.owntracks.android.ui.map

import android.annotation.SuppressLint
import android.content.Context
import android.content.res.Configuration
import android.graphics.Canvas
import android.util.AttributeSet
import android.util.TypedValue
import android.view.View
import android.widget.TextView
import androidx.appcompat.widget.AppCompatTextView

/**
 * Auto resizing text view with listener.
 *
 * Derived from https://stackoverflow.com/a/52445825/352740
 *
 * @constructor As [TextView]
 */
@SuppressLint("RestrictedApi")
class AutoResizingTextViewWithListener : AppCompatTextView {
  constructor(
      context: Context,
      attrs: AttributeSet,
      defStyleAttr: Int
  ) : super(context, attrs, defStyleAttr)

  constructor(context: Context, attrs: AttributeSet) : super(context, attrs)

  constructor(context: Context) : super(context)

  private var listener: OnTextSizeChangedListener? = null
  private var previousTextSize = 0f
  private val originalAutoSizeMinTextSize = this.autoSizeMinTextSize
  private val originalAutoSizeMaxTextSize = this.autoSizeMaxTextSize

  var configurationChangedFlag = false

  override fun onDraw(canvas: Canvas) {
    super.onDraw(canvas)
    if (previousTextSize != this.textSize && listener != null) {
      previousTextSize = this.textSize
      listener?.onTextSizeChanged(this, previousTextSize)
    }
  }

  fun withListener(listener: OnTextSizeChangedListener) {
    this.listener = listener
  }

  /** Fired when the text size of this [TextView] changes */
  interface OnTextSizeChangedListener {
    fun onTextSizeChanged(view: View, newSize: Float)
  }

  override fun onConfigurationChanged(newConfig: Configuration?) {
    super.onConfigurationChanged(newConfig)
    setAutoSizeTextTypeUniformWithConfiguration(
        originalAutoSizeMinTextSize, originalAutoSizeMaxTextSize, 1, TypedValue.COMPLEX_UNIT_PX)
    configurationChangedFlag = true
    previousTextSize = 0f
  }
}
