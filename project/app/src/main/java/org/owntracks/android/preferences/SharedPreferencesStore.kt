package org.owntracks.android.preferences

import android.content.Context
import android.content.SharedPreferences
import android.util.Base64
import androidx.preference.PreferenceManager
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import timber.log.Timber

/***
 * Implements a PreferencesStore that uses a SharedPreferecnces as a backend.
 */
@Singleton
class SharedPreferencesStore @Inject constructor(@ApplicationContext private val context: Context) :
    PreferencesStore() {

    private val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)

    init {
        migrateToSingleSharedPreferences()
        migrateCertificatesToInline()
    }

    private fun migrateCertificatesToInline() {
        listOf(Preferences::tlsClientCrt.name).forEach { preferenceName ->
            sharedPreferences.getString(preferenceName, null)
                ?.run {
                    val crtFileExists = try {
                        context.getFileStreamPath(this)
                            .exists()
                    } catch (e: IllegalArgumentException) {
                        false
                    }
                    if (crtFileExists && this.isNotBlank()) {
                        context.openFileInput(this)
                            .use { fileInputStream ->
                                Timber.i("Migrating $preferenceName to inline base64")
                                sharedPreferences.edit()
                                    .putString(
                                        preferenceName,
                                        Base64.encodeToString(fileInputStream.readBytes(), Base64.NO_WRAP)
                                    )
                                    .commit()
                            }
                        context.deleteFile(this)
                    }
                }
        }
    }

    @Suppress("UNCHECKED_CAST")
    private fun migrateToSingleSharedPreferences() {
        val oldSharedPreferenceNames = listOf(
            "org.owntracks.android.preferences.private",
            "org.owntracks.android.preferences.http"
        )
        with(sharedPreferences.edit()) {
            if (sharedPreferences.contains("setupNotCompleted")) {
                val oldValue = sharedPreferences.getBoolean("setupNotCompleted", true)
                putBoolean("setupCompleted", !oldValue)
                remove("setupNotCompleted")
            }
            val nonEmptyLegacyPreferences =
                oldSharedPreferenceNames
                    .map { context.getSharedPreferences(it, Context.MODE_PRIVATE) }
                    .filter { it.all.isNotEmpty() }
                    .onEach {
                        it.all.forEach { (key, value) ->
                            Timber.d("Migrating legacy preference $key from $it")
                            when (value) {
                                is String -> putString(key, value)
                                is Set<*> -> putStringSet(key, value as Set<String>)
                                is Boolean -> putBoolean(key, value)
                                is Int -> putInt(key, value)
                                is Long -> putLong(key, value)
                                is Float -> putFloat(key, value)
                            }
                        }
                    }
                    .isNotEmpty()
            if (commit()) {
                if (nonEmptyLegacyPreferences) {
                    /* Running edit / clear / apply will actually create the preference file, which we don't want to do
                     if they didn't exist in the first place */
                    oldSharedPreferenceNames.forEach {
                        context.getSharedPreferences(it, Context.MODE_PRIVATE)
                            .edit()
                            .clear()
                            .apply()
                    }
                }
                oldSharedPreferenceNames.forEach {
                    val deleted = context.deleteSharedPreferences(it)
                    if (!deleted) {
                        Timber.e("Failed to delete shared preference $it")
                    } else {
                        Timber.i("Deleted legacy preference file $it")
                    }
                }
            }
        }
    }

    override fun putString(key: String, value: String) =
        sharedPreferences.edit()
            .putString(key, value)
            .apply()

    override fun getString(key: String, default: String): String? =
        sharedPreferences.getString(key, default)

    override fun remove(key: String) =
        sharedPreferences.edit()
            .remove(key)
            .apply()

    override fun getBoolean(key: String, default: Boolean): Boolean =
        sharedPreferences.getBoolean(key, default)

    override fun getSharedPreferencesName(): String = sharedPreferences.toString()

    override fun putBoolean(key: String, value: Boolean) =
        sharedPreferences.edit()
            .putBoolean(key, value)
            .apply()

    override fun getInt(key: String, default: Int): Int =
        sharedPreferences.getInt(key, default)

    override fun putFloat(key: String, value: Float) =
        sharedPreferences.edit()
            .putFloat(key, value)
            .apply()

    override fun getFloat(key: String, default: Float): Float =
        sharedPreferences.getFloat(key, default)

    override fun putInt(key: String, value: Int) =
        sharedPreferences.edit()
            .putInt(key, value)
            .apply()

    override fun putStringSet(key: String, values: Set<String>) {
        sharedPreferences.edit()
            .putStringSet(key, values)
            .apply()
    }

    override fun getStringSet(key: String, defaultValues: Set<String>): Set<String> =
        sharedPreferences.getStringSet(key, defaultValues)
            ?.toSortedSet() ?: defaultValues.toSortedSet()

    override fun hasKey(key: String): Boolean =
        sharedPreferences.contains(key)

    override fun registerOnSharedPreferenceChangeListener(
        listener: SharedPreferences.OnSharedPreferenceChangeListener
    ) {
        sharedPreferences.registerOnSharedPreferenceChangeListener(listener)
    }

    override fun unregisterOnSharedPreferenceChangeListener(
        listener: SharedPreferences.OnSharedPreferenceChangeListener
    ) {
        sharedPreferences.unregisterOnSharedPreferenceChangeListener(listener)
    }
}
