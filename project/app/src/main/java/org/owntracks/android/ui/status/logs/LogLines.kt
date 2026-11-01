package org.owntracks.android.ui.status.logs

import org.owntracks.android.logging.LogEntry

/** The most recent log entries, and the rows of those that [filter] shows. */
class LogLines(private val maxEntries: Int, filter: LogFilter) {
  private val entries = ArrayDeque<LogEntry>()
  private val mutableRows = ArrayDeque<LogEntry>()

  /** How [rows] changed. A removed row is from the start, an inserted row is at the end. */
  data class Update(val removed: Boolean, val inserted: Boolean)

  val rows: List<LogEntry> = mutableRows

  /** Whether there are log entries, but [filter] hides all of them. */
  val allEntriesHidden: Boolean
    get() = entries.isNotEmpty() && mutableRows.isEmpty()

  var filter: LogFilter = filter
    set(value) {
      field = value
      mutableRows.clear()
      mutableRows.addAll(entries.filter(value::matches))
    }

  fun add(logEntry: LogEntry): Update {
    val removed = entries.size == maxEntries && filter.matches(entries.removeFirst())
    if (removed) {
      mutableRows.removeFirst()
    }
    entries.addLast(logEntry)
    val inserted = filter.matches(logEntry)
    if (inserted) {
      mutableRows.addLast(logEntry)
    }
    return Update(removed, inserted)
  }

  fun clear() {
    entries.clear()
    mutableRows.clear()
  }
}
