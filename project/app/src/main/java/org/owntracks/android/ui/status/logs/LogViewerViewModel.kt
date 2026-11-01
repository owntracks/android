package org.owntracks.android.ui.status.logs

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import org.owntracks.android.logging.TimberInMemoryLogTree
import org.owntracks.android.preferences.Preferences
import timber.log.Timber

@HiltViewModel
class LogViewerViewModel @Inject constructor(private val preferences: Preferences) : ViewModel() {
  private val timberInMemoryLogTree =
      Timber.forest().filterIsInstance(TimberInMemoryLogTree::class.java).first()

  var filter =
      LogFilter(
          levels =
              LogLevel.entries.toSet().let { if (preferences.debugLog) it else it - LogLevel.DEBUG }
      )
    private set

  fun logLines() = timberInMemoryLogTree.liveLogs

  fun clearLog() {
    timberInMemoryLogTree.clear()
  }

  fun isDebugEnabled(): Boolean = preferences.debugLog

  fun setLevelEnabled(level: LogLevel, enabled: Boolean) {
    filter = filter.copy(levels = if (enabled) filter.levels + level else filter.levels - level)
    if (level == LogLevel.DEBUG) {
      preferences.debugLog = enabled
    }
  }

  fun setQuery(query: String) {
    filter = filter.copy(query = query)
  }
}
