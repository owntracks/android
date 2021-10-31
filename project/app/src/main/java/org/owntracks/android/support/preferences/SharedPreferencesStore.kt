package org.owntracks.android.support.preferences

import android.content.Context
import android.content.SharedPreferences
import androidx.preference.PreferenceManager
import dagger.hilt.android.qualifiers.ApplicationContext
import org.owntracks.android.R
import org.owntracks.android.services.MessageProcessorEndpointHttp
import org.owntracks.android.services.MessageProcessorEndpointMqtt
import org.owntracks.android.support.Preferences
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

/***
 * Implements a PreferencesStore that uses a SharedPreferecnces as a backend.
 */
@Singleton
class SharedPreferencesStore @Inject constructor(@ApplicationContext context: Context) :
    PreferencesStore {
    private lateinit var sharedPreferencesName: String
    private val activeSharedPreferencesChangeListener =
        LinkedList<OnModeChangedPreferenceChangedListener>()

    private lateinit var activeSharedPreferences: SharedPreferences
    private val commonSharedPreferences: SharedPreferences =
        PreferenceManager.getDefaultSharedPreferences(context)

    private val privateSharedPreferences: SharedPreferences =
        context.getSharedPreferences(FILENAME_PRIVATE, Context.MODE_PRIVATE)
    private val httpSharedPreferences: SharedPreferences =
        context.getSharedPreferences(FILENAME_HTTP, Context.MODE_PRIVATE)

    // Some preferences are always read from commonSharedPreferences. We list these out so that we can use the right store when these keys are requested.
    private val commonPreferenceKeys: Set<String> = setOf(
        context.getString(R.string.preferenceKeyFirstStart),
        context.getString(R.string.preferenceKeySetupNotCompleted),
        context.getString(R.string.preferenceKeyObjectboxMigrated),
        Preferences.preferenceKeyUserDeclinedEnableLocationPermissions,
        Preferences.preferenceKeyUserDeclinedEnableLocationServices
    )

    override fun putString(key: String, value: String) {
        activeSharedPreferences.edit().putString(key, value).apply()
    }

    override fun getString(key: String, default: String): String? =
        activeSharedPreferences.getString(key, default)

    override fun remove(key: String) {
        activeSharedPreferences.edit().remove(key).apply()
    }

    override fun getBoolean(key: String, default: Boolean): Boolean =
        if (commonPreferenceKeys.contains(key)) commonSharedPreferences.getBoolean(
            key,
            default
        ) else activeSharedPreferences.getBoolean(key, default)


    override fun putBoolean(key: String, value: Boolean) {
        if (commonPreferenceKeys.contains(key)) commonSharedPreferences.edit()
            .putBoolean(key, value).apply() else activeSharedPreferences.edit()
            .putBoolean(key, value).apply()
    }

    override fun getInt(key: String, default: Int): Int =
        activeSharedPreferences.getInt(key, default)


    override fun putInt(key: String, value: Int) {
        activeSharedPreferences.edit().putInt(key, value).apply()
    }

    override fun getSharedPreferencesName(): String = sharedPreferencesName

    override fun putStringSet(key: String, values: Set<String>) {
        activeSharedPreferences.edit().putStringSet(key, values).apply()
    }

    override fun getStringSet(key: String): Set<String> {
        return activeSharedPreferences.getStringSet(key, setOf()) ?: setOf()
    }

    override fun hasKey(key: String): Boolean {
        return this::activeSharedPreferences.isInitialized && activeSharedPreferences.contains(key)
    }

    override fun getInitMode(key: String, default: Int): Int {
        val initMode = commonSharedPreferences.getInt(key, default)
        return if (initMode in listOf(
                MessageProcessorEndpointMqtt.MODE_ID,
                MessageProcessorEndpointHttp.MODE_ID
            )
        ) {
            initMode
        } else {
            default
        }
    }

    override fun setMode(key: String, mode: Int) {
        detachAllActivePreferenceChangeListeners()
        when (mode) {
            MessageProcessorEndpointMqtt.MODE_ID -> {
                activeSharedPreferences = privateSharedPreferences
                sharedPreferencesName = FILENAME_PRIVATE
            }
            MessageProcessorEndpointHttp.MODE_ID -> {
                activeSharedPreferences = httpSharedPreferences
                sharedPreferencesName = FILENAME_HTTP
            }
        }
        commonSharedPreferences.edit().putInt(key, mode).apply()
        // Mode switcher reads from currently active sharedPreferences, so we commit the value to all
        privateSharedPreferences.edit().putInt(key, mode).apply()
        httpSharedPreferences.edit().putInt(key, mode).apply()

        attachAllActivePreferenceChangeListeners()
    }

    override fun registerOnSharedPreferenceChangeListener(listenerModeChanged: OnModeChangedPreferenceChangedListener) {
        if (this::activeSharedPreferences.isInitialized) {
            activeSharedPreferences.registerOnSharedPreferenceChangeListener(listenerModeChanged)
        }
        activeSharedPreferencesChangeListener.push(listenerModeChanged)
    }

    override fun unregisterOnSharedPreferenceChangeListener(listenerModeChanged: OnModeChangedPreferenceChangedListener) {
        if (this::activeSharedPreferences.isInitialized) {
            activeSharedPreferences.unregisterOnSharedPreferenceChangeListener(listenerModeChanged)
        }
        activeSharedPreferencesChangeListener.remove(listenerModeChanged)
    }

    private fun detachAllActivePreferenceChangeListeners() {
        activeSharedPreferencesChangeListener.forEach {
            activeSharedPreferences.unregisterOnSharedPreferenceChangeListener(
                it
            )
        }
    }

    private fun attachAllActivePreferenceChangeListeners() {
        activeSharedPreferencesChangeListener.forEach {
            activeSharedPreferences.registerOnSharedPreferenceChangeListener(it)
            it.onAttachAfterModeChanged()
        }
    }

    companion object {
        private const val FILENAME_PRIVATE = "org.owntracks.android.preferences.private"
        private const val FILENAME_HTTP = "org.owntracks.android.preferences.http"
    }
}

