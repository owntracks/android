package org.owntracks.android.support.preferences

import android.content.SharedPreferences

/***
 * Allows a preferences class to read and write values from some sort of store
 */
interface PreferencesStore {
    fun getSharedPreferencesName(): String

    fun setMode(key: String, mode: Int)
    fun getInitMode(key: String, default: Int): Int

    fun putBoolean(key: String, value: Boolean)
    fun getBoolean(key: String, default: Boolean): Boolean

    fun putInt(key: String, value: Int)
    fun getInt(key: String, default: Int): Int

    fun putString(key: String, value: String)
    fun getString(key: String, default: String): String?

    fun putStringSet(key: String, values: Set<String>)
    fun getStringSet(key: String): Set<String>

    fun hasKey(key: String): Boolean

    fun remove(key: String)

    fun registerOnSharedPreferenceChangeListener(listener: SharedPreferences.OnSharedPreferenceChangeListener)
    fun unregisterOnSharedPreferenceChangeListener(listener: SharedPreferences.OnSharedPreferenceChangeListener)
}
