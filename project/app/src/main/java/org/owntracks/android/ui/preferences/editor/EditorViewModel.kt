package org.owntracks.android.ui.preferences.editor

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import org.owntracks.android.data.waypoints.WaypointsRepo
import org.owntracks.android.model.Parser
import org.owntracks.android.preferences.Preferences
import org.owntracks.android.support.MessageWaypointCollection
import timber.log.Timber

@HiltViewModel
class EditorViewModel
@Inject
constructor(
    private val preferences: Preferences,
    private val parser: Parser,
    private val waypointsRepo: WaypointsRepo
) : ViewModel(), Preferences.OnPreferenceChangeListener {
  private val mutableEffectiveConfiguration = MutableStateFlow("")
  val effectiveConfiguration: StateFlow<String> = mutableEffectiveConfiguration

  private val mutableConfigLoadError = MutableStateFlow<Exception?>(null)
  val configLoadError: StateFlow<Exception?> = mutableConfigLoadError

  init {
    preferences.registerOnPreferenceChangedListener(this)
    updateEffectiveConfiguration()
  }

  override fun onCleared() {
    super.onCleared()
    preferences.unregisterOnPreferenceChangedListener(this)
  }

  val preferenceKeys = preferences.allConfigKeys.map { it.name }.toList()

  private fun updateEffectiveConfiguration() {
    viewModelScope.launch(Dispatchers.IO) {
      try {
        val message = preferences.exportToMessage()
        message.waypoints =
            MessageWaypointCollection().apply {
              addAll(waypointsRepo.getAll().map(waypointsRepo::fromDaoObject))
            }
        mutableEffectiveConfiguration.value = parser.toUnencryptedJsonPretty(message)
      } catch (e: Exception) {
        Timber.e(e)
        mutableConfigLoadError.value = e
      }
    }
  }

  fun setNewPreferenceValue(key: String, value: String) {
    preferences.importKeyValue(key, value)
  }

  override fun onPreferenceChanged(properties: Set<String>) {
    updateEffectiveConfiguration()
  }
}
