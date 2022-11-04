package org.owntracks.android.preferences

import android.content.SharedPreferences
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty

/***
 * Allows a preferences class to read and write values from some sort of store
 */
interface PreferencesStore {
    fun getSharedPreferencesName(): String

    fun putBoolean(key: String, value: Boolean)
    fun getBoolean(key: String, default: Boolean): Boolean

    fun putInt(key: String, value: Int)
    fun getInt(key: String, default: Int): Int

    fun putFloat(key: String, value: Float)
    fun getFloat(key: String, default: Float): Float

    fun putString(key: String, value: String)
    fun getString(key: String, default: String): String?

    fun putStringSet(key: String, values: Set<String>)
    fun getStringSet(key: String): Set<String>

    fun hasKey(key: String): Boolean

    fun remove(key: String)

    fun registerOnSharedPreferenceChangeListener(
        listener: SharedPreferences.OnSharedPreferenceChangeListener
    )

    fun unregisterOnSharedPreferenceChangeListener(
        listener: SharedPreferences.OnSharedPreferenceChangeListener
    )
}

class SomethingDelegate<in R, T> () : ReadWriteProperty<R,T> {
    override fun getValue(thisRef: R, property: KProperty<*>): T {
        TODO("Not yet implemented")
    }

    override fun setValue(thisRef: R, property: KProperty<*>, value: T) {
        TODO("Not yet implemented")
    }

}
