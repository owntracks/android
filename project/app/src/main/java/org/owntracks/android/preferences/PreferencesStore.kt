package org.owntracks.android.preferences

import android.content.SharedPreferences
import java.io.Closeable
import kotlin.reflect.KProperty
import kotlin.reflect.typeOf
import org.owntracks.android.preferences.types.AppTheme
import org.owntracks.android.preferences.types.ConnectionMode
import org.owntracks.android.preferences.types.MonitoringMode
import org.owntracks.android.preferences.types.MqttProtocolLevel
import org.owntracks.android.preferences.types.MqttQos
import org.owntracks.android.preferences.types.ReverseGeocodeProvider
import org.owntracks.android.preferences.types.StringMaxTwoAlphaNumericChars
import org.owntracks.android.ui.map.MapLayerStyle
import timber.log.Timber

/**
 * Allows a preferences class to read and write values from some sort of store
 */
abstract class PreferencesStore :
    DefaultsProvider by DefaultsProviderImpl(),
    CoercionsProvider by CoercionsProviderImpl() {
    abstract fun getSharedPreferencesName(): String

    abstract fun putBoolean(key: String, value: Boolean)
    abstract fun getBoolean(key: String, default: Boolean): Boolean

    abstract fun putInt(key: String, value: Int)
    abstract fun getInt(key: String, default: Int): Int

    abstract fun putFloat(key: String, value: Float)
    abstract fun getFloat(key: String, default: Float): Float

    abstract fun putString(key: String, value: String)
    abstract fun getString(key: String, default: String): String?

    abstract fun putStringSet(key: String, values: Set<String>)
    abstract fun getStringSet(key: String, defaultValues: Set<String>): Set<String>

    abstract fun hasKey(key: String): Boolean

    abstract fun remove(key: String)

    abstract fun registerOnSharedPreferenceChangeListener(
        listener: SharedPreferences.OnSharedPreferenceChangeListener
    )

    abstract fun unregisterOnSharedPreferenceChangeListener(
        listener: SharedPreferences.OnSharedPreferenceChangeListener
    )

    /**
     * For getting, we have to maybe assume that the type that we're passed of the property will be the same as what was
     * previously written to the store, and then just throw caution to the wind and cast it to that thing. If this
     * explodes, then just grab the default
     *
     * @param T type of the property to be returned
     * @param preferences instance of [Preferences] that's asking for the property value
     * @param property the actual field on the [Preferences] class
     * @return The preference value as pulled from the [PreferencesStore], or the default if there's an error
     */
    @Suppress("UNCHECKED_CAST", "IMPLICIT_CAST_TO_ANY")
    operator fun <T> getValue(preferences: Preferences, property: KProperty<*>): T =
        if (hasKey(property.name)) {
            try {
                when (property.returnType) {
                    typeOf<Boolean>() -> getBoolean(property.name, false)
                    typeOf<String>() -> getString(property.name, "")
                    typeOf<Int>() -> getInt(property.name, 0)
                    typeOf<Float>() -> getFloat(property.name, 0f)
                    typeOf<Set<String>>() -> getStringSet(property.name, emptySet())
                    typeOf<ReverseGeocodeProvider>() -> ReverseGeocodeProvider.getByValue(
                        getString(
                            property.name,
                            ""
                        ) ?: ""
                    )
                    typeOf<MapLayerStyle>() -> MapLayerStyle.valueOf(getString(property.name, "") ?: "")
                    typeOf<ConnectionMode>() -> ConnectionMode.getByValue(getInt(property.name, -1))
                    typeOf<MonitoringMode>() -> MonitoringMode.getByValue(getInt(property.name, 1))
                    typeOf<MqttProtocolLevel>() -> MqttProtocolLevel.getByValue(getInt(property.name, 3))
                    typeOf<MqttQos>() -> MqttQos.getByValue(getInt(property.name, 1))
                    typeOf<AppTheme>() -> AppTheme.getByValue(getInt(property.name, 0))
                    typeOf<StringMaxTwoAlphaNumericChars>() -> StringMaxTwoAlphaNumericChars(
                        getString(
                            property.name,
                            ""
                        ) ?: ""
                    )
                    else -> throw UnsupportedPreferenceTypeException(
                        "Trying to get property ${property.name} has type ${property.returnType}"
                    )
                } as T
            } catch (e: java.lang.ClassCastException) {
                getAndSetDefault(preferences, property)
            } catch (e: java.lang.IllegalArgumentException) {
                getAndSetDefault(preferences, property)
            }
        } else {
            getAndSetDefault(preferences, property)
        }

    /**
     * Fetches the default value of the preference from the given [Preferences] class
     *
     * @param T the type of the preference
     * @param preferences the [Preferences] instance that can give us the default value
     * @param property the actual field on the [Preferences] class
     * @return the default value of the property
     */
    private fun <T> getAndSetDefault(
        preferences: Preferences,
        property: KProperty<*>
    ): T {
        return getDefaultValue<T>(preferences, property)
            .also {
                Timber.i("Setting default preference value for ${property.name} to $it")
                setValueWithoutNotifying(preferences, property, it)
            }
    }

    /**
     * Sets the value of the preference into the [PreferencesStore]. Knows how to coerce each of the different supported
     * types into one of the types that the [PreferencesStore] supports.
     *
     * @param T type of the preference
     * @param property the actual field on the [Preferences] instance that's looking to set the value
     * @param value the value to be set
     */
    operator fun <T> setValue(preferences: Preferences, property: KProperty<*>, value: T) {
        setValueWithoutNotifying(preferences, property, value)
        setterTransaction?.apply { addProperty(property) } ?: run {
            preferences.notifyChanged(setOf(property))
        }
    }

    var setterTransaction: Transaction? = null

    class Transaction internal constructor(
        private val preferences: Preferences,
        private val preferencesStore: PreferencesStore
    ) : Closeable {
        init {
            preferencesStore.setterTransaction = this
        }

        fun addProperty(property: KProperty<*>) {
            propertiesToNotify.add(property)
        }

        private fun commit() {
            Timber.d("Committing preferences transaction for $propertiesToNotify")
            preferences.notifyChanged(propertiesToNotify)
        }

        private val propertiesToNotify = mutableSetOf<KProperty<*>>()
        override fun close() {
            try {
                commit()
            } finally {
                preferencesStore.setterTransaction = null
            }
        }
    }

    @Suppress("UNCHECKED_CAST")
    private fun <T> setValueWithoutNotifying(preferences: Preferences, property: KProperty<*>, value: T) {
        val coercedValue = getCoercion(property, value, preferences)
        Timber.d("Setting preference ${property.name} to $value (coerced to $coercedValue)")
        when (coercedValue) {
            is Boolean -> putBoolean(property.name, coercedValue)
            is String -> putString(property.name, coercedValue)
            is Int -> putInt(property.name, coercedValue)
            is Float -> putFloat(property.name, coercedValue)
            is Set<*> -> putStringSet(property.name, value as Set<String>)
            is ReverseGeocodeProvider -> putString(property.name, coercedValue.value)
            is MapLayerStyle -> putString(property.name, coercedValue.name)
            is ConnectionMode -> putInt(property.name, coercedValue.value)
            is MonitoringMode -> putInt(property.name, coercedValue.value)
            is MqttProtocolLevel -> putInt(property.name, coercedValue.value)
            is MqttQos -> putInt(property.name, coercedValue.value)
            is AppTheme -> putInt(property.name, coercedValue.value)
            is StringMaxTwoAlphaNumericChars -> putString(property.name, coercedValue.toString())
            else -> throw UnsupportedPreferenceTypeException(
                "Trying to set property ${property.name} has type ${property.returnType}"
            )
        }
    }

    class UnsupportedPreferenceTypeException(message: String) : Throwable(message)
}
