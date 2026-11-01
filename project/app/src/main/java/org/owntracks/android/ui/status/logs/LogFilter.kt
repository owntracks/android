package org.owntracks.android.ui.status.logs

import android.util.Log
import org.owntracks.android.logging.LogEntry

/** The levels that a user can choose to show in the log viewer. */
enum class LogLevel {
  ERROR,
  WARNING,
  INFO,
  DEBUG;

  companion object {
    fun forPriority(priority: Int): LogLevel =
        when {
          priority >= Log.ERROR -> ERROR
          priority == Log.WARN -> WARNING
          priority == Log.INFO -> INFO
          else -> DEBUG
        }
  }
}

/** Decides which log entries are shown in the log viewer. */
data class LogFilter(val levels: Set<LogLevel> = LogLevel.entries.toSet(), val query: String = "") {
  fun matches(logEntry: LogEntry): Boolean =
      LogLevel.forPriority(logEntry.priority) in levels &&
          (query.isEmpty() ||
              logEntry.tag?.contains(query, ignoreCase = true) == true ||
              logEntry.message.contains(query, ignoreCase = true))
}
