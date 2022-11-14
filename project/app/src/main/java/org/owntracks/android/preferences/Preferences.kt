package org.owntracks.android.preferences

import android.content.Context
import android.content.SharedPreferences
import android.os.Build
import android.os.Build.VERSION.SDK_INT
import androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_AUTO_BATTERY
import androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.reflect.full.declaredMemberProperties
import kotlin.time.Duration.Companion.milliseconds
import org.owntracks.android.model.messages.MessageConfiguration
import org.owntracks.android.preferences.types.ConnectionMode
import org.owntracks.android.preferences.types.MonitoringMode
import org.owntracks.android.preferences.types.MqttProtocolLevel
import org.owntracks.android.preferences.types.MqttQos
import org.owntracks.android.preferences.types.NightMode
import org.owntracks.android.preferences.types.ReverseGeocodeProvider
import org.owntracks.android.preferences.types.StringMaxTwoAlphaNumericChars
import org.owntracks.android.services.worker.Scheduler.MIN_PERIODIC_INTERVAL_MILLIS
import org.owntracks.android.ui.AppShortcuts
import org.owntracks.android.ui.map.MapLayerStyle

@Singleton
class Preferences @Inject constructor(
    @ApplicationContext private val applicationContext: Context,
    private val preferencesStore: PreferencesStore,
    private val appShortcuts: AppShortcuts
) : DefaultsProvider by DefaultsProviderImpl() {
    val allKeys =
        Preferences::class.declaredMemberProperties
            .filter { it.annotations.any { it is Preference } }

    init {
        initializeDefaults()
    }

    /*
    To initialize the defaults for each property, we can simply get the property. This should set
    the value in the underlying backing store to be the default, as only the backing store knows
    which properties have not already been set
     */
    fun initializeDefaults() {
        println(this.autostartOnBoot)
    }

    fun importKeyValue(key: String, value: Any) {
        TODO("Not implemented")
    }

    fun importConfiguration(configuration: MessageConfiguration) {
        TODO("Not implemented")
    }

    fun setPortDefault() {
        TODO("Not implemented")
    }

    fun setKeepaliveDefault() {
        TODO("Not implemented")
    }

    /* Persisted preferences */
    @Preference
    var autostartOnBoot: Boolean by preferencesStore

    @Preference(exportModeHttp = false)
    var cleanSession: Boolean by preferencesStore

    @Preference(exportModeHttp = false)
    var clientId: String by preferencesStore

    @Preference
    var connectionTimeoutSeconds: Int by preferencesStore

    @Preference
    var debugLog: Boolean by preferencesStore

    @Preference
    var deviceId: String by preferencesStore

    @Preference
    var dontReuseHttpClient: Boolean by preferencesStore

    @Preference
    var enableMapRotation: Boolean by preferencesStore

    @Preference
    var encryptionKey: String by preferencesStore

    @Preference
    var experimentalFeatures: Set<String> by preferencesStore

    @Preference
    var fusedRegionDetection: Boolean by preferencesStore

    @Preference
    var host: String by preferencesStore

    @Preference
    var ignoreInaccurateLocations: Int by preferencesStore

    @Preference
    var ignoreStaleLocations: Float by preferencesStore

    @Preference
    var info: Boolean by preferencesStore

    @Preference
    var isSetupCompleted: Boolean by preferencesStore

    @Preference
    var keepalive: Int by preferencesStore

    @Preference
    var locatorDisplacement: Int by preferencesStore

    @Preference
    var locatorInterval: Int by preferencesStore

    @Preference
    var locatorPriority: Int by preferencesStore

    @Preference
    var mapLayerStyle: MapLayerStyle by preferencesStore

    @Preference
    var mode: ConnectionMode by preferencesStore

    @Preference
    var monitoring: MonitoringMode by preferencesStore

    @Preference
    var moveModeLocatorInterval: Int by preferencesStore

    @Preference
    var mqttProtocolLevel: MqttProtocolLevel by preferencesStore

    @Preference
    var notificationEvents: Boolean by preferencesStore

    @Preference
    var notificationGeocoderErrors: Boolean by preferencesStore

    @Preference
    var notificationHigherPriority: Boolean by preferencesStore

    @Preference
    var notificationLocation: Boolean by preferencesStore

    @Preference
    var openCageGeocoderApiKey: String by preferencesStore

    @Preference
    var osmTileScaleFactor: Float by preferencesStore

    @Preference
    var password: String by preferencesStore

    @Preference
    var pegLocatorFastestIntervalToInterval: Boolean by preferencesStore

    @Preference
    var ping: Int by preferencesStore

    @Preference
    var port: Int by preferencesStore

    @Preference
    var pubLocationExtendedData: Boolean by preferencesStore

    @Preference
    var pubQosEvents: MqttQos by preferencesStore

    @Preference
    var pubQosLocations: MqttQos by preferencesStore

    @Preference
    var pubQosWaypoints: MqttQos by preferencesStore

    @Preference
    var pubRetain: Boolean by preferencesStore

    @Preference
    var pubTopicBaseFormatString: String by preferencesStore

    @Preference
    var publishLocationOnConnect: Boolean by preferencesStore

    @Preference
    var remoteCommand: Boolean by preferencesStore

    @Preference
    var remoteConfiguration: Boolean by preferencesStore

    @Preference
    var reverseGeocodeProvider: ReverseGeocodeProvider by preferencesStore

    @Preference
    var showRegionsOnMap: Boolean by preferencesStore

    @Preference
    var sub: Boolean by preferencesStore

    @Preference
    var subQos: MqttQos by preferencesStore

    @Preference
    var subTopic: String by preferencesStore

    @Preference
    var theme: NightMode by preferencesStore

    @Preference
    var tls: Boolean by preferencesStore

    @Preference
    var tlsCaCrt: String by preferencesStore

    @Preference
    var tlsClientCrt: String by preferencesStore

    @Preference
    var tlsClientCrtPassword: String by preferencesStore

    @Preference
    var trackerId: StringMaxTwoAlphaNumericChars by preferencesStore

    @Preference
    var url: String by preferencesStore

    @Preference
    var userDeclinedEnableLocationPermissions: Boolean by preferencesStore

    @Preference
    var userDeclinedEnableLocationServices: Boolean by preferencesStore

    @Preference
    var username: String by preferencesStore

    @Preference(exportModeHttp = false)
    var ws: Boolean by preferencesStore

    /* Derived / non-stored preferences */
    val pubRetainLocations: Boolean = pubRetain
    val pubRetainWaypoints: Boolean = false
    val pubRetainEvents: Boolean = false
    val pubTopicBase: String
        get() = pubTopicBaseFormatString.replace(
            "%u",
            username.ifBlank { "user" }
        ).replace("%d", deviceId)

    private val eventTopicSuffix = "/event"
    private val commandTopicSuffix = "/cmd"
    private val infoTopicSuffix = "/info"
    private val waypointsTopicSuffix = "/waypoints"

    val receivedCommandsTopic: String = pubTopicBase + commandTopicSuffix

    val pubTopicEvents: String = pubTopicBase + eventTopicSuffix
    val pubTopicLocations: String = pubTopicBase
    val pubTopicWaypoints: String = pubTopicBase + waypointsTopicSuffix

    val subTopicEvents: String = subTopic + eventTopicSuffix
    val subTopicInfo: String = subTopic + infoTopicSuffix
    val subTopicWaypoints: String = subTopic + waypointsTopicSuffix

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
        listener: SharedPreferences.OnSharedPreferenceChangeListener
    ) {
        preferencesStore.registerOnSharedPreferenceChangeListener(listener)
    }

    fun unregisterOnPreferenceChangedListener(
        listener: SharedPreferences.OnSharedPreferenceChangeListener
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

    @Target(AnnotationTarget.PROPERTY)
    @Retention(AnnotationRetention.RUNTIME)
    annotation class Preference(
        val exportModeMqtt: Boolean = true,
        val exportModeHttp: Boolean = true
    )
}
