package org.owntracks.android.logging

import android.util.Log
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class LogEntry(
    val priority: Int,
    val tag: String?,
    val message: String,
    val threadName: String,
    val time: Date = Date(System.currentTimeMillis()),
) {
  private val priorityChar =
      when (priority) {
        Log.ASSERT -> "A"
        Log.ERROR -> "E"
        Log.WARN -> "W"
        Log.INFO -> "I"
        Log.DEBUG -> "D"
        Log.VERBOSE -> "V"
        else -> "U"
      }

  companion object {
    private val longDateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.ROOT)
    private val shortDateFormat = SimpleDateFormat("MM-dd HH:mm:ss.SSS", Locale.ROOT)
    private val stackFrameRegex = Regex("^at${Typography.nbsp}.*\$", RegexOption.MULTILINE)
  }

  /**
   * Returns the string indexes of the boundaries of the priority and tag components, so that we can
   * highlight those
   *
   * @return a [Pair<Int,Int>] of string indexes representing the exclusive start and end
   */
  fun sliceLength(): Pair<Int, Int> =
      shortDateFormat.format(time).length.let { Pair(it, it + " $priorityChar $tag".length) }

  override fun toString(): String =
      "${shortDateFormat.format(time)} $priorityChar $tag\n" +
          message.trimEnd().replace("\tat ", "at${Typography.nbsp}")

  /**
   * Returns the ranges of [toString] that hold stack trace frames, so that we can indent those
   *
   * @return the [IntRange] of each frame
   */
  fun stackFrames(): List<IntRange> = stackFrameRegex.findAll(toString()).map { it.range }.toList()

  /**
   * Long date format string, for exporting to files / shares
   *
   * @return
   */
  fun toExportedString(): String =
      "${longDateFormat.format(time)} $priorityChar [$threadName] $tag: $message"
}
