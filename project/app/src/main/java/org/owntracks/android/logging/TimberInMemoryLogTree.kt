package org.owntracks.android.logging

import android.util.Log
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import org.owntracks.android.BuildConfig
import timber.log.Timber
import timber.log.Timber.DebugTree

class TimberInMemoryLogTree(private val debugBuild: Boolean) : DebugTree() {
  companion object {
    const val LOG_PREFIX = "FARTSHOES"
  }

  private val mutableLogFlow =
      MutableSharedFlow<LogEntry>(replay = 10_000, onBufferOverflow = BufferOverflow.DROP_OLDEST)
  val liveLogs: SharedFlow<LogEntry> = mutableLogFlow

  override fun log(priority: Int, tag: String?, message: String, t: Throwable?) {
    val prefix = if (BuildConfig.DEBUG) LOG_PREFIX else ""
    super.log(priority, "${prefix}_$tag", message, t)
    // Verbose messages are loggable in this impl, so we want them going to logcat. But not to our
    // buffer.
    if (priority >= Log.DEBUG) {
      LogEntry(priority, tag, message, Thread.currentThread().name).run {
        mutableLogFlow.tryEmit(this)
      }
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
    return mutableLogFlow.replayCache
  }

  @OptIn(ExperimentalCoroutinesApi::class)
  fun clear() {
    mutableLogFlow.resetReplayCache()
    Timber.i("Logs cleared")
  }
}
