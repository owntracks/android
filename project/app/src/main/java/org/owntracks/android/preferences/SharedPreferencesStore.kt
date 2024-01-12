package org.owntracks.android.preferences

import android.Manifest
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.Intent.FLAG_ACTIVITY_NEW_TASK
import android.content.SharedPreferences
import android.content.pm.PackageManager
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.edit
import androidx.preference.PreferenceManager
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import org.owntracks.android.R
import org.owntracks.android.geocoding.GeocoderProvider
import org.owntracks.android.ui.NotificationsStash
import org.owntracks.android.ui.preferences.ConnectionFragment
import org.owntracks.android.ui.preferences.PreferencesActivity
import org.owntracks.android.ui.preferences.PreferencesActivity.Companion.START_FRAGMENT_KEY
import timber.log.Timber

/***
 * Implements a PreferencesStore that uses a SharedPreferecnces as a backend.
 */
@Singleton
class SharedPreferencesStore @Inject constructor(
    @ApplicationContext private val context: Context,
    private val notificationManager: NotificationManagerCompat,
    private val notificationStash: NotificationsStash
) :
    PreferencesStore() {

    private val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)

    init {
        migrate()
    }

    override fun migrate() {
        migrateToSingleSharedPreferences()
        detectIfCertsInConfig()
    }

    private fun detectIfCertsInConfig() {
        val legacyTlsCrtKeys = setOf("tlsCaCrt", "tlsClientCrtPassword")
        val shouldNotify = legacyTlsCrtKeys.any {
            sharedPreferences.contains(it) && !sharedPreferences.getString(it, "").isNullOrEmpty()
        }

        if (shouldNotify) {
            // Delete local files
            setOf("tlsClientCrt", "tlsCaCrt").forEach {
                sharedPreferences.getString(it, "")
                    .also{Timber.i("Deleting legacy cert from app storage: $it")}
                    .run (context::deleteFile)
            }
            // Notify user
            NotificationCompat.Builder(
                context,
                GeocoderProvider.ERROR_NOTIFICATION_CHANNEL_ID
            )
                .setContentTitle(context.getString(R.string.certificateMigrationRequiredNotificationTitle))
                .setAutoCancel(true)
                .setSmallIcon(R.drawable.ic_owntracks_80)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .addAction(
                    R.drawable.ic_owntracks_80,
                    context.getString(R.string.certificateMigrationRequiredOpenSettingsAction),
                    PendingIntent.getActivity(
                        context,
                        0,
                        Intent(context,PreferencesActivity::class.java)
                            .addFlags(FLAG_ACTIVITY_NEW_TASK)
                            .putExtra(START_FRAGMENT_KEY, ConnectionFragment::class.java.name),

                        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
                    )
                )
                .setStyle(
                    NotificationCompat.BigTextStyle()
                        .bigText(context.getString(R.string.certificateMigrationRequiredNotificationText))
                )
                .setSilent(true)
                .build()
                .run {
                    if (ActivityCompat.checkSelfPermission(
                            context,
                            Manifest.permission.POST_NOTIFICATIONS
                        ) == PackageManager.PERMISSION_GRANTED
                    ) {
                        notificationManager.notify("CertificateManagementNotification", 0, this)
                    } else {
                        notificationStash.add(this)
                    }
                }
        }
        // Remove old preferences from sharedPreferences
        sharedPreferences.edit {
            legacyTlsCrtKeys.forEach(this::remove)
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
