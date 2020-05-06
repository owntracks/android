package org.owntracks.android.support

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import android.os.Environment
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat.getColor
import org.owntracks.android.R
import timber.log.Timber
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
        Timber.i("External storage state: %s", Environment.getExternalStorageState())
        val logDir = context.getExternalFilesDir("logs")
        val pattern = File(logDir, String.format("%s-%%u.txt", dateformat))
        val filehandler = FileHandler(pattern.absolutePath, true)
        filehandler.formatter = NoFormatter()
        this.logger.level = Level.ALL
        this.logger.addHandler(filehandler)
        showDebugEnabledNotification(context, logDir!!.path)
    }

    private fun showDebugEnabledNotification(context: Context, logDir: String) {
        val notificationManager = NotificationManagerCompat.from(context)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(NOTIFICATION_CHANNEL_DEBUG, context.getString(R.string.debugChannelOngoing), NotificationManager.IMPORTANCE_LOW)
            notificationManager.createNotificationChannel(channel)
        }

        notificationManager.notify(Companion.DEBUG_NOTIFICATION_ID,
                NotificationCompat.Builder(context, "OwntracksDebugNotificationChannel")
                        .setColor(getColor(context, R.color.primary))
                        .setAutoCancel(true)
                        .setChannelId(NOTIFICATION_CHANNEL_DEBUG)
                        .setPriority(NotificationCompat.PRIORITY_MAX)
                        .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                        .setBadgeIconType(NotificationCompat.BADGE_ICON_SMALL)
                        .setStyle(NotificationCompat.BigTextStyle().bigText("Writing to %s".format(logDir)))
                        .setSmallIcon(R.drawable.ic_notification)
                        .setContentTitle("Debug Logging Enabled")
                        .build()
        )
    }

    private fun skipLog(priority: Int): Boolean {
        return priorityFilter.skipLog(priority)
    }

    private fun format(priority: Int, tag: String?, message: String): String {
        return formatter.format(priority, tag, message)
    }

    override fun log(priority: Int, tag: String?, message: String, t: Throwable?) {
        if (skipLog(priority)) {
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

    companion object {
        const val NOTIFICATION_CHANNEL_DEBUG = "D"
        const val DEBUG_NOTIFICATION_ID: Int = 100
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
    fun skipLog(priority: Int): Boolean {
        return priority < minPriority
    }

}