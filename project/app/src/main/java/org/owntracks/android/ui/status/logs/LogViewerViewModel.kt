package org.owntracks.android.ui.status.logs

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import org.owntracks.android.logging.TimberInMemoryLogTree
import org.owntracks.android.support.Preferences
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class LogViewerViewModel @Inject constructor(private val preferences: Preferences) : ViewModel() {
    private val timberInMemoryLogTree =
        Timber.forest().filterIsInstance(TimberInMemoryLogTree::class.java).first()

    fun logLines() = timberInMemoryLogTree.liveLogs

    fun clearLog() {
        timberInMemoryLogTree.clear()
    }

    fun isDebugEnabled(): Boolean = preferences.debugLog

    fun enableDebugLogs(enabled: Boolean) {
        preferences.debugLog = enabled
    }
}
