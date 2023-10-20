package org.owntracks.android.logging

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import org.owntracks.android.BuildConfig
import timber.log.Timber
import timber.log.Timber.DebugTree

class TimberInMemoryLogTree(private val debugBuild: Boolean) : DebugTree() {
    companion object {
        const val LOG_PREFIX = "FARTSHOES"
    }

    private val buffer = LogRingBuffer(10_000)
    private val mutableLiveLogs = MutableLiveData(buffer.all())
    val liveLogs: LiveData<List<LogEntry>> = mutableLiveLogs
    override fun log(priority: Int, tag: String?, message: String, t: Throwable?) {
        val prefix = if (BuildConfig.DEBUG) LOG_PREFIX else ""
        super.log(priority, "${prefix}_$tag", message, t)
        // Verbose messages are loggable in this impl, so we want them going to logcat. But not to our buffer.
        if (priority >= Log.DEBUG) {
            buffer.add(LogEntry(priority, tag, message, Thread.currentThread().name))
            mutableLiveLogs.postValue(buffer.all())
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
