package org.owntracks.android.ui.status.logs

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.core.app.ShareCompat
import androidx.core.net.toUri
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.flowWithLifecycle
import androidx.lifecycle.lifecycleScope
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlin.random.Random
import org.owntracks.android.preferences.Preferences
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import org.owntracks.android.BuildConfig
import org.owntracks.android.R
import org.owntracks.android.logging.LogEntry
import org.owntracks.android.ui.theme.OwnTracksTheme
import timber.log.Timber

@AndroidEntryPoint
class LogViewerActivity : AppCompatActivity() {
    @Inject
    lateinit var preferences: Preferences

    val viewModel: LogViewerViewModel by viewModels()

    private val shareIntentActivityLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {}

    private var logExportUri: Uri? = null
    private var collectorJob: Job? = null

    // State for Compose
    private val logEntries = mutableStateListOf<LogEntry>()
    private var isDebugEnabled by mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        isDebugEnabled = viewModel.isDebugEnabled()

        setContent {
            OwnTracksTheme(dynamicColor = preferences.dynamicColorsEnabled) {
                LogViewerScreen(
                    logEntries = logEntries,
                    isDebugEnabled = isDebugEnabled,
                    onBackClick = { finish() },
                    onShareClick = { shareLogFile() },
                    onClearClick = {
                        viewModel.clearLog()
                        restartLogCollector()
                    },
                    onToggleDebug = { enabled ->
                        isDebugEnabled = enabled
                        viewModel.enableDebugLogs(enabled)
                        restartLogCollector()
                    },
                    modifier = Modifier.fillMaxSize()
                )
            }
        }

        restartLogCollector()
    }

    private fun restartLogCollector() {
        collectorJob?.cancel("Restarting")
        logEntries.clear()
        collectorJob = lifecycleScope.launch {
            viewModel.logLines()
                .flowWithLifecycle(lifecycle, Lifecycle.State.STARTED)
                .collect { logEntry ->
                    if (isDebugEnabled || logEntry.priority >= Log.INFO) {
                        addLogEntry(logEntry)
                    }
                }
        }
    }

    private fun addLogEntry(logEntry: LogEntry) {
        // Split multi-line log entries
        val explodedLines = logEntry.message
            .split("\n")
            .filter { it.isNotBlank() }
            .map {
                LogEntry(logEntry.priority, logEntry.tag, it, logEntry.threadName, logEntry.time)
            }
        logEntries.addAll(explodedLines)
    }

    private fun shareLogFile() {
        val key = "${getRandomHexString()}/debug=${viewModel.isDebugEnabled()}/owntracks-debug.txt"
        logExportUri = "content://${BuildConfig.APPLICATION_ID}.log/$key".toUri()
        val shareIntent = ShareCompat.IntentBuilder(this)
            .setType("text/plain")
            .setSubject(getString(R.string.exportLogFileSubject))
            .setChooserTitle(R.string.exportLogFilePrompt)
            .setStream(logExportUri)
            .createChooserIntent()
            .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            .also { Timber.v("Created share intent of $logExportUri") }
        grantUriPermission("android", logExportUri, Intent.FLAG_GRANT_READ_URI_PERMISSION).also {
            Timber.v("Granted READ_URI_PERMISSION permission to $logExportUri")
        }
        shareIntentActivityLauncher.launch(shareIntent)
    }

    private fun getRandomHexString(): String {
        return Random.nextInt(0X1000000).toString(16)
    }
}
