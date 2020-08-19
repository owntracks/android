package org.owntracks.android.support.widgets

import android.content.Context
import android.util.AttributeSet
import android.view.MotionEvent
import androidx.viewpager.widget.ViewPager

class PausableViewPager(context: Context?, attrs: AttributeSet?) : ViewPager(context!!, attrs) {
    override fun onInterceptTouchEvent(event: MotionEvent): Boolean {
        return false
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        performClick()
        return false
    }

    override fun performClick(): Boolean {
        super.performClick()
        return false
    }
}