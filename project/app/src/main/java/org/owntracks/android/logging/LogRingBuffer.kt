package org.owntracks.android.logging

import java.util.Collections

class LogRingBuffer(capacity: Int) {
    private val actualCapacity = if (capacity > 0) capacity else DEFAULT_CAPACITY
    private val buffer: Array<LogEntry?> = arrayOfNulls(actualCapacity)
    private var headPosition: Int = 0
    fun add(value: LogEntry) {
        synchronized(buffer) {
            buffer[headPosition] = value
            headPosition = (headPosition + 1) % actualCapacity
        }
    }

    fun all(): List<LogEntry> {
        val logList: MutableList<LogEntry?>
        synchronized(buffer) {
            logList = buffer.toMutableList()
            Collections.rotate(logList, -headPosition)
        }
        return logList.filterNotNull()
    }

    fun clear() {
        synchronized(buffer) {
            headPosition = 0
            buffer.fill(null)
        }
    }

    companion object {
        const val DEFAULT_CAPACITY = 10
    }
}
