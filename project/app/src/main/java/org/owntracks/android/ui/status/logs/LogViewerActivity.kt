package org.owntracks.android.ui.status.logs

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SearchView
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.app.ShareCompat
import androidx.core.net.toUri
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.doOnPreDraw
import androidx.core.view.isVisible
import androidx.core.view.updateLayoutParams
import androidx.core.view.updatePadding
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.chip.Chip
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.divider.MaterialDividerItemDecoration
import dagger.hilt.android.AndroidEntryPoint
import kotlin.random.Random
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import org.owntracks.android.BuildConfig
import org.owntracks.android.R
import org.owntracks.android.databinding.UiPreferencesLogsBinding
import org.owntracks.android.logging.LogEntry
import timber.log.Timber

@AndroidEntryPoint
class LogViewerActivity : AppCompatActivity() {
  val viewModel: LogViewerViewModel by viewModels()

  private val shareIntentActivityLauncher =
      registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {}

  private lateinit var logAdapter: LogEntryAdapter
  private var logExportUri: Uri? = null
  private var recyclerView: RecyclerView? = null
  private var clearButton: MenuItem? = null
  private lateinit var binding: UiPreferencesLogsBinding
  private var collectorJob: Job? = null

  override fun onCreate(savedInstanceState: Bundle?) {
    enableEdgeToEdge()
    super.onCreate(savedInstanceState)
    binding =
        DataBindingUtil.setContentView<UiPreferencesLogsBinding>(
                this,
                R.layout.ui_preferences_logs,
            )
            .apply {
              lifecycleOwner = this@LogViewerActivity
              setSupportActionBar(appbar.toolbar)

              // Handle window insets for edge-to-edge
              val fabBaseMarginBottom =
                  (shareFab.layoutParams as CoordinatorLayout.LayoutParams).bottomMargin
              val jumpBaseMarginBottom =
                  (jumpToLatest.layoutParams as CoordinatorLayout.LayoutParams).bottomMargin
              val logsBasePaddingBottom = logsRecyclerView.paddingBottom
              ViewCompat.setOnApplyWindowInsetsListener(frame) { _, windowInsets ->
                val insets =
                    windowInsets.getInsets(
                        WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.ime()
                    )
                appbar.root.updatePadding(top = insets.top)
                shareFab.updateLayoutParams<CoordinatorLayout.LayoutParams> {
                  bottomMargin = fabBaseMarginBottom + insets.bottom
                }
                jumpToLatest.updateLayoutParams<CoordinatorLayout.LayoutParams> {
                  bottomMargin = jumpBaseMarginBottom + insets.bottom
                }
                logsRecyclerView.updatePadding(bottom = logsBasePaddingBottom + insets.bottom)
                WindowInsetsCompat.CONSUMED
              }
            }

    supportActionBar?.apply {
      setDisplayShowHomeEnabled(true)
      setDisplayHomeAsUpEnabled(true)
    }

    @Suppress("DEPRECATION")
    logAdapter =
        LogEntryAdapter(
            LogPalette(
                resources.getColor(com.mikepenz.materialize.R.color.primary),
                resources.getColor(R.color.log_debug_tag_color),
                resources.getColor(R.color.log_info_tag_color),
                resources.getColor(R.color.log_warning_tag_color),
                resources.getColor(R.color.log_error_tag_color),
            ),
            viewModel.filter,
        )
    restartLogCollector()

    binding.logsRecyclerView.apply {
      recyclerView = this
      layoutManager = LinearLayoutManager(context)
      adapter = logAdapter
      addItemDecoration(
          MaterialDividerItemDecoration(context, LinearLayoutManager.VERTICAL).apply {
            isLastItemDecorated = false
          }
      )
      addOnScrollListener(
          object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
              binding.jumpToLatest.isVisible = recyclerView.canScrollVertically(1)
            }
          }
      )
    }
    bindLevelChip(binding.logLevelError, LogLevel.ERROR)
    bindLevelChip(binding.logLevelWarning, LogLevel.WARNING)
    bindLevelChip(binding.logLevelInfo, LogLevel.INFO)
    bindLevelChip(binding.logLevelDebug, LogLevel.DEBUG)
    binding.jumpToLatest.setOnClickListener { scrollToLatest() }
    binding.shareFab.setOnClickListener {
      val key = "${getRandomHexString()}/debug=${viewModel.isDebugEnabled()}/owntracks-debug.txt"
      logExportUri = "content://${BuildConfig.APPLICATION_ID}.log/$key".toUri()
      val shareIntent =
          ShareCompat.IntentBuilder(this)
              .setType("text/plain")
              .setSubject(getString(R.string.exportLogFileSubject))
              .setChooserTitle(R.string.exportLogFilePrompt)
              .setStream(logExportUri)
              .createChooserIntent()
              .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION) // Temporary. No need to revoke.
              .also { Timber.v("Created share intent of r$logExportUri") }
      grantUriPermission("android", logExportUri, Intent.FLAG_GRANT_READ_URI_PERMISSION).also {
        Timber.v("Granted READ_URI_PERMISSION permission to $logExportUri")
      }
      shareIntentActivityLauncher.launch(shareIntent)
    }
  }

  private fun bindLevelChip(chip: Chip, level: LogLevel) {
    chip.isChecked = level in viewModel.filter.levels
    chip.setOnCheckedChangeListener { _, isChecked ->
      viewModel.setLevelEnabled(level, isChecked)
      applyFilter()
    }
  }

  private fun applyFilter() {
    logAdapter.filter = viewModel.filter
    scrollToLatest()
    updateNoMatchingLogs()
  }

  private fun restartLogCollector() {
    collectorJob?.cancel("Restarting")
    collectorJob = lifecycleScope.launch {
      repeatOnLifecycle(Lifecycle.State.STARTED) {
        // The flow replays its whole buffer to each new collector
        logAdapter.clearLogs()
        viewModel.logLines().collect { updateAdapterWithLogLines(it) }
      }
    }
  }

  private fun updateAdapterWithLogLines(logEntry: LogEntry) {
    val atTheBottom = !binding.logsRecyclerView.canScrollVertically(1)
    logAdapter.addLogLine(logEntry)
    if (atTheBottom) {
      scrollToLatest()
    }
    updateNoMatchingLogs()
  }

  private fun updateNoMatchingLogs() {
    binding.noMatchingLogs.isVisible = logAdapter.allEntriesHidden
  }

  private fun scrollToLatest() {
    binding.logsRecyclerView.apply {
      scrollToPosition(logAdapter.itemCount - 1)
      // scrollToPosition can align the top of a row that's taller than the list
      doOnPreDraw {
        findViewHolderForAdapterPosition(logAdapter.itemCount - 1)?.let {
          scrollBy(0, it.itemView.bottom - (height - paddingBottom))
        }
      }
    }
  }

  override fun onResume() {
    super.onResume()
    scrollToLatest()
  }

  override fun onCreateOptionsMenu(menu: Menu): Boolean {
    menuInflater.inflate(R.menu.log_viewer, menu)
    clearButton = menu.findItem(R.id.clear_log)
    val searchItem = menu.findItem(R.id.search_log)
    (searchItem.actionView as SearchView).apply {
      queryHint = getString(R.string.logViewerSearch)
      val query = viewModel.filter.query
      if (query.isNotEmpty()) {
        searchItem.expandActionView()
        setQuery(query, false)
      }
      setOnQueryTextListener(
          object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String): Boolean {
              clearFocus()
              return true
            }

            override fun onQueryTextChange(newText: String): Boolean {
              viewModel.setQuery(newText)
              applyFilter()
              return true
            }
          }
      )
    }
    return true
  }

  override fun onOptionsItemSelected(item: MenuItem): Boolean {
    return when (item.itemId) {
      android.R.id.home -> {
        finish()
        true
      }
      R.id.clear_log -> {
        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.logViewerClearTitle)
            .setMessage(R.string.logViewerClearMessage)
            .setPositiveButton(R.string.clear_log) { _, _ ->
              viewModel.clearLog()
              restartLogCollector()
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
        true
      }
      else -> super.onOptionsItemSelected(item)
    }
  }

  private fun getRandomHexString(): String {
    return Random.nextInt(0X1000000).toString(16)
  }
}
