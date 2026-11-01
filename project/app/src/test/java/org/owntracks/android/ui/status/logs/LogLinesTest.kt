package org.owntracks.android.ui.status.logs

import android.util.Log
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.owntracks.android.logging.LogEntry

internal class LogLinesTest {
  private fun entry(message: String, priority: Int = Log.INFO) =
      LogEntry(priority, "Tag", message, "main")

  private fun LogLines.messages() = rows.map { it.message }

  @Test
  fun `given a multi-line entry, when adding it, then it is a single row`() {
    val logLines = LogLines(10, LogFilter())
    val stackTrace = "Failed\n\tat one(One.kt:1)\n\tat two(Two.kt:2)"
    assertEquals(LogLines.Update(removed = false, inserted = true), logLines.add(entry(stackTrace)))
    assertEquals(listOf(stackTrace), logLines.messages())
  }

  @Test
  fun `given an entry that the filter hides, when adding it, then no row is inserted`() {
    val logLines = LogLines(10, LogFilter(levels = setOf(LogLevel.ERROR)))
    assertEquals(LogLines.Update(false, false), logLines.add(entry("hidden")))
    assertTrue(logLines.rows.isEmpty())
  }

  @Test
  fun `given a full buffer, when adding an entry, then the oldest entry goes`() {
    val logLines = LogLines(2, LogFilter())
    logLines.add(entry("a"))
    logLines.add(entry("b"))
    assertEquals(LogLines.Update(removed = true, inserted = true), logLines.add(entry("c")))
    assertEquals(listOf("b", "c"), logLines.messages())
  }

  @Test
  fun `given a full buffer whose oldest entry is hidden, when adding an entry, then no row is removed`() {
    val logLines = LogLines(2, LogFilter(levels = setOf(LogLevel.ERROR)))
    logLines.add(entry("hidden"))
    logLines.add(entry("shown", Log.ERROR))
    assertEquals(LogLines.Update(false, true), logLines.add(entry("also shown", Log.ERROR)))
    assertEquals(listOf("shown", "also shown"), logLines.messages())
  }

  @Test
  fun `given a full buffer, when adding a hidden entry, then the oldest entry still goes`() {
    val logLines = LogLines(2, LogFilter(levels = setOf(LogLevel.ERROR)))
    logLines.add(entry("first", Log.ERROR))
    logLines.add(entry("second", Log.ERROR))
    assertEquals(LogLines.Update(true, false), logLines.add(entry("hidden")))
    assertEquals(listOf("second"), logLines.messages())
  }

  @Test
  fun `given entries, when changing the filter, then rows are rebuilt and can be restored`() {
    val logLines = LogLines(10, LogFilter())
    logLines.add(entry("info"))
    logLines.add(entry("error", Log.ERROR))
    logLines.filter = LogFilter(levels = setOf(LogLevel.ERROR))
    assertEquals(listOf("error"), logLines.messages())
    logLines.filter = LogFilter()
    assertEquals(listOf("info", "error"), logLines.messages())
  }

  @Test
  fun `given entries all hidden by the filter, when checking, then all entries are hidden`() {
    val logLines = LogLines(10, LogFilter(query = "nothing"))
    assertFalse(logLines.allEntriesHidden)
    logLines.add(entry("hidden"))
    assertTrue(logLines.allEntriesHidden)
    logLines.clear()
    assertFalse(logLines.allEntriesHidden)
  }
}
