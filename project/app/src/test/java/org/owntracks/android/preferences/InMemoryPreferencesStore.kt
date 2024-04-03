package org.owntracks.android.preferences

import android.content.SharedPreferences

class InMemoryPreferencesStore : PreferencesStore() {
  private val valueMap: MutableMap<String, Any> = HashMap()

  override fun getSharedPreferencesName(): String {
    return ""
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

  override fun putFloat(key: String, value: Float) {
    valueMap[key] = value
  }

  override fun getFloat(key: String, default: Float): Float {
    return valueMap[key] as Float? ?: default
  }

  override fun putString(key: String, value: String) {
    valueMap[key] = value
  }

  override fun getString(key: String, default: String): String {
    return valueMap[key] as String? ?: default
  }

  @Suppress("UNCHECKED_CAST")
  override fun getStringSet(key: String, defaultValues: Set<String>): Set<String> =
      (valueMap[key] ?: setOf<String>()) as Set<String>

  override fun hasKey(key: String): Boolean = valueMap.containsKey(key)

  override fun putStringSet(key: String, values: Set<String>) {
    valueMap[key] = values
  }

  override fun remove(key: String) {
    valueMap.remove(key)
  }

  override fun migrate() {}

  override fun registerOnSharedPreferenceChangeListener(
      listener: SharedPreferences.OnSharedPreferenceChangeListener
  ) {}

  override fun unregisterOnSharedPreferenceChangeListener(
      listener: SharedPreferences.OnSharedPreferenceChangeListener
  ) {}
}
