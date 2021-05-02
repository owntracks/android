package org.owntracks.android.ui.status.logs

import android.view.View
import androidx.recyclerview.widget.RecyclerView
import org.owntracks.android.R

/**
 * RecyclerView holder for the log line
 */
class LogViewHolder(val layout: View) : RecyclerView.ViewHolder(layout) {
    private val scrollview: HorizontalScrollViewWithOnScrollHandler = layout.findViewById(R.id.log_item_scroll_view)

    fun bind(scrollNotifier: HorizontalScrollNotifier) {
        scrollview.setOnScrollHandler { oldLeft, newLeft ->
            if (newLeft - oldLeft != 0) {
                scrollNotifier.notify(newLeft)
            }
        }
        scrollNotifier.subscribe(this)
    }

    fun unbind(scrollNotifier: HorizontalScrollNotifier) {
        scrollNotifier.unsubscribe(this)
    }

    fun horizontalScroll(currentScrollPosition: Int) {
        scrollview.scrollTo(currentScrollPosition, 0)
        if (currentScrollPosition != scrollview.scrollX) {
            scrollview.post {
                scrollview.scrollTo(currentScrollPosition, 0)
            }
        }

    }
}