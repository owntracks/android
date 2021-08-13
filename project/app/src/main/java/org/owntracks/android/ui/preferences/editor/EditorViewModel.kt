package org.owntracks.android.ui.preferences.editor

import android.content.SharedPreferences
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import org.owntracks.android.R
import org.owntracks.android.data.repos.WaypointsRepo
import org.owntracks.android.support.Parser
import org.owntracks.android.support.Preferences
import timber.log.Timber
import java.io.IOException
import javax.inject.Inject

@HiltViewModel
class EditorViewModel @Inject constructor(
        private val preferences: Preferences,
        private val parser: Parser,
        private val waypointsRepo: WaypointsRepo
) : ViewModel(), SharedPreferences.OnSharedPreferenceChangeListener {

    private val mutableConfiguration = MutableLiveData("")
    val effectiveConfiguration: LiveData<String>
        get() = mutableConfiguration

    init {
        preferences.registerOnPreferenceChangedListener(this)
        updateEffectiveConfiguration()
    }

    private fun updateEffectiveConfiguration() {
        try {
            val message = preferences.exportToMessage()
            message.waypoints = waypointsRepo.exportToMessage()
            message[preferences.getPreferenceKey(R.string.preferenceKeyPassword)] = "********"
            mutableConfiguration.postValue(parser.toUnencryptedJsonPretty(message))
        } catch (e: IOException) {
            Timber.e(e)
        }
    }

    override fun onCleared() {
        preferences.unregisterOnPreferenceChangedListener(this)
        super.onCleared()
    }

    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences?, key: String?) {
        updateEffectiveConfiguration()
    }
}