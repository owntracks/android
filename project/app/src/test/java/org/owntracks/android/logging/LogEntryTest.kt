package org.owntracks.android.logging

import android.util.Log
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import org.junit.Assert.assertEquals
import org.junit.Test

internal class LogEntryTest {
  private val time = Date(1_700_000_000_123L)
  private val shortTime = SimpleDateFormat("MM-dd HH:mm:ss.SSS", Locale.ROOT).format(time)
  private val longTime = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.ROOT).format(time)

  private fun entry(priority: Int = Log.INFO, tag: String? = "Tag/method/1") =
      LogEntry(priority, tag, "a message", "main", time)

  @Test
  fun `given a log entry, when converting to a string, then the header and message are on separate lines`() {
    assertEquals("$shortTime I Tag/method/1\na message", entry().toString())
  }

  @Test
  fun `given a stack trace, when converting to a string, then it stays in one entry with each frame kept with its at`() {
    val entry =
        LogEntry(Log.WARN, "Tag", "Failed\n\tat one(One.kt:1)\n\tat two(Two.kt:2)\n", "main", time)
    assertEquals(
        "$shortTime W Tag\nFailed\nat${Typography.nbsp}one(One.kt:1)\nat${Typography.nbsp}two(Two.kt:2)",
        entry.toString(),
    )
  }

  @Test
  fun `given a stack trace, when finding frames, then each frame line is found and nothing else`() {
    val entry =
        LogEntry(
            Log.WARN,
            "Tag",
            "at the door\n\tat one(One.kt:1)\nCaused by: x\n\tat two(Two.kt:2)",
            "main",
            time,
        )
    val text = entry.toString()
    assertEquals(
        listOf("at${Typography.nbsp}one(One.kt:1)", "at${Typography.nbsp}two(Two.kt:2)"),
        entry.stackFrames().map { text.substring(it) },
    )
  }

  @Test
  fun `given no stack trace, when finding frames, then there are none`() {
    assertEquals(emptyList<IntRange>(), entry().stackFrames())
  }

  @Test
  fun `given a stack trace, when exporting, then the original tabs are kept`() {
    val entry = LogEntry(Log.WARN, "Tag", "Failed\n\tat one(One.kt:1)", "main", time)
    assertEquals("$longTime W [main] Tag: Failed\n\tat one(One.kt:1)", entry.toExportedString())
  }

  @Test
  fun `given a log entry, when slicing, then the slice covers the priority and tag up to the end of the header`() {
    val entry = entry()
    val (start, end) = entry.sliceLength()
    assertEquals(" I Tag/method/1", entry.toString().substring(start, end))
    assertEquals('\n', entry.toString()[end])
  }

  @Test
  fun `given each log priority, when converting to a string, then the matching priority character is used`() {
    mapOf(
            Log.ASSERT to "A",
            Log.ERROR to "E",
            Log.WARN to "W",
            Log.INFO to "I",
            Log.DEBUG to "D",
            Log.VERBOSE to "V",
            0 to "U",
        )
        .forEach { (priority, character) ->
          assertEquals(
              "$shortTime $character Tag\na message",
              entry(priority, "Tag").toString(),
          )
        }
  }

  @Test
  fun `given a log entry, when exporting, then it is a single line with the full date and thread name`() {
    assertEquals("$longTime I [main] Tag/method/1: a message", entry().toExportedString())
  }
}
