package org.owntracks.android.logging

import android.util.Log
import java.text.SimpleDateFormat
import java.util.*

data class LogEntry(val priority: Int, val tag: String?, val message: String, val time: String = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.ROOT).format(Date(System.currentTimeMillis()))) {
    val priorityChar = when (priority) {
        Log.ASSERT -> "A"
        Log.ERROR -> "E"
        Log.WARN -> "W"
        Log.INFO -> "I"
        Log.DEBUG -> "D"
        Log.VERBOSE -> "V"
        else -> "U"
    }

    override fun toString(): String {
        return "$time $priorityChar $tag: $message"
    }
}
