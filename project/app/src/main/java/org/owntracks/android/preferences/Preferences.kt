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
import kotlin.reflect.KMutableProperty
import kotlin.reflect.full.companionObject
import kotlin.reflect.full.companionObjectInstance
import kotlin.reflect.full.declaredMemberProperties
import kotlin.reflect.full.isSubtypeOf
import kotlin.reflect.jvm.jvmErasure
import kotlin.reflect.typeOf
import kotlin.time.Duration.Companion.milliseconds
import org.owntracks.android.BuildConfig
import org.owntracks.android.model.messages.MessageConfiguration
import org.owntracks.android.preferences.types.ConnectionMode
import org.owntracks.android.preferences.types.FromConfiguration
import org.owntracks.android.preferences.types.MonitoringMode
import org.owntracks.android.preferences.types.MqttProtocolLevel
import org.owntracks.android.preferences.types.MqttQos
import org.owntracks.android.preferences.types.NightMode
import org.owntracks.android.preferences.types.ReverseGeocodeProvider
import org.owntracks.android.preferences.types.StringMaxTwoAlphaNumericChars
import org.owntracks.android.services.worker.Scheduler.MIN_PERIODIC_INTERVAL_MILLIS
import org.owntracks.android.ui.AppShortcuts
import org.owntracks.android.ui.map.MapLayerStyle
import timber.log.Timber

@Singleton
class Preferences @Inject constructor(
    @ApplicationContext private val applicationContext: Context,
    private val preferencesStore: PreferencesStore,
    private val appShortcuts: AppShortcuts
) {
    val allConfigKeys =
        Preferences::class.declaredMemberProperties
            .filter { it.annotations.any { it is Preference } }

    val mqttExportedConfigKeys = allConfigKeys.filter { it.annotations.any { it is Preference && it.exportModeMqtt } }
    val httpExportedConfigKeys = allConfigKeys.filter { it.annotations.any { it is Preference && it.exportModeHttp } }

    /*
    To initialize the defaults for each property, we can simply get the property. This should set
    the value in the underlying backing store to be the default, as only the backing store knows
    which properties have not already been set
     */
    fun initializeDefaults() {
        Timber.d("Initializing defaults for unset preferences")
        allConfigKeys.forEach { it.get(this) }
    }

    fun importKeyValue(key: String, value: Any) {
        TODO("Not implemented")
    }

    fun importConfiguration(configuration: MessageConfiguration) {
        allConfigKeys.filterIsInstance<KMutableProperty<*>>()
            .filter { configuration.containsKey(it.name) }
            .forEach {
                val configValue = configuration.get(it.name)
                if (configValue == null) {
                    preferencesStore.remove(it.name)
                } else {
                    try {
                        // We need to convert the imported config value into an enum if the type of the preference is
                        // actually an enum
                        if (it.returnType.isSubtypeOf(typeOf<Enum<*>>())) {
                            // Find the companion object method annotated with FromConfiguration with a single parameter
                            // that's the same type as the configuration value
                            val conversionMethod = it.returnType.jvmErasure
                                .companionObject
                                ?.members
                                ?.first {
                                    it.annotations.any { it is FromConfiguration } &&
                                        it.parameters.size == 2 &&
                                        it.parameters.any {
                                            it.type.jvmErasure == configValue.javaClass.kotlin
                                        }
                                }
                            val enumValue =
                                conversionMethod?.call(it.returnType.jvmErasure.companionObjectInstance, configValue)
                            it.setter.call(this, enumValue)
                        } else if (
                            it.returnType.isSubtypeOf(typeOf<StringMaxTwoAlphaNumericChars>()) && configValue is String
                        ) {
                            it.setter.call(this, StringMaxTwoAlphaNumericChars(configValue))
                        } else {
                            it.setter.call(this, configValue)
                        }
                    } catch (e: java.lang.IllegalArgumentException) {
                        Timber.w(
                            "Trying to import wrong type of preference for ${it.name}. " +
                                "Expected ${it.getter.returnType} but given ${configValue.javaClass}. Ignoring."
                        )
                    }
                }
            }
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

    @Preference(exportModeMqtt = false)
    var dontReuseHttpClient: Boolean by preferencesStore

    @Preference
    var enableMapRotation: Boolean by preferencesStore

    @Preference
    var encryptionKey: String by preferencesStore

    @Preference
    var experimentalFeatures: Set<String> by preferencesStore

    @Preference
    var fusedRegionDetection: Boolean by preferencesStore

    @Preference(exportModeHttp = false)
    var host: String by preferencesStore

    @Preference
    var ignoreInaccurateLocations: Int by preferencesStore

    @Preference
    var ignoreStaleLocations: Float by preferencesStore

    @Preference(exportModeHttp = false)
    var info: Boolean by preferencesStore

    @Preference
    var isSetupCompleted: Boolean by preferencesStore

    @Preference(exportModeHttp = false)
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

    @Preference(exportModeHttp = false)
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
    var opencageApiKey: String by preferencesStore

    @Preference
    var osmTileScaleFactor: Float by preferencesStore

    @Preference
    var password: String by preferencesStore

    @Preference
    var pegLocatorFastestIntervalToInterval: Boolean by preferencesStore

    @Preference
    var ping: Int by preferencesStore

    @Preference(exportModeHttp = false)
    var port: Int by preferencesStore

    @Preference
    var pubExtendedData: Boolean by preferencesStore

    @Preference(exportModeHttp = false)
    var pubQos: MqttQos by preferencesStore

    @Preference(exportModeHttp = false)
    var pubRetain: Boolean by preferencesStore

    @Preference(exportModeHttp = false)
    var pubTopicBase: String by preferencesStore

    @Preference
    var publishLocationOnConnect: Boolean by preferencesStore

    @Preference
    var cmd: Boolean by preferencesStore

    @Preference
    var remoteConfiguration: Boolean by preferencesStore

    @Preference
    var reverseGeocodeProvider: ReverseGeocodeProvider by preferencesStore

    @Preference
    var showRegionsOnMap: Boolean by preferencesStore

    @Preference(exportModeHttp = false)
    var sub: Boolean by preferencesStore

    @Preference(exportModeHttp = false)
    var subQos: MqttQos by preferencesStore

    @Preference(exportModeHttp = false)
    var subTopic: String by preferencesStore

    @Preference
    var theme: NightMode by preferencesStore

    @Preference(exportModeHttp = false)
    var tls: Boolean by preferencesStore

    @Preference(exportModeHttp = false)
    var tlsCaCrt: String by preferencesStore

    @Preference(exportModeHttp = false)
    var tlsClientCrt: String by preferencesStore

    @Preference(exportModeHttp = false)
    var tlsClientCrtPassword: String by preferencesStore

    @Preference
    var tid: StringMaxTwoAlphaNumericChars by preferencesStore

    @Preference(exportModeMqtt = false)
    var url: String by preferencesStore

    @Preference
    var userDeclinedEnableLocationPermissions: Boolean by preferencesStore

    @Preference
    var userDeclinedEnableLocationServices: Boolean by preferencesStore

    @Preference
    var username: String by preferencesStore

    @Preference(exportModeHttp = false)
    var ws: Boolean by preferencesStore

    // Needs to be after all the preferences are declared, otherwise the delegates are null.
    init {
        initializeDefaults()
    }

    /* Derived / non-stored preferences */
    val pubQosEvents: MqttQos
        get() {
            return pubQos
        }
    val pubQosLocations: MqttQos
        get() {
            return pubQos
        }
    val pubQosWaypoints = MqttQos.ZERO

    val pubRetainLocations: Boolean
        get() {
            return pubRetain
        }
    val pubRetainWaypoints: Boolean = false
    val pubRetainEvents: Boolean = false
    val pubTopicBaseWithUserDetails: String
        get() {
            return pubTopicBase.replace(
                "%u",
                username.ifBlank { "user" }
            )
                .replace("%d", deviceId)
        }

    val eventTopicSuffix = "/event"
    val commandTopicSuffix = "/cmd"
    val infoTopicSuffix = "/info"
    val waypointsTopicSuffix = "/waypoints"

    val receivedCommandsTopic: String
        get() {
            return pubTopicBaseWithUserDetails + commandTopicSuffix
        }

    val pubTopicEvents: String
        get() {
            return pubTopicBaseWithUserDetails + eventTopicSuffix
        }
    val pubTopicLocations: String
        get() {
            return pubTopicBaseWithUserDetails
        }
    val pubTopicWaypoints: String
        get() {
            return pubTopicBaseWithUserDetails + waypointsTopicSuffix
        }

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
        return MessageConfiguration()
            .apply { set("_build", BuildConfig.VERSION_CODE) }
            .apply {
                when (mode) {
                    ConnectionMode.MQTT -> mqttExportedConfigKeys
                    ConnectionMode.HTTP -> httpExportedConfigKeys
                }.forEach { set(it.name, it.get(this@Preferences)) }
            }
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
