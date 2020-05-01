package org.owntracks.android.support

import android.content.Context
import android.util.Log
import timber.log.Timber.DebugTree
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import java.util.logging.*
import java.util.logging.Formatter
import kotlin.collections.HashMap

class TimberDebugLogFileTree(context: Context) : DebugTree() {
    private val priorityFilter: PriorityFilter = PriorityFilter(Log.DEBUG)
    private val logger: Logger = Logger.getGlobal()
    private val formatter = LogcatFormatter.INSTANCE

    init {
        val dateformat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        val logDir = context.getExternalFilesDir("logs")
        val pattern = File(logDir, String.format("%s-%%u.txt", dateformat))
        val filehandler = FileHandler(pattern.absolutePath, true)
        filehandler.formatter = NoFormatter()
        this.logger.level = Level.ALL
        this.logger.addHandler(filehandler)
    }

    private fun skipLog(priority: Int, tag: String?, message: String, t: Throwable?): Boolean {
        return priorityFilter.skipLog(priority, tag, message, t)
    }

    private fun format(priority: Int, tag: String?, message: String): String {
        return formatter.format(priority, tag, message)
    }

    override fun log(priority: Int, tag: String?, message: String, t: Throwable?) {
        if (skipLog(priority, tag, message, t)) {
            return
        }
        val formattedMsg = format(priority, tag, message)
        logger.log(fromPriorityToLevel(priority), formattedMsg)
        if (t != null) {
            logger.log(fromPriorityToLevel(priority), "", t)
        }
    }

    private fun fromPriorityToLevel(priority: Int): Level {
        return when (priority) {
            Log.VERBOSE -> Level.FINER
            Log.DEBUG -> Level.FINE
            Log.INFO -> Level.INFO
            Log.WARN -> Level.WARNING
            Log.ERROR, Log.ASSERT -> Level.SEVERE
            else -> Level.FINEST
        }
    }
}

private class NoFormatter : Formatter() {
    override fun format(record: LogRecord): String {
        return record.message
    }
}

internal class LogcatFormatter {
    private val prioPrefixes = HashMap<Int, String>()
    fun format(priority: Int, tag: String?, message: String): String {
        var prio = prioPrefixes[priority]
        if (prio == null) {
            prio = ""
        }
        val date = Date(System.currentTimeMillis())
        val sdf = SimpleDateFormat("MM-dd HH:mm:ss:SSS", Locale.US)
        sdf.timeZone = TimeZone.getDefault()
        return String.format(Locale.ROOT, "%s%s%s%s(%d) :%s%s\n", sdf.format(date), SEP, prio, tag
                ?: "", Thread.currentThread().id, SEP, message)
    }

    companion object {
        val INSTANCE = LogcatFormatter()
        private const val SEP = " "
    }

    init {
        prioPrefixes[Log.VERBOSE] = "V/"
        prioPrefixes[Log.DEBUG] = "D/"
        prioPrefixes[Log.INFO] = "I/"
        prioPrefixes[Log.WARN] = "W/"
        prioPrefixes[Log.ERROR] = "E/"
        prioPrefixes[Log.ASSERT] = "WTF/"
    }
}

internal class PriorityFilter(private val minPriority: Int) {
    fun skipLog(priority: Int, tag: String?, message: String?, t: Throwable?): Boolean {
        return priority < minPriority
    }

    fun isLoggable(priority: Int, tag: String?): Boolean {
        return priority >= minPriority
    }

}