package org.owntracks.android.logging

import android.util.Log
import timber.log.Timber
import timber.log.Timber.DebugTree

class TimberInMemoryLogTree(private val debugBuild: Boolean) : DebugTree() {
    private val buffer = LogRingBuffer(1_000)
    override fun log(priority: Int, tag: String?, message: String, t: Throwable?) {
        super.log(priority, tag, message, t)
        // Verbose messages are loggable in this impl, so we want them going to logcat. But not to our buffer.
        if (priority >= Log.DEBUG) {
            buffer.add(LogEntry(priority, tag, message))
        }
    }

    override fun createStackElementTag(element: StackTraceElement): String? {
        return if (debugBuild) {
            "${super.createStackElementTag(element)}/${element.methodName}/${element.lineNumber}"
        } else {
            super.createStackElementTag(element)
        }
    }

    fun logLines(): List<LogEntry> {
        return buffer.all()
    }

    fun clear() {
        buffer.clear()
        Timber.i("Logs cleared")
    }
}