package org.owntracks.android.ui.preferences.editor

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.owntracks.android.data.waypoints.WaypointsRepo
import org.owntracks.android.preferences.Preferences
import org.owntracks.android.support.MessageWaypointCollection
import org.owntracks.android.support.Parser
import timber.log.Timber

@HiltViewModel
class EditorViewModel
@Inject
constructor(
    private val preferences: Preferences,
    private val parser: Parser,
    private val waypointsRepo: WaypointsRepo
) : ViewModel(), Preferences.OnPreferenceChangeListener {
  private val mutableEffectiveConfiguration = MutableLiveData<String>()
  val effectiveConfiguration: LiveData<String> = mutableEffectiveConfiguration

  private val mutableConfigLoadError = MutableLiveData<Exception>()
  val configLoadError: LiveData<Exception> = mutableConfigLoadError

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
    viewModelScope.launch {
      try {
        val message = preferences.exportToMessage()
        waypointsRepo.allLive.stateIn(viewModelScope).value.let {
          message.waypoints =
              MessageWaypointCollection().apply { addAll(it.map(waypointsRepo::fromDaoObject)) }
        }
        mutableEffectiveConfiguration.postValue(parser.toUnencryptedJsonPretty(message))
      } catch (e: Exception) {
        Timber.e(e)
        mutableConfigLoadError.postValue(e)
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
