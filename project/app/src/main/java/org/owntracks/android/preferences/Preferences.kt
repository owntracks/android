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

    val something =listOf(Boolean::class).map { it to PreferencesStore.PreferenceStoreDelegate<it>(preferencesStore) }

    val parp = mapOf<Class, PreferencesStore.PreferenceStoreDelegate>(
        Boolean to PreferencesStore.PreferenceStoreDelegate<Boolean>(preferencesStore)
    )


    val boolDelegate = PreferencesStore.PreferenceStoreDelegate<Boolean>(preferencesStore)
    val stringDelegate = PreferencesStore.PreferenceStoreDelegate<String>(preferencesStore)
    val intDelegate = PreferencesStore.PreferenceStoreDelegate<Int>(preferencesStore)
    val floatDelegate = PreferencesStore.PreferenceStoreDelegate<Float>(preferencesStore)
    val stringSetDelegate = PreferencesStore.PreferenceStoreDelegate<Set<String>>(preferencesStore)
    val mapLayerStyleDelegate = PreferencesStore.PreferenceStoreDelegate<MapLayerStyle>(preferencesStore)
    val reverseGeocodeProviderDelegate = PreferencesStore.PreferenceStoreDelegate<ReverseGeocodeProvider>(preferencesStore)

    var autostartOnBoot: Boolean by boolDelegate
    var cleanSession: Boolean by boolDelegate
    var clientId: String by stringDelegate
    var connectionTimeoutSeconds: Int by intDelegate
    var enableMapRotation: Boolean by boolDelegate
    var encryptionKey: String by stringDelegate
    var experimentalFeatures: Set<String> by stringSetDelegate
    var host: String by stringDelegate
    var info: Boolean by boolDelegate
    var isSetupCompleted: Boolean by boolDelegate
    var keepalive: Int by intDelegate
    var locatorDisplacement: Int by intDelegate
    var locatorInterval: Int by intDelegate
    var mapLayerStyle: MapLayerStyle by mapLayerStyleDelegate
    var mode: ConnectionMode by preferencesStore
    var monitoring: MonitoringMode by preferencesStore
    var remoteConfiguration: Boolean by boolDelegate
    var debugLog: Boolean by boolDelegate
    var moveModeLocatorInterval: Int by intDelegate
    var mqttProtocolLevel: MqttProtocolLevel by preferencesStore
    var notificationGeocoderErrors: Boolean by boolDelegate
    var notificationHigherPriority: Boolean by boolDelegate
    var notificationLocation: Boolean by boolDelegate
    var openCageGeocoderApiKey: String by stringDelegate
    var password: String by stringDelegate
    var pegLocatorFastestIntervalToInterval: Boolean by boolDelegate
    var locatorPriority: Int by intDelegate
    var ping: Int by intDelegate
    var port: Int by intDelegate
    var pubQosEvents: MqttQos by preferencesStore
    var pubQosLocations: MqttQos by preferencesStore
    var pubQosWaypoints: MqttQos by preferencesStore
    var pubRetainEvents: Boolean by boolDelegate
    var pubRetainLocations: Boolean by boolDelegate
    var pubRetainWaypoints: Boolean by boolDelegate
    var pubTopicBase: String by stringDelegate
    var pubTopicCommands: String by stringDelegate
    var pubTopicCommandsPart: String by stringDelegate
    var pubTopicEvents: String by stringDelegate
    var notificationEvents: Boolean by boolDelegate
    var pubTopicEventsPart: String by stringDelegate
    var userDeclinedEnableLocationPermissions: Boolean by boolDelegate
    var pubTopicInfoPart: String by stringDelegate
    var userDeclinedEnableLocationServices: Boolean by boolDelegate
    var pubTopicLocations: String by stringDelegate
    var pubTopicWaypoints: String by stringDelegate
    var pubTopicWaypointsPart: String by stringDelegate
    var reverseGeocodeProvider: ReverseGeocodeProvider by reverseGeocodeProviderDelegate
    var showRegionsOnMap: Boolean by boolDelegate
    var sub: Boolean by boolDelegate
    var subTopic: String by stringDelegate
    var theme: NightMode by preferencesStore
    var tls: Boolean by boolDelegate
    var tlsCaCrt: String by stringDelegate
    var osmTileScaleFactor: Float by floatDelegate
    var tlsClientCrt: String by stringDelegate
    var tlsClientCrtPassword: String by stringDelegate
    var username: String by stringDelegate
    var ws: Boolean by boolDelegate

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
