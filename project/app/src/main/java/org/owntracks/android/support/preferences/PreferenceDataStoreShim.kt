package org.owntracks.android.support.preferences

import androidx.preference.PreferenceDataStore

/***
 * Provides some helper methods around a PreferenceDataStore
 */
abstract class PreferenceDataStoreShim : PreferenceDataStore() {
    abstract fun contains(key: String): Boolean

    abstract fun remove(key: String)

    abstract fun registerOnSharedPreferenceChangeListener(listenerModeChanged: OnModeChangedPreferenceChangedListener)
    abstract fun unregisterOnSharedPreferenceChangeListener(listenerModeChanged: OnModeChangedPreferenceChangedListener)
}
