package org.owntracks.android.preferences

import android.content.Context
import android.content.SharedPreferences
import android.os.Build
import android.os.Build.VERSION.SDK_INT
import androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_AUTO_BATTERY
import androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
import androidx.preference.PreferenceManagerFix
import com.fasterxml.jackson.annotation.JsonValue
import dagger.hilt.android.qualifiers.ApplicationContext
import org.owntracks.android.R
import org.owntracks.android.model.messages.MessageConfiguration
import org.owntracks.android.services.worker.Scheduler.MIN_PERIODIC_INTERVAL_MILLIS
import org.owntracks.android.ui.AppShortcuts
import org.owntracks.android.ui.map.MapLayerStyle
import java.io.InputStream
import java.io.OutputStream
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.time.Duration.Companion.milliseconds

@Singleton
class Preferences @Inject constructor(
    @ApplicationContext private val applicationContext: Context,
    private val preferencesStore: PreferencesStore,
    private val appShortcuts: AppShortcuts,
) {
    init {
        // Need to set the preferences to their default values
        // From https://stackoverflow.com/a/2877795/352740
        listOf(
            R.xml.preferences_root,
            R.xml.preferences_advanced,
            R.xml.preferences_notification,
            R.xml.preferences_reporting,
        ).forEach {
            PreferenceManagerFix.setDefaultValues(applicationContext, it, true)
        }
    }

    var autostartOnBoot: Boolean by preferencesStore
    var cleanSession: Boolean by preferencesStore
    var clientId: String by preferencesStore
    var connectionTimeoutSeconds: Int by preferencesStore
    var enableMapRotation: Boolean by preferencesStore
    var encryptionKey: String by preferencesStore
    var experimentalFeatures: Set<String> by preferencesStore
    var host: String by preferencesStore
    var info: Boolean by preferencesStore
    var isSetupCompleted: Boolean by preferencesStore
    var keepalive: Int by preferencesStore
    var locatorDisplacement: Int by preferencesStore
    var locatorInterval: Int by preferencesStore
    var mapLayerStyle: MapLayerStyle by preferencesStore
    var mode: ConnectionMode by preferencesStore
    var monitoring: MonitoringMode by preferencesStore
    var remoteConfiguration: Boolean by preferencesStore
    var debugLog: Boolean by preferencesStore
    var moveModeLocatorInterval: Int by preferencesStore
    var mqttProtocolLevel: MqttProtocolLevel by preferencesStore
    var notificationGeocoderErrors: Boolean by preferencesStore
    var notificationHigherPriority: Boolean by preferencesStore
    var notificationLocation: Boolean by preferencesStore
    var openCageGeocoderApiKey: String by preferencesStore
    var password: String by preferencesStore
    var pegLocatorFastestIntervalToInterval: Boolean by preferencesStore
    var locatorPriority: Int by preferencesStore
    var ping: Int by preferencesStore
    var port: Int by preferencesStore
    var pubQosEvents: MqttQos by preferencesStore
    var pubQosLocations: MqttQos by preferencesStore
    var pubQosWaypoints: MqttQos by preferencesStore
    var pubRetainEvents: Boolean by preferencesStore
    var pubRetainLocations: Boolean by preferencesStore
    var pubRetainWaypoints: Boolean by preferencesStore
    var pubTopicBase: String by preferencesStore
    var pubTopicCommands: String by preferencesStore
    var pubTopicCommandsPart: String by preferencesStore
    var pubTopicEvents: String by preferencesStore
    var notificationEvents: Boolean by preferencesStore
    var pubTopicEventsPart: String by preferencesStore
    var userDeclinedEnableLocationPermissions: Boolean by preferencesStore
    var pubTopicInfoPart: String by preferencesStore
    var userDeclinedEnableLocationServices: Boolean by preferencesStore
    var pubTopicLocations: String by preferencesStore
    var pubTopicWaypoints: String by preferencesStore
    var pubTopicWaypointsPart: String by preferencesStore
    var reverseGeocodeProvider: ReverseGeocodeProvider by preferencesStore
    var showRegionsOnMap: Boolean by preferencesStore
    var sub: Boolean by preferencesStore
    var subTopic: String by preferencesStore
    var theme: NightMode by preferencesStore
    var tls: Boolean by preferencesStore
    var tlsCaCrt: String by preferencesStore
    var osmTileScaleFactor: Float by preferencesStore
    var tlsClientCrt: String by preferencesStore
    var tlsClientCrtPassword: String by preferencesStore
    var username: String by preferencesStore
    var ws: Boolean by preferencesStore

    val minimumKeepaliveSeconds = MIN_PERIODIC_INTERVAL_MILLIS.milliseconds.inWholeSeconds
    fun keepAliveInRange(i: Int): Boolean = i >= minimumKeepaliveSeconds

    val sharedPreferencesName: String
        get() = preferencesStore.getSharedPreferencesName()

    fun setMonitoringNext() {
    }

    fun checkFirstStart() {
        TODO("Not implemented. Can we put this in the constructor?")
    }

    // SharedPreferencesImpl stores its listeners as a list of WeakReferences. So we shouldn't use a
    // lambda as a listener, as that'll just get GC'd and then mysteriously disappear
    // https://stackoverflow.com/a/3104265/352740
    fun registerOnPreferenceChangedListener(
        listener: SharedPreferences.OnSharedPreferenceChangeListener,
    ) {
        preferencesStore.registerOnSharedPreferenceChangeListener(listener)
    }

    fun unregisterOnPreferenceChangedListener(
        listener: SharedPreferences.OnSharedPreferenceChangeListener,
    ) {
        preferencesStore.unregisterOnSharedPreferenceChangeListener(listener)
    }

    fun getPreferenceKey(s: Int): String {
        TODO("Not implemented")
    }

    fun exportToMessage(): MessageConfiguration {
        val cfg = MessageConfiguration()
//        cfg[getPreferenceKey(R.string.preferenceKeyVersion)] = BuildConfig.VERSION_CODE //TODO work out what's going on here
//        exportMethods.forEach {
//            cfg[it.key] = it.value.invoke(this)
//        }
        return cfg
    }

    companion object {
        const val EXPERIMENTAL_FEATURE_SHOW_EXPERIMENTAL_PREFERENCE_UI =
            "showExperimentalPreferenceUI"
        const val EXPERIMENTAL_FEATURE_ALLOW_SMALL_KEEPALIVE = "allowSmallKeepalive"
        const val EXPERIMENTAL_FEATURE_BEARING_ARROW_FOLLOWS_DEVICE_ORIENTATION =
            "bearingArrowFollowsDeviceOrientation"
        const val EXPERIMENTAL_FEATURE_ENABLE_APP_SHORTCUTS =
            "enableAppShortcuts"

        internal val EXPERIMENTAL_FEATURES = setOf(
            EXPERIMENTAL_FEATURE_SHOW_EXPERIMENTAL_PREFERENCE_UI,
            EXPERIMENTAL_FEATURE_ALLOW_SMALL_KEEPALIVE,
            EXPERIMENTAL_FEATURE_BEARING_ARROW_FOLLOWS_DEVICE_ORIENTATION,
            EXPERIMENTAL_FEATURE_ENABLE_APP_SHORTCUTS
        )

        val SYSTEM_NIGHT_AUTO_MODE by lazy {
            if (SDK_INT > Build.VERSION_CODES.Q) MODE_NIGHT_FOLLOW_SYSTEM
            else MODE_NIGHT_AUTO_BATTERY
        }

        // Preference Keys
        const val preferenceKeyUserDeclinedEnableLocationPermissions =
            "userDeclinedEnableLocationPermissions"
        const val preferenceKeyUserDeclinedEnableLocationServices =
            "userDeclinedEnableLocationServices"
    }

    enum class MqttProtocolLevel(val value: Int) {
        MQTT_3_1(3),
        MQTT_3_1_1(4);

        @JsonValue
        fun getVal(): Int {
            return value
        }
    }

    enum class MqttQos(val value: Int) {
        ZERO(0),
        ONE(1),
        TWO(2);

        @JsonValue
        fun getVal(): Int {
            return value
        }
    }
}
