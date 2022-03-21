package org.owntracks.android.support

import android.content.SharedPreferences
import org.owntracks.android.support.preferences.PreferencesStore

class InMemoryPreferencesStore : PreferencesStore {
    private val valueMap: MutableMap<String, Any> = HashMap()
    override fun getSharedPreferencesName(): String {
        return ""
    }

    override fun setMode(key: String, mode: Int) {
        valueMap[key] = mode
    }

    override fun getInitMode(key: String, default: Int): Int {
        return getInt(key, default)
    }

    override fun getBoolean(key: String, default: Boolean): Boolean {
        return valueMap[key] as Boolean? ?: default
    }

    override fun putBoolean(key: String, value: Boolean) {
        valueMap[key] = value
    }

    override fun putInt(key: String, value: Int) {
        valueMap[key] = value
    }

    override fun getInt(key: String, default: Int): Int {
        return valueMap[key] as Int? ?: default
    }

    override fun putString(key: String, value: String) {
        valueMap[key] = value
    }

    override fun getString(key: String, default: String): String? {
        return valueMap[key] as String? ?: default
    }

    override fun getStringSet(key: String): Set<String> {
        @Suppress("UNCHECKED_CAST")
        return (valueMap[key] ?: setOf<String>()) as Set<String>
    }

    override fun hasKey(key: String): Boolean {
        return (valueMap.keys.contains(key))
    }

    override fun putStringSet(key: String, values: Set<String>) {
        valueMap[key] = values
    }

    override fun remove(key: String) {
        valueMap.remove(key)
    }

    override fun registerOnSharedPreferenceChangeListener(listener: SharedPreferences.OnSharedPreferenceChangeListener) {

    }

    override fun unregisterOnSharedPreferenceChangeListener(listener: SharedPreferences.OnSharedPreferenceChangeListener) {
        
    }
}