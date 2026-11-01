package org.owntracks.android.ui.status.logs

import android.util.Log
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.owntracks.android.logging.LogEntry

internal class LogFilterTest {
  private fun entry(priority: Int = Log.INFO, tag: String? = "MessageProcessor") =
      LogEntry(priority, tag, "Connection refused", "main")

  @Test
  fun `given each log priority, when finding its level, then the matching level is returned`() {
    mapOf(
            Log.ASSERT to LogLevel.ERROR,
            Log.ERROR to LogLevel.ERROR,
            Log.WARN to LogLevel.WARNING,
            Log.INFO to LogLevel.INFO,
            Log.DEBUG to LogLevel.DEBUG,
            Log.VERBOSE to LogLevel.DEBUG,
        )
        .forEach { (priority, level) -> assertEquals(level, LogLevel.forPriority(priority)) }
  }

  @Test
  fun `given a default filter, when matching any entry, then it matches`() {
    assertTrue(LogFilter().matches(entry(Log.DEBUG)))
  }

  @Test
  fun `given a filter without a level, when matching an entry of that level, then it does not match`() {
    assertFalse(LogFilter(levels = setOf(LogLevel.ERROR)).matches(entry(Log.INFO)))
  }

  @Test
  fun `given a query, when matching against the tag or message, then case is ignored`() {
    assertTrue(LogFilter(query = "messagep").matches(entry()))
    assertTrue(LogFilter(query = "REFUSED").matches(entry()))
  }

  @Test
  fun `given a query, when neither the tag nor the message contain it, then it does not match`() {
    assertFalse(LogFilter(query = "geocoder").matches(entry()))
  }

  @Test
  fun `given a query, when the tag is null, then the message is still searched`() {
    assertTrue(LogFilter(query = "refused").matches(entry(tag = null)))
    assertFalse(LogFilter(query = "null").matches(entry(tag = null)))
  }

  @Test
  fun `given a matching query and a non-matching level, when matching, then it does not match`() {
    assertFalse(
        LogFilter(levels = setOf(LogLevel.ERROR), query = "refused").matches(entry(Log.INFO))
    )
  }
}
