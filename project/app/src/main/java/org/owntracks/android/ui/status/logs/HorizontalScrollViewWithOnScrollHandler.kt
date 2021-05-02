package org.owntracks.android.ui.status.logs

import android.content.Context
import android.util.AttributeSet
import android.widget.HorizontalScrollView

/**
 * A HorizontalScrollView that will broadcast horizontal scroll events to a given handler
 */
class HorizontalScrollViewWithOnScrollHandler : HorizontalScrollView {
    private var onHorizontalScrollHandler: ((Int, Int) -> Unit)? = null

    constructor(context: Context) : super(context)
    constructor(context: Context, attributeSet: AttributeSet) : super(context, attributeSet)

    override fun onScrollChanged(l: Int, t: Int, oldl: Int, oldt: Int) {
        onHorizontalScrollHandler?.run { invoke(oldl, l) }
        super.onScrollChanged(l, t, oldl, oldt)
    }

    fun setOnScrollHandler(onHorizontalScrollHandler: (Int, Int) -> Unit) {
        this.onHorizontalScrollHandler = onHorizontalScrollHandler
    }
}