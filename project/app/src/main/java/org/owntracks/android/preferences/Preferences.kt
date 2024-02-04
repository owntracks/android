package org.owntracks.android.preferences

import android.os.Build
import android.os.Build.VERSION.SDK_INT
import androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_AUTO_BATTERY
import androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
import java.util.WeakHashMap
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton
import kotlin.reflect.KMutableProperty
import kotlin.reflect.KProperty
import kotlin.reflect.full.companionObject
import kotlin.reflect.full.companionObjectInstance
import kotlin.reflect.full.declaredMemberProperties
import kotlin.reflect.full.isSubtypeOf
import kotlin.reflect.jvm.jvmErasure
import kotlin.reflect.typeOf
import org.owntracks.android.BuildConfig
import org.owntracks.android.model.messages.MessageConfiguration
import org.owntracks.android.preferences.types.AppTheme
import org.owntracks.android.preferences.types.ConnectionMode
import org.owntracks.android.preferences.types.FromConfiguration
import org.owntracks.android.preferences.types.MonitoringMode
import org.owntracks.android.preferences.types.MqttProtocolLevel
import org.owntracks.android.preferences.types.MqttQos
import org.owntracks.android.preferences.types.ReverseGeocodeProvider
import org.owntracks.android.preferences.types.StringMaxTwoAlphaNumericChars
import org.owntracks.android.services.worker.Scheduler.Companion.MIN_PERIODIC_INTERVAL
import org.owntracks.android.support.SimpleIdlingResource
import org.owntracks.android.ui.map.MapLayerStyle
import timber.log.Timber

@Singleton
class Preferences @Inject constructor(
    private val preferencesStore: PreferencesStore,
    @Named("importConfigurationIdlingResource")
    private val importConfigurationIdlingResource: SimpleIdlingResource
) {
    val allConfigKeys =
        Preferences::class.declaredMemberProperties.filter { property ->
            property.annotations.any { annotation -> annotation is Preference }
        }

    private val mqttExportedConfigKeys =
        allConfigKeys.filter { property ->
            property.annotations.any { annotation -> annotation is Preference && annotation.exportModeMqtt }
        }
    private val httpExportedConfigKeys =
        allConfigKeys.filter { property ->
            property.annotations.any { annotation -> annotation is Preference && annotation.exportModeHttp }
        }

    private val placeholder = Any()
    private val listeners = WeakHashMap<OnPreferenceChangeListener, Any>()

    /*
    To initialize the defaults for each property, we can simply get the property. This should set
    the value in the underlying backing store to be the default, as only the backing store knows
    which properties have not already been set
     */
    private fun initializeDefaults() {
        Timber.d("Initializing defaults for unset preferences")
        allConfigKeys.forEach { it.get(this) }
    }

    /**
     * Imports a value for a key String. Untyped, so basically accepts anything for any key string
     *
     * @param key key to set preference for
     * @param value value to try and set
     */
    fun importKeyValue(key: String, value: Any) {
        importPreference(
            allConfigKeys.filterIsInstance<KMutableProperty<*>>()
                .first { it.name == key },
            value
        )
    }

    /**
     * We're going to loop through every existing preference, filtered by those that are actually set on the given
     * [MessageConfiguration]. For each of those, we're going to either just set the preference, or if it's an Enum,
     * convert from the value in the [MessageConfiguration] to the actual Enum type.
     *
     * @param configuration the [MessageConfiguration] to import
     */
    fun importConfiguration(configuration: MessageConfiguration) {
        PreferencesStore.Transaction(this, preferencesStore)
            .use {
                allConfigKeys.filterIsInstance<KMutableProperty<*>>()
                    .filter { configuration.containsKey(it.name) }
                    .forEach {
                        val configValue = configuration[it.name]
                        if (configValue == null) {
                            resetPreference(it.name)
                        } else {
                            Timber.d("Importing configuration key ${it.name} -> $configValue")
                            try {
                                // We need to convert the imported config value into an enum if the type of the preference is
                                // actually an enum
                                importPreference(it, configValue)
                            } catch (e: java.lang.IllegalArgumentException) {
                                Timber.w(
                                    "Trying to import wrong type of preference for ${it.name}. " +
                                        "Expected ${it.getter.returnType} but given ${configValue.javaClass}. Ignoring."
                                )
                            }
                        }
                    }
            }
        importConfigurationIdlingResource.setIdleState(true)
    }

    /**
     * Imports an untyped value to a known preference type
     *
     * @param it the preference to set
     * @param value untyped value to try and set
     */
    private fun importPreference(it: KMutableProperty<*>, value: Any) {
        if (it.returnType.isSubtypeOf(typeOf<Enum<*>>())) {
            // Find the companion object method annotated with FromConfiguration with a single parameter
            // that's the same type as the configuration value
            val conversionMethod = it.returnType.jvmErasure.companionObject?.members?.first { method ->
                method.annotations.any {
                    it is FromConfiguration
                } && method.parameters.size == 2 && method.parameters.any {
                    it.type.jvmErasure == value.javaClass.kotlin
                }
            }
            val enumValue = conversionMethod?.call(it.returnType.jvmErasure.companionObjectInstance, value)
            it.setter.call(this, enumValue)
        } else if (it.returnType.isSubtypeOf(typeOf<StringMaxTwoAlphaNumericChars>()) && value is String) {
            it.setter.call(this, StringMaxTwoAlphaNumericChars(value))
        } else if (value is String) {
            if (it.returnType.isSubtypeOf(typeOf<Set<*>>())) {
                it.setter.call(
                    this,
                    value.split(",")
                        .map { it.trim() }
                        .filter { it.isNotBlank() }
                        .toSortedSet()
                )
            } else if (it.returnType.isSubtypeOf(typeOf<Boolean>())) {
                it.setter.call(
                    this,
                    value.lowercase()
                        .toBoolean()
                )
            } else if (it.returnType.isSubtypeOf(typeOf<Float>())) {
                it.setter.call(this, value.toFloat())
            } else if (it.returnType.isSubtypeOf(typeOf<Int>())) {
                it.setter.call(this, value.toInt())
            } else if (it.returnType.isSubtypeOf(typeOf<Long>())) {
                it.setter.call(this, value.toLong())
            } else {
                it.setter.call(this, value)
            }
        } else {
            it.setter.call(this, value)
        }
    }

    fun getPreferenceByName(name: String): Any? {
        return allConfigKeys.first { it.name == name }
            .get(this)
    }

    private fun resetPreference(name: String) {
        preferencesStore.remove(name)
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

    @Preference(exportModeHttp = false)
    var keepalive: Int by preferencesStore

    @Preference
    var locatorDisplacement: Int by preferencesStore

    @Preference
    var locatorInterval: Int by preferencesStore

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
    var theme: AppTheme by preferencesStore

    @Preference(exportModeHttp = false)
    var tls: Boolean by preferencesStore

    @Preference(exportModeHttp = false)
    var tlsClientCrt: String by preferencesStore

    @Preference
    var tid: StringMaxTwoAlphaNumericChars by preferencesStore

    @Preference(exportModeMqtt = false)
    var url: String by preferencesStore

    @Preference(exportModeMqtt = false, exportModeHttp = false)
    var userDeclinedEnableLocationPermissions: Boolean by preferencesStore

    @Preference(exportModeMqtt = false, exportModeHttp = false)
    var userDeclinedEnableLocationServices: Boolean by preferencesStore

    @Preference(exportModeMqtt = false, exportModeHttp = false)
    var userDeclinedEnableNotificationPermissions: Boolean by preferencesStore

    @Preference
    var username: String by preferencesStore

    @Preference(exportModeHttp = false)
    var ws: Boolean by preferencesStore

    // Preferences we store but don't export / import
    var firstStart: Boolean by preferencesStore
    var setupCompleted: Boolean by preferencesStore

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
            return pubTopicBase.replace("%u", username.ifBlank { "user" })
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

    val minimumKeepaliveSeconds = MIN_PERIODIC_INTERVAL.inWholeSeconds
    fun keepAliveInRange(i: Int): Boolean =
        i >= if (EXPERIMENTAL_FEATURE_ALLOW_SMALL_KEEPALIVE in experimentalFeatures) 1 else minimumKeepaliveSeconds

    fun setMonitoringNext() {
        monitoring = monitoring.next()
    }

    // SharedPreferencesImpl stores its listeners as a list of WeakReferences. So we shouldn't use a
    // lambda as a listener, as that'll just get GC'd and then mysteriously disappear
    // https://stackoverflow.com/a/3104265/352740
    fun registerOnPreferenceChangedListener(listener: OnPreferenceChangeListener) {
        synchronized(listeners) {
            listeners[listener] = placeholder
        }
    }

    fun unregisterOnPreferenceChangedListener(listener: OnPreferenceChangeListener) {
        synchronized(listeners) {
            listeners.remove(listener)
        }
    }

    fun exportToMessage(): MessageConfiguration {
        return MessageConfiguration().apply { set("_build", BuildConfig.VERSION_CODE) }
            .apply {
                when (mode) {
                    ConnectionMode.MQTT -> mqttExportedConfigKeys
                    ConnectionMode.HTTP -> httpExportedConfigKeys
                }.forEach { property -> set(property.name, property.get(this@Preferences)) }
            }
    }

    fun notifyChanged(properties: Set<KProperty<*>>) {
        val propertyNames = properties.map { it.name }
            .toSet()
        synchronized(listeners) {
            listeners.toMap() // TODO migrate the notifications to async, so we can get rid of this clone
                .forEach {
                    it.key.onPreferenceChanged(propertyNames)
                }
        }
    }

    companion object {
        const val EXPERIMENTAL_FEATURE_SHOW_EXPERIMENTAL_PREFERENCE_UI = "showExperimentalPreferenceUI"
        const val EXPERIMENTAL_FEATURE_ALLOW_SMALL_KEEPALIVE = "allowSmallKeepalive"
        const val EXPERIMENTAL_FEATURE_LOCATION_PING_USES_HIGH_ACCURACY_LOCATION_REQUEST =
            "locationPingUsesHighAccuracyLocationRequest"

        internal val EXPERIMENTAL_FEATURES = setOf(
            EXPERIMENTAL_FEATURE_SHOW_EXPERIMENTAL_PREFERENCE_UI,
            EXPERIMENTAL_FEATURE_ALLOW_SMALL_KEEPALIVE,
            EXPERIMENTAL_FEATURE_LOCATION_PING_USES_HIGH_ACCURACY_LOCATION_REQUEST
        )

        val SYSTEM_NIGHT_AUTO_MODE by lazy {
            if (SDK_INT > Build.VERSION_CODES.Q) {
                MODE_NIGHT_FOLLOW_SYSTEM
            } else {
                MODE_NIGHT_AUTO_BATTERY
            }
        }
        // These preferences changing should trigger wiping the contacts and messagequeue
        val PREFERENCES_THAT_WIPE_QUEUE_AND_CONTACTS = setOf(
            Preferences::mode.name,
            Preferences::url.name,
            Preferences::port.name,
            Preferences::host.name,
            Preferences::username.name,
            Preferences::clientId.name,
            Preferences::tlsClientCrt.name
        )
    }

    @Target(AnnotationTarget.PROPERTY)
    @Retention(AnnotationRetention.RUNTIME)
    annotation class Preference(
        val exportModeMqtt: Boolean = true,
        val exportModeHttp: Boolean = true
    )

    interface OnPreferenceChangeListener {
        fun onPreferenceChanged(properties: Set<String>)
    }
}
