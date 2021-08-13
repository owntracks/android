package org.owntracks.android.support.preferences

import android.content.SharedPreferences
import androidx.preference.PreferenceDataStore

/***
 * Provides some helper methods around a PreferenceDataStore
 */
abstract class PreferenceDataStoreShim : PreferenceDataStore() {
    abstract fun contains(key: String): Boolean

    abstract fun remove(key: String)

    abstract fun registerOnSharedPreferenceChangeListener(listenerModeChanged: SharedPreferences.OnSharedPreferenceChangeListener)
    abstract fun unregisterOnSharedPreferenceChangeListener(listenerModeChanged: SharedPreferences.OnSharedPreferenceChangeListener)
}
