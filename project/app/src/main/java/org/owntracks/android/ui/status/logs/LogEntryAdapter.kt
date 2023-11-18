package org.owntracks.android.ui.status.logs

import android.graphics.Typeface
import android.text.Spannable
import android.text.SpannableString
import android.text.style.ForegroundColorSpan
import android.text.style.StyleSpan
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import org.owntracks.android.R
import org.owntracks.android.logging.LogEntry

/**
 * RecyclerView Adapter that manages the LogLines displayed to the user.
 */
class LogEntryAdapter(
    private val logPalette: LogPalette
) : RecyclerView.Adapter<LogEntryAdapter.ViewHolder>() {
    private val logLines = mutableListOf<LogEntry>()

    fun clearLogs() {
        logLines.clear()
        // Need to clear the whole thing out here
        notifyDataSetChanged()
    }

    fun addLogLine(logEntry: LogEntry) {
        val explodedLines = logEntry.message.split("\n")
            .filter { it.isNotBlank() }
            .map { LogEntry(logEntry.priority, logEntry.tag, it, logEntry.threadName, logEntry.time) }
        logLines.addAll(explodedLines)
        notifyItemRangeInserted(logLines.size - explodedLines.size, explodedLines.size)
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

    override fun getItemCount() = logLines.size

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(
            LayoutInflater.from(parent.context).inflate(R.layout.log_viewer_entry, parent, false)
        )
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        logLines.run {
            val line = this[position]
            val spannable = if (position > 0 && this[position - 1].tag == line.tag && line.message.startsWith(
                    "\tat "
                )
            ) {
                SpannableString(
                    line.message.prependIndent()
                )
            } else {
                SpannableString(line.toString()).apply {
                    line.sliceLength().let {
                        setSpan(
                            StyleSpan(Typeface.BOLD),
                            it.first,
                            it.second,
                            Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                        )
                        setSpan(
                            ForegroundColorSpan(levelToColor(line.priority)),
                            it.first,
                            it.second,
                            Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                        )
                    }
                }
            }

            holder.layout.apply {
                findViewById<TextView>(R.id.log_msg).apply {
                    setSingleLine()
                    text = spannable
                }
            }
        }
    }

    class ViewHolder(val layout: View) : RecyclerView.ViewHolder(layout)
}
