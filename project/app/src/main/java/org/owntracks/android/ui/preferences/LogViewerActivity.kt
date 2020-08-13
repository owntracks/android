/*
 * Copyright © 2020 WireGuard LLC. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

/*
Derived and adapted from the wireguard implementation taken from: https://github.com/WireGuard/wireguard-android/blob/1.0.20200724/ui/src/main/java/com/wireguard/android/activity/LogViewerActivity.kt

Adaptations include:
* Allowing option to clear logs
* Removing or simplifying  wireguard-specific implementation details
*/

package org.owntracks.android.ui.preferences

import android.content.ContentProvider
import android.content.ContentValues
import android.content.Intent
import android.database.Cursor
import android.database.MatrixCursor
import android.graphics.Typeface
import android.net.Uri
import android.os.Bundle
import android.os.ParcelFileDescriptor
import android.provider.OpenableColumns
import android.text.Spannable
import android.text.SpannableString
import android.text.style.ForegroundColorSpan
import android.text.style.StyleSpan
import android.view.*
import androidx.core.app.ShareCompat
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.textview.MaterialTextView
import kotlinx.coroutines.*
import org.owntracks.android.BuildConfig
import org.owntracks.android.R
import org.owntracks.android.databinding.UiPreferencesLogsBinding
import org.owntracks.android.ui.base.BaseActivity
import org.owntracks.android.ui.base.view.MvvmView
import org.owntracks.android.ui.base.viewmodel.NoOpViewModel
import timber.log.Timber
import java.io.BufferedReader
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStreamReader
import java.nio.charset.StandardCharsets
import java.text.DateFormat
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import java.util.regex.Matcher
import java.util.regex.Pattern

class LogViewerActivity : BaseActivity<UiPreferencesLogsBinding, NoOpViewModel>(), MvvmView {
    private data class LogLine(val pid: Int, val tid: Int, val time: Date?, val level: String, val tag: String, var msg: String)

    private lateinit var logAdapter: LogEntryAdapter
    private lateinit var streamingJob: Job
    private var logExportUri: Uri? = null
    private var logLines = arrayListOf<LogLine>()
    private var rawLogLines = StringBuffer()
    private var recyclerView: RecyclerView? = null
    private val coroutineScope = CoroutineScope(Dispatchers.Default)
    private var clearButton: MenuItem? = null

    private val year by lazy {
        val yearFormatter: DateFormat = SimpleDateFormat("yyyy", Locale.US)
        yearFormatter.format(Date())
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        bindAndAttachContentView(R.layout.ui_preferences_logs, savedInstanceState)
        setSupportToolbar(binding.toolbar)
        setHasEventBus(false)
        logAdapter = LogEntryAdapter()

        binding.recyclerView.apply {
            recyclerView = this
            layoutManager = LinearLayoutManager(context)
            adapter = logAdapter
            addItemDecoration(DividerItemDecoration(context, LinearLayoutManager.VERTICAL))
        }

        streamingJob = coroutineScope.launch { streamingLog() }

        binding.shareFab.setOnClickListener {
            revokeExportUriPermissions()
            val key = getRandomHexString()
            LOGS[key] = rawLogLines.toString().toByteArray(Charsets.UTF_8)
            logExportUri = Uri.parse("content://${BuildConfig.APPLICATION_ID}.log/$key")
            val shareIntent = ShareCompat.IntentBuilder.from(this)
                    .setType("text/plain")
                    .setSubject("Owntracks Log File")
                    .setChooserTitle(R.string.exportLogFilePrompt)
                    .setStream(logExportUri)
                    .createChooserIntent()
                    .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            grantUriPermission("android", logExportUri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
            startActivityForResult(shareIntent, SHARE_ACTIVITY_REQUEST)
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.log_viewer, menu)
        clearButton = menu?.findItem(R.id.clear_log)
        return true
    }
    override fun onPrepareOptionsMenu(menu: Menu): Boolean {
        menu.findItem(R.id.show_debug_logs).isChecked = preferences.debugLog
        return true
    }
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                finish()
                true
            }
            R.id.clear_log -> {
                coroutineScope.launch { clearLog() }
                true
            }
            R.id.show_debug_logs -> {
                item.isChecked = !item.isChecked
                preferences.debugLog = item.isChecked
                streamingJob.cancel("Debug settings changed")
                streamingJob = coroutineScope.launch { streamingLog() }
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == SHARE_ACTIVITY_REQUEST) {
            revokeExportUriPermissions()
        }
        super.onActivityResult(requestCode, resultCode, data)
    }

    override fun onDestroy() {
        super.onDestroy()
        coroutineScope.cancel()
    }

    private fun getRandomHexString(): String {
        return Random().nextInt(0X1000000).toString(16)
    }

    private fun revokeExportUriPermissions() {
        logExportUri?.let {
            LOGS.remove(it.pathSegments.lastOrNull())
            revokeUriPermission(it, Intent.FLAG_GRANT_READ_URI_PERMISSION)
            logExportUri = null
        }
    }


    private suspend fun clearLog() = withContext(Dispatchers.IO) {
        val builder = ProcessBuilder().command("logcat", "-c")
        builder.environment()["LC_ALL"] = "C"
        try {
            builder.start()
        } catch (e: IOException) {
            Timber.e(e, "Unable to start process to clear log")
            return@withContext
        }

        Timber.i("Logs cleared")
        withContext(Dispatchers.Main) {
            recyclerView?.let {
                logLines.clear()
                logAdapter.notifyDataSetChanged()
            }
        }
    }

    private suspend fun streamingLog() = withContext(Dispatchers.IO) {
        val builder = ProcessBuilder().command("logcat", "-v", "threadtime", if (preferences.debugLog) "*:D" else "*:I")
        builder.environment()["LC_ALL"] = "C"
        val process = try {
            builder.start()
        } catch (e: IOException) {
            Timber.e(e, "Unable to start process to stream log")
            return@withContext
        }
        val stdout = BufferedReader(InputStreamReader(process!!.inputStream, StandardCharsets.UTF_8))
        var haveScrolled = false
        val start = System.nanoTime()
        var startPeriod = start
        logLines.clear()
        rawLogLines.setLength(0)
        while (true) {
            val line = stdout.readLine() ?: break
            rawLogLines.append(line)
            rawLogLines.append('\n')
            val logLine = parseLine(line)
            withContext(Dispatchers.Main) {
                if (logLine != null) {
                    recyclerView?.let {
                        val shouldScroll = haveScrolled && !it.canScrollVertically(1)
                        logLines.add(logLine)
                        if (haveScrolled) logAdapter.notifyDataSetChanged()
                        if (shouldScroll)
                            it.scrollToPosition(logLines.size - 1)
                    }
                } else {
                    /* I'd prefer for the next line to be:
                     *    logLines.lastOrNull()?.msg += "\n$line"
                     * However, as of writing, that causes the kotlin compiler to freak out and crash, spewing bytecode.
                     */
                    logLines.lastOrNull()?.apply { msg += "\n$line" }
                    if (haveScrolled) logAdapter.notifyDataSetChanged()
                }
                if (!haveScrolled) {
                    val end = System.nanoTime()
                    val scroll = (end - start) > 1000000000L * 2.5 || !stdout.ready()
                    if (logLines.isNotEmpty() && (scroll || (end - startPeriod) > 1000000000L / 4)) {
                        logAdapter.notifyDataSetChanged()
                        recyclerView?.scrollToPosition(logLines.size - 1)
                        startPeriod = end
                    }
                    if (scroll) haveScrolled = true
                }
            }
        }
    }

    private fun parseTime(timeStr: String): Date? {
        val formatter: DateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.US)
        return try {
            formatter.parse("$year-$timeStr")
        } catch (e: ParseException) {
            null
        }
    }

    private fun parseLine(line: String): LogLine? {
        val m: Matcher = THREADTIME_LINE.matcher(line)
        return if (m.matches()) {
            LogLine(m.group(2)!!.toInt(), m.group(3)!!.toInt(), parseTime(m.group(1)!!), m.group(4)!!, m.group(5)!!, m.group(6)!!)
        } else {
            null
        }
    }

    companion object {
        private val THREADTIME_LINE: Pattern = Pattern.compile("^(\\d{2}-\\d{2} \\d{2}:\\d{2}:\\d{2}.\\d{3})(?:\\s+[0-9A-Za-z]+)?\\s+(\\d+)\\s+(\\d+)\\s+([A-Z])\\s+(.+?)\\s*: (.*)$")
        private val LOGS: MutableMap<String, ByteArray> = ConcurrentHashMap()
        private const val SHARE_ACTIVITY_REQUEST = 45043
    }

    private inner class LogEntryAdapter : RecyclerView.Adapter<LogEntryAdapter.ViewHolder>() {

        @Suppress("Deprecation")
        private val defaultColor by lazy { resources.getColor(R.color.primary) }

        @Suppress("Deprecation")
        private val debugColor by lazy { resources.getColor(R.color.log_debug_tag_color) }

        @Suppress("Deprecation")
        private val errorColor by lazy { resources.getColor(R.color.log_error_tag_color) }

        @Suppress("Deprecation")
        private val infoColor by lazy { resources.getColor(R.color.log_info_tag_color) }

        @Suppress("Deprecation")
        private val warningColor by lazy { resources.getColor(R.color.log_warning_tag_color) }

        private inner class ViewHolder(val layout: View, var isSingleLine: Boolean = true) : RecyclerView.ViewHolder(layout)

        private fun levelToColor(level: String): Int {
            return when (level) {
                "D" -> debugColor
                "E" -> errorColor
                "I" -> infoColor
                "W" -> warningColor
                else -> defaultColor
            }
        }

        override fun getItemCount() = logLines.size

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context)
                    .inflate(R.layout.log_viewer_entry, parent, false)
            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val line = logLines[position]
            val spannable = if (position > 0 && logLines[position - 1].tag == line.tag)
                SpannableString(line.msg)
            else
                SpannableString("${line.tag}: ${line.msg}").apply {
                    setSpan(StyleSpan(Typeface.BOLD), 0, "${line.tag}:".length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
                    setSpan(ForegroundColorSpan(levelToColor(line.level)),
                            0, "${line.tag}:".length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
                }
            holder.layout.apply {
                findViewById<MaterialTextView>(R.id.log_date).text = line.time.toString()
                findViewById<MaterialTextView>(R.id.log_msg).apply {
                    setSingleLine()
                    text = spannable
                    setOnClickListener {
                        isSingleLine = !holder.isSingleLine
                        holder.isSingleLine = !holder.isSingleLine
                    }
                }
            }
        }
    }

    class ExportedLogContentProvider : ContentProvider() {
        private fun logForUri(uri: Uri): ByteArray? = LOGS[uri.pathSegments.lastOrNull()]
        override fun insert(uri: Uri, values: ContentValues?): Uri? {
            TODO("Not yet implemented")
        }

        override fun query(uri: Uri, projection: Array<out String>?, selection: String?, selectionArgs: Array<out String>?, sortOrder: String?): Cursor? =
                logForUri(uri)?.let {
                    val m = MatrixCursor(arrayOf(OpenableColumns.DISPLAY_NAME, OpenableColumns.SIZE), 1)
                    m.addRow(arrayOf("owntracks-log.txt", it.size.toLong()))
                    m
                }


        override fun onCreate(): Boolean = true

        override fun update(uri: Uri, values: ContentValues?, selection: String?, selectionArgs: Array<out String>?): Int {
            TODO("Not yet implemented")
        }

        override fun delete(uri: Uri, selection: String?, selectionArgs: Array<out String>?): Int {
            TODO("Not yet implemented")
        }

        override fun getType(uri: Uri): String? = "text/plain"
        override fun openFile(uri: Uri, mode: String): ParcelFileDescriptor? {
            if (mode != "r") return null
            val log = logForUri(uri) ?: return null
            return openPipeHelper(uri, "text/plain", null, log) { output, _, _, _, l ->
                try {
                    FileOutputStream(output.fileDescriptor).write(l!!)
                } catch (e: Exception) {
                    Timber.e(e,"Can't write logs to output")
                }
            }
        }
    }
}