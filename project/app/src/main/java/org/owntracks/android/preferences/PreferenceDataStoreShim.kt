package org.owntracks.android.preferences

import androidx.preference.PreferenceDataStore
import javax.inject.Inject
import javax.inject.Singleton
import org.owntracks.android.preferences.types.AppTheme
import org.owntracks.android.preferences.types.ConnectionMode
import org.owntracks.android.preferences.types.ReverseGeocodeProvider
import org.owntracks.android.preferences.types.StringMaxTwoAlphaNumericChars

/**
 * The whole reason this exists is to give an [androidx.preference.PreferenceFragmentCompat] a thing
 * that it can poke values into when UI preferences get twiddled. The default behaviour is to
 * interact with [android.content.SharedPreferences] directly which doesn't really suit us as we've
 * shimmed that (to provide abstraction and type saftey). If you supply
 * [androidx.preference.PreferenceFragmentCompat] with a [androidx.preference.PreferenceDataStore]
 * instead, it'll use that to read/write preferences from, so we create our own that wraps around
 * [Preferences]
 *
 * @property preferences instance of [Preferences] that we're going to read/write values to and
 *   from.
 */
@Singleton
class PreferenceDataStoreShim @Inject constructor(private val preferences: Preferences) :
    PreferenceDataStore() {
  override fun getBoolean(key: String?, defValue: Boolean): Boolean {
    return (key?.run(preferences::getPreferenceByName) ?: defValue) as Boolean
  }

  override fun getFloat(key: String?, defValue: Float): Float {
    return (key?.run(preferences::getPreferenceByName) ?: defValue) as Float
  }

  override fun getInt(key: String?, defValue: Int): Int {
    val intPreferenceValue =
        when (val preferenceValue = key?.run(preferences::getPreferenceByName) ?: defValue) {
          is AppTheme -> preferenceValue.value
          is ConnectionMode -> preferenceValue.value
          else -> preferenceValue
        }
    return intPreferenceValue as Int
  }

  override fun getString(key: String?, defValue: String?): String {
    val stringPreferenceValue =
        when (val preferenceValue = key?.run(preferences::getPreferenceByName) ?: defValue) {
          is ReverseGeocodeProvider -> preferenceValue.name
          is StringMaxTwoAlphaNumericChars -> preferenceValue.toString()
          else -> preferenceValue
        }
    return stringPreferenceValue as String
  }

  @Suppress("UNCHECKED_CAST")
  override fun getStringSet(key: String?, defValues: MutableSet<String>?): MutableSet<String>? =
      (key?.run(preferences::getPreferenceByName) ?: defValues) as MutableSet<String>?

  override fun getLong(key: String?, defValue: Long): Long {
    return (key?.run(preferences::getPreferenceByName) ?: defValue) as Long
  }

  override fun putBoolean(key: String?, value: Boolean) {
    key?.run { preferences.importKeyValue(this, value) }
  }

  override fun putFloat(key: String?, value: Float) {
    key?.run { preferences.importKeyValue(this, value) }
  }

  override fun putInt(key: String?, value: Int) {
    key?.run { preferences.importKeyValue(this, value) }
  }

  override fun putString(key: String?, value: String?) {
    key?.let { notNullKey ->
      value?.let { notNullValue -> preferences.importKeyValue(notNullKey, notNullValue) }
    }
  }

  override fun putLong(key: String?, value: Long) {
    key?.run { preferences.importKeyValue(this, value) }
  }

  override fun putStringSet(key: String?, values: MutableSet<String>?) {
    key?.let { notNullKey ->
      values?.let { notNullValues -> preferences.importKeyValue(notNullKey, notNullValues) }
    }
  }
}
