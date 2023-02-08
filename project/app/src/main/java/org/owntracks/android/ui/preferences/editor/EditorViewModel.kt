package org.owntracks.android.ui.preferences.editor

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import org.owntracks.android.data.repos.WaypointsRepo
import org.owntracks.android.preferences.Preferences
import org.owntracks.android.support.Parser
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class EditorViewModel @Inject constructor(
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

    val preferenceKeys = preferences.allConfigKeys.map { it.name }
        .toList()

    private fun updateEffectiveConfiguration() {
        try {
            val message = preferences.exportToMessage()
            message.waypoints = waypointsRepo.exportToMessage()
            message[preferences::password.name] = "********"
            mutableEffectiveConfiguration.postValue(parser.toUnencryptedJsonPretty(message))
        } catch (e: Exception) {
            Timber.e(e)
            mutableConfigLoadError.postValue(e)
        }
    }

    fun setNewPreferenceValue(key: String, value: String) {
        preferences.importKeyValue(key, value)
    }

    override fun onPreferenceChanged(properties: List<String>) {
        updateEffectiveConfiguration()
    }
}
