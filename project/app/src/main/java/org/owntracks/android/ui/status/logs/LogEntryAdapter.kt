package org.owntracks.android.ui.status.logs

import android.graphics.Typeface
import android.text.Spannable
import android.text.SpannableString
import android.text.style.ForegroundColorSpan
import android.text.style.LeadingMarginSpan
import android.text.style.StyleSpan
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import org.owntracks.android.R
import org.owntracks.android.logging.LogEntry
import org.owntracks.android.logging.TimberInMemoryLogTree

/** RecyclerView Adapter that manages the LogLines displayed to the user. */
class LogEntryAdapter(private val logPalette: LogPalette, filter: LogFilter) :
    RecyclerView.Adapter<LogEntryAdapter.ViewHolder>() {
  private val logLines = LogLines(TimberInMemoryLogTree.MAX_LOG_ENTRIES, filter)

  val allEntriesHidden: Boolean
    get() = logLines.allEntriesHidden

  var filter: LogFilter
    get() = logLines.filter
    set(value) {
      logLines.filter = value
      // Any row can come or go with a new filter
      notifyDataSetChanged()
    }

  fun clearLogs() {
    logLines.clear()
    // Need to clear the whole thing out here
    notifyDataSetChanged()
  }

  fun addLogLine(logEntry: LogEntry) {
    val (removed, inserted) = logLines.add(logEntry)
    if (removed) {
      notifyItemRemoved(0)
    }
    if (inserted) {
      notifyItemInserted(logLines.rows.size - 1)
    }
  }

  private fun levelToColor(level: Int): Int {
    return when (level) {
      Log.DEBUG -> logPalette.debug
      Log.ERROR -> logPalette.error
      Log.INFO -> logPalette.info
      Log.WARN -> logPalette.warning
      else -> logPalette.default
    }
  }

  override fun getItemCount() = logLines.rows.size

  override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
    return ViewHolder(
        LayoutInflater.from(parent.context).inflate(R.layout.log_viewer_entry, parent, false)
    )
  }

  override fun onBindViewHolder(holder: ViewHolder, position: Int) {
    val line = logLines.rows[position]
    val spannable =
        SpannableString(line.toString()).apply {
          line.sliceLength().let {
            setSpan(
                StyleSpan(Typeface.BOLD),
                it.first,
                it.second,
                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE,
            )
            setSpan(
                ForegroundColorSpan(levelToColor(line.priority)),
                it.first,
                it.second,
                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE,
            )
          }
          val indent = (16 * holder.layout.resources.displayMetrics.density).toInt()
          line.stackFrames().forEach {
            setSpan(
                LeadingMarginSpan.Standard(indent),
                it.first,
                it.last + 1,
                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE,
            )
          }
        }

    holder.layout.apply { findViewById<TextView>(R.id.log_msg).text = spannable }
  }

  class ViewHolder(val layout: View) : RecyclerView.ViewHolder(layout)
}
