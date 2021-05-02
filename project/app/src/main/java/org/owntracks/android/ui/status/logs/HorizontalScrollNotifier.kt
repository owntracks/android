package org.owntracks.android.ui.status.logs

/**
 * Allows LogViewHolders to subscribe and unsubscribe from scroll events sent by other LogViewHolders
 */
class HorizontalScrollNotifier {
    private var currentScrollPosition: Int = 0
    private val logViewHolders = mutableListOf<LogViewHolder>()
    fun subscribe(logViewHolder: LogViewHolder) {
        logViewHolders.add(logViewHolder)
        logViewHolder.horizontalScroll(currentScrollPosition)
    }

    fun unsubscribe(logViewHolder: LogViewHolder) {
        logViewHolders.remove(logViewHolder)
    }

    fun notify(scrollPosition: Int) {
        currentScrollPosition = scrollPosition
        logViewHolders.forEach { it.horizontalScroll(currentScrollPosition) }
    }
}