package org.owntracks.android.ui.status.logs

import android.graphics.Typeface
import android.text.Spannable
import android.text.SpannableString
import android.text.style.ForegroundColorSpan
import android.text.style.StyleSpan
import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import org.owntracks.android.R
import org.owntracks.android.logging.LogEntry

/**
 * RecyclerView Adapter that manages the LogLines displayed to the user.
 */
class LogEntryAdapter(private val logPalette: LogPalette) : RecyclerView.Adapter<LogViewHolder>() {
    private var logLines = arrayListOf<LogEntry>()
    private var longestLogEntry: Int = 0

    private val scrollNotifier = HorizontalScrollNotifier()

    fun setLogLines(lines: Collection<LogEntry>, showDebugLogs: Boolean) {
        logLines.clear()
        val expandedForMultiline = lines
                .filter { (it.priority >= Log.DEBUG && showDebugLogs) || it.priority >= Log.INFO }
                .flatMap { logEntry -> logEntry.message.split("\n").map { LogEntry(logEntry.priority, logEntry.tag, it, logEntry.time) } }
        longestLogEntry = expandedForMultiline.maxByOrNull { it.toString().length }.toString().length
        logLines.addAll(expandedForMultiline)
        notifyDataSetChanged()
        scrollNotifier.notify(0)
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

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): LogViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.log_viewer_entry, parent, false)
        return LogViewHolder(view)
    }

    override fun onBindViewHolder(holder: LogViewHolder, position: Int) {
        val line = logLines[position]
        val spannable = if (position > 0 && logLines[position - 1].tag == line.tag && line.message.startsWith("\tat "))
            SpannableString(line.message.prependIndent().padEnd(longestLogEntry))
        else
            SpannableString(line.toString().padEnd(longestLogEntry)).apply {
                setSpan(StyleSpan(Typeface.BOLD), line.time.length, line.time.length + "${line.priorityChar} ${line.tag}:".length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
                setSpan(ForegroundColorSpan(levelToColor(line.priority)), line.time.length, line.time.length + "${line.priorityChar} ${line.tag}:".length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
            }
        holder.layout.apply {
            findViewById<TextView>(R.id.log_msg).apply {
                setSingleLine()
                text = spannable
            }
        }
        holder.bind(scrollNotifier)
    }

    override fun onViewRecycled(holder: LogViewHolder) {
        super.onViewRecycled(holder)
        holder.unbind(scrollNotifier)
    }
}
