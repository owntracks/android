package org.owntracks.android.support

import android.annotation.SuppressLint
import android.content.Context
import android.content.SharedPreferences
import android.os.Build
import android.os.Build.VERSION.SDK_INT
import androidx.appcompat.app.AppCompatDelegate
import androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_AUTO_BATTERY
import androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
import dagger.hilt.android.qualifiers.ApplicationContext
import java.lang.reflect.InvocationTargetException
import java.lang.reflect.Method
import java.lang.reflect.ParameterizedType
import java.lang.reflect.Type
import java.util.*
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton
import org.eclipse.paho.client.mqttv3.MqttConnectOptions
import org.greenrobot.eventbus.EventBus
import org.owntracks.android.BuildConfig
import org.owntracks.android.R
import org.owntracks.android.model.messages.MessageConfiguration
import org.owntracks.android.services.MessageProcessorEndpointHttp
import org.owntracks.android.services.MessageProcessorEndpointMqtt
import org.owntracks.android.services.worker.Scheduler
import org.owntracks.android.support.Events.ModeChanged
import org.owntracks.android.support.Events.MonitoringChanged
import org.owntracks.android.support.preferences.PreferencesStore
import org.owntracks.android.ui.AppShortcuts
import org.owntracks.android.ui.map.MapLayerStyle
import timber.log.Timber

@Singleton
@SuppressLint("NonConstantResourceId")
class Preferences @Inject constructor(
    @ApplicationContext private val applicationContext: Context,
    private val eventBus: EventBus?,
    private val preferencesStore: PreferencesStore,
    private val appShortcuts: AppShortcuts
) {
    private var isFirstStart = false
    private var currentMode = MessageProcessorEndpointMqtt.MODE_ID

    val sharedPreferencesName: String
        get() = preferencesStore.getSharedPreferencesName()

    // need to iterated thought hierarchy in order to retrieve methods from above the current instance
    // iterate though the list of methods declared in the class represented by klass variable, and insert those annotated with the specified annotation
    private val exportMethods: Map<String, Method>
        get() = Preferences::class.java
            .parentClasses()
            .flatMap { it.declaredMethods.asSequence() }
            .filter { it.isAnnotationPresent(Export::class.java) }
            .filter {
                val annotation = it.getAnnotation(Export::class.java)
                annotation != null &&
                    (
                        currentMode == MessageProcessorEndpointMqtt.MODE_ID && annotation.exportModeMqtt ||
                            currentMode == MessageProcessorEndpointHttp.MODE_ID && annotation.exportModeHttp
                        )
            }
            .map { Pair(getPreferenceKey(it.getAnnotation(Export::class.java)!!.keyResId), it) }
            .toMap()

    val importKeys: List<String>
        get() = ArrayList(importMethods.keys)

    // need to iterated thought hierarchy in order to retrieve methods from above the current instance
    // iterate though the list of methods declared in the class represented by klass variable, and insert those annotated with the specified annotation
    private val importMethods: Map<String, Method>
        get() = Preferences::class.java
            .parentClasses()
            .flatMap { it.declaredMethods.asSequence() }
            .filter { it.isAnnotationPresent(Import::class.java) }
            .map { Pair(getPreferenceKey(it.getAnnotation(Import::class.java)!!.keyResId), it) }
            .toMap()

    private fun Class<*>.parentClasses(): Sequence<Class<*>> {
        var k = this
        return sequence {
            yield(k)
            while (k.superclass != null) {
                k = k.superclass!!
                yield(k)
            }
        }
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

    fun checkFirstStart() {
        if (preferencesStore.getBoolean(getPreferenceKey(R.string.preferenceKeyFirstStart), true)) {
            Timber.v("Initial application launch")
            isFirstStart = true
            preferencesStore.putBoolean(getPreferenceKey(R.string.preferenceKeyFirstStart), false)
            preferencesStore.putBoolean(
                getPreferenceKey(R.string.preferenceKeySetupNotCompleted),
                true
            )
        }
    }

    @Throws(IllegalAccessException::class, IllegalArgumentException::class)
    fun importKeyValue(key: String?, value: String?) {
        Timber.v("setting %s, for key %s", value, key)
        val methods = importMethods
        val m = methods[key] ?: throw IllegalAccessException()
        if (value == null) {
            clearKey(key)
            return
        }
        try {
            val t = m.genericParameterTypes[0]
            Timber.v("type of parameter: %s %s", t, t.javaClass)
            methods[key]!!.invoke(this, convert(t, value))
        } catch (e: InvocationTargetException) {
            throw IllegalAccessException()
        }
    }

    @Throws(IllegalArgumentException::class)
    private fun convert(t: Type, value: String): Any {
        if (java.lang.Boolean.TYPE == t) {
            require(!("true" != value && "false" != value))
            return java.lang.Boolean.parseBoolean(value)
        }
        try {
            if (Integer.TYPE == t) return value.toInt()
        } catch (e: NumberFormatException) {
            throw IllegalArgumentException()
        }

        try {
            if (java.lang.Float.TYPE == t) return value.toFloat()
        } catch (e: NumberFormatException) {
            throw IllegalArgumentException()
        }

        if (t is ParameterizedType &&
            Collection::class.java.isAssignableFrom(t.rawType as Class<*>)
        ) {
            return value.split(",").map { it.trim() }.filter { it.isNotBlank() }.toSortedSet()
        }
        return value
    }

    fun exportToMessage(): MessageConfiguration {
        val cfg = MessageConfiguration()
        cfg[getPreferenceKey(R.string.preferenceKeyVersion)] = BuildConfig.VERSION_CODE
        exportMethods.forEach {
            cfg[it.key] = it.value.invoke(this)
        }
        return cfg
    }

    @SuppressLint("CommitPrefEdits", "ApplySharedPref")
    fun importFromMessage(messageConfiguration: MessageConfiguration) {
        Timber.v("importing %s keys ", messageConfiguration.keys.size)

        if (messageConfiguration.containsKey(getPreferenceKey(R.string.preferenceKeyModeId))) {
            Timber.v(
                "setting mode to %s",
                messageConfiguration[getPreferenceKey(R.string.preferenceKeyModeId)]
            )
            mode = messageConfiguration[getPreferenceKey(R.string.preferenceKeyModeId)] as Int
            messageConfiguration.removeKey(getPreferenceKey(R.string.preferenceKeyModeId))
        }

        // Don't show setup if a config has been imported
        setSetupCompleted()

        messageConfiguration.keys
            .filter { messageConfiguration[it] == null }
            .forEach {
                Timber.d("clearing value for key %s", it)
                clearKey(it)
            }
        val methods = importMethods
        messageConfiguration.keys
            .filter { messageConfiguration[it] != null }
            .filter { methods.containsKey(it) }
            .forEach { configurationKey ->
                Timber.d(
                    "Loading key $configurationKey from method: ${methods.getValue(configurationKey).name}"
                )
                try {
                    methods.getValue(configurationKey).let { importMethod ->
                        if (messageConfiguration[configurationKey]?.javaClass?.isAssignableFrom(
                                Integer::class.java
                            ) ?: false &&
                            importMethod.parameterTypes.first() == MonitoringMode::class.java
                        ) {
                            importMethod.invoke(
                                this,
                                MonitoringMode.getByValue(
                                    messageConfiguration[configurationKey] as Int
                                )
                            )
                        } else {
                            importMethod.invoke(this, messageConfiguration[configurationKey])
                        }
                    }
                } catch (e: IllegalArgumentException) {
                    Timber.e(
                        "Tried to import $configurationKey but value is wrong type. Expected: ${
                            methods.getValue(configurationKey).parameterTypes.first().canonicalName
                        }, given ${messageConfiguration[configurationKey]?.javaClass?.canonicalName}"
                    )
                }
            }
    }

    private fun setMode(requestedMode: Int, init: Boolean) {
        Timber.v("setMode: %s", requestedMode)
        if (!(requestedMode == MessageProcessorEndpointMqtt.MODE_ID || requestedMode == MessageProcessorEndpointHttp.MODE_ID)) {
            Timber.v("Invalid mode requested: %s", requestedMode)
            return
        }
        if (!init && currentMode == requestedMode) {
            Timber.v("mode is already set to requested mode")
            return
        }
        Timber.v("setting mode to: %s", requestedMode)
        preferencesStore.setMode(getPreferenceKey(R.string.preferenceKeyModeId), requestedMode)
        currentMode = requestedMode
        if (!init && eventBus != null) {
            Timber.v("broadcasting mode change event")
            eventBus.post(ModeChanged(currentMode))
        }
    }

    fun setMonitoringNext() {
        monitoring = monitoring.next()
    }

    @get:Export(
        keyResId = R.string.preferenceKeyModeId,
        exportModeMqtt = true,
        exportModeHttp = true
    )
    @set:Import(keyResId = R.string.preferenceKeyModeId)
    var mode: Int
        get() = currentMode
        set(active) {
            setMode(active, false)
        }

    @get:Export(
        keyResId = R.string.preferenceKeyMonitoring,
        exportModeMqtt = true,
        exportModeHttp = true
    )
    @set:Import(keyResId = R.string.preferenceKeyMonitoring)
    var monitoring: MonitoringMode
        get() = MonitoringMode.getByValue(
            getIntOrDefault(
                R.string.preferenceKeyMonitoring,
                R.integer.valMonitoring
            )
        )
        set(newMode) {
            if (newMode != this.monitoring) {
                setInt(R.string.preferenceKeyMonitoring, newMode.mode)
                eventBus?.post(MonitoringChanged())
            }
        }

    @get:Export(
        keyResId = R.string.preferenceKeyDontReuseHttpClient,
        exportModeMqtt = false,
        exportModeHttp = true
    )
    @set:Import(keyResId = R.string.preferenceKeyDontReuseHttpClient)
    var dontReuseHttpClient: Boolean
        get() = getBooleanOrDefault(R.string.preferenceKeyDontReuseHttpClient, R.bool.valFalse)
        set(newValue) {
            setBoolean(R.string.preferenceKeyDontReuseHttpClient, newValue)
        }

    @get:Export(
        keyResId = R.string.preferenceKeyOpencageGeocoderApiKey,
        exportModeMqtt = true,
        exportModeHttp = true
    )
    @set:Import(keyResId = R.string.preferenceKeyOpencageGeocoderApiKey)
    var openCageGeocoderApiKey: String
        get() = getStringOrDefault(R.string.preferenceKeyOpencageGeocoderApiKey, R.string.valEmpty)
        set(key) {
            setString(R.string.preferenceKeyOpencageGeocoderApiKey, key.trim())
        }

    @get:Export(
        keyResId = R.string.preferenceKeyRemoteCommand,
        exportModeMqtt = true,
        exportModeHttp = true
    )
    @set:Import(keyResId = R.string.preferenceKeyRemoteCommand)
    var remoteCommand: Boolean
        get() = getBooleanOrDefault(R.string.preferenceKeyRemoteCommand, R.bool.valRemoteCommand)
        set(newValue) {
            setBoolean(R.string.preferenceKeyRemoteCommand, newValue)
        }

    @get:Export(
        keyResId = R.string.preferenceKeyCleanSession,
        exportModeMqtt = true,
        exportModeHttp = false
    )
    @set:Import(keyResId = R.string.preferenceKeyCleanSession)
    var cleanSession: Boolean
        get() = getBooleanOrDefault(R.string.preferenceKeyCleanSession, R.bool.valCleanSession)
        set(newValue) {
            setBoolean(R.string.preferenceKeyCleanSession, newValue)
        }

    @get:Export(
        keyResId = R.string.preferenceKeyConnectionTimeoutSeconds,
        exportModeMqtt = true,
        exportModeHttp = true
    )
    @set:Import(keyResId = R.string.preferenceKeyConnectionTimeoutSeconds)
    var connectionTimeoutSeconds: Int
        get() = getIntOrDefault(
            R.string.preferenceKeyConnectionTimeoutSeconds,
            R.integer.defaultConnectionTimeoutSeconds
        )
        set(newValue) {
            setInt(R.string.preferenceKeyConnectionTimeoutSeconds, newValue.coerceAtLeast(1))
        }

    @get:Export(
        keyResId = R.string.preferenceKeyPublishExtendedData,
        exportModeMqtt = true,
        exportModeHttp = true
    )
    @set:Import(keyResId = R.string.preferenceKeyPublishExtendedData)
    var pubLocationExtendedData: Boolean
        get() = getBooleanOrDefault(
            R.string.preferenceKeyPublishExtendedData,
            R.bool.valPubExtendedData
        )
        set(newValue) {
            setBoolean(R.string.preferenceKeyPublishExtendedData, newValue)
        }

    @get:Export(
        keyResId = R.string.preferenceKeyLocatorInterval,
        exportModeMqtt = true,
        exportModeHttp = true
    )
    @set:Import(keyResId = R.string.preferenceKeyLocatorInterval)
    var locatorInterval: Int
        get() = getIntOrDefault(R.string.preferenceKeyLocatorInterval, R.integer.valLocatorInterval)
        set(anInt) {
            setInt(R.string.preferenceKeyLocatorInterval, anInt)
        }

    @get:Export(
        keyResId = R.string.preferenceKeyMoveModeLocatorInterval,
        exportModeMqtt = true,
        exportModeHttp = true
    )
    @set:Import(keyResId = R.string.preferenceKeyMoveModeLocatorInterval)
    var moveModeLocatorInterval: Int
        get() = getIntOrDefault(
            R.string.preferenceKeyMoveModeLocatorInterval,
            R.integer.valMoveModeLocatorInterval
        )
        set(moveModeLocatorInterval) {
            setInt(R.string.preferenceKeyMoveModeLocatorInterval, moveModeLocatorInterval)
        }

    // Unit is minutes
    @get:Export(keyResId = R.string.preferenceKeyPing, exportModeMqtt = true, exportModeHttp = true)
    @set:Import(keyResId = R.string.preferenceKeyPing)
    var ping: Int
        get() = getIntOrDefault(R.string.preferenceKeyPing, R.integer.valPing).coerceAtLeast(
            TimeUnit.MILLISECONDS.toMinutes(Scheduler.MIN_PERIODIC_INTERVAL_MILLIS).toInt()
        )
        set(anInt) {
            setInt(R.string.preferenceKeyPing, anInt)
        }

    @get:Export(
        keyResId = R.string.preferenceKeyUsername,
        exportModeMqtt = true,
        exportModeHttp = true
    )
    @set:Import(keyResId = R.string.preferenceKeyUsername)
    var username: String
        get() = getStringOrDefault(R.string.preferenceKeyUsername, R.string.valEmpty)
        set(value) {
            setString(R.string.preferenceKeyUsername, value)
        }

    @get:Export(
        keyResId = R.string.preferenceKeyDeviceId,
        exportModeMqtt = true,
        exportModeHttp = true
    )
    @set:Import(keyResId = R.string.preferenceKeyDeviceId)
    var deviceId: String
        get() = getDeviceId(true)
        set(deviceId) {
            setString(R.string.preferenceKeyDeviceId, deviceId)
        }

    fun getDeviceId(fallbackToDefault: Boolean): String {
        val deviceId = getStringOrDefault(R.string.preferenceKeyDeviceId, R.string.valEmpty)
        return if ("" == deviceId && fallbackToDefault) deviceIdDefault else deviceId
    }

    @get:Export(
        keyResId = R.string.preferenceKeyIgnoreStaleLocations,
        exportModeMqtt = true,
        exportModeHttp = true
    )
    @set:Import(keyResId = R.string.preferenceKeyIgnoreStaleLocations)
    var ignoreStaleLocations: Double
        get() = getStringOrDefault(
            R.string.preferenceKeyIgnoreStaleLocations,
            R.string.valIgnoreStaleLocations
        ).toDouble()
        set(days) {
            setString(R.string.preferenceKeyIgnoreStaleLocations, days.toString())
        }

    @get:Export(
        keyResId = R.string.preferenceKeyIgnoreInaccurateLocations,
        exportModeMqtt = true,
        exportModeHttp = true
    )
    @set:Import(keyResId = R.string.preferenceKeyIgnoreInaccurateLocations)
    var ignoreInaccurateLocations: Int
        get() = getIntOrDefault(
            R.string.preferenceKeyIgnoreInaccurateLocations,
            R.integer.valIgnoreInaccurateLocations
        )
        set(meters) {
            setInt(R.string.preferenceKeyIgnoreInaccurateLocations, meters)
        }

    @get:Export(keyResId = R.string.preferenceKeyClientId, exportModeMqtt = true)
    @set:Import(keyResId = R.string.preferenceKeyClientId)
    var clientId: String
        get() {
            var clientId = getStringOrDefault(R.string.preferenceKeyClientId, R.string.valEmpty)
            if ("" == clientId) clientId = clientIdDefault
            return clientId
        }
        set(clientId) {
            setString(R.string.preferenceKeyClientId, clientId)
        }

    @get:Export(keyResId = R.string.preferenceKeyPubTopicBase, exportModeMqtt = true)
    @set:Import(keyResId = R.string.preferenceKeyPubTopicBase)
    var pubTopicBaseFormatString: String
        get() = getStringOrDefault(R.string.preferenceKeyPubTopicBase, R.string.valPubTopic)
        set(deviceTopic) {
            setString(R.string.preferenceKeyPubTopicBase, deviceTopic)
        }

    val pubTopicBase: String
        get() = pubTopicBaseFormatString.replace(
            "%u",
            username.ifBlank { "user" }
        ).replace("%d", deviceId)

    @get:Export(keyResId = R.string.preferenceKeySubTopic, exportModeMqtt = true)
    @set:Import(keyResId = R.string.preferenceKeySubTopic)
    var subTopic: String
        get() = getStringOrDefault(R.string.preferenceKeySubTopic, R.string.defaultSubTopic)
        set(string) {
            setString(R.string.preferenceKeySubTopic, string)
        }

    @get:Export(keyResId = R.string.preferenceKeySub, exportModeMqtt = true)
    @set:Import(keyResId = R.string.preferenceKeySub)
    var sub: Boolean
        get() = getBooleanOrDefault(R.string.preferenceKeySub, R.bool.valSub)
        set(sub) {
            setBoolean(R.string.preferenceKeySub, sub)
        }

    // value validation - must be max 2 characters, only letters and digits
    @get:Export(
        keyResId = R.string.preferenceKeyTrackerId,
        exportModeMqtt = true,
        exportModeHttp = true
    )
    @set:Import(keyResId = R.string.preferenceKeyTrackerId)
    var trackerId: String
        get() = getTrackerId(false)
        set(trackerId) {
            val len = trackerId.length
            // value validation - must be max 2 characters, only letters and digits
            if (len >= 2) {
                val shortTrackerId = trackerId.substring(0, 2)
                if (Character.isLetterOrDigit(shortTrackerId[0]) && Character.isLetterOrDigit(
                        shortTrackerId[1]
                    )
                ) {
                    setString(R.string.preferenceKeyTrackerId, shortTrackerId)
                }
            } else {
                if (len > 0 && Character.isLetterOrDigit(trackerId[0])) {
                    setString(
                        R.string.preferenceKeyTrackerId,
                        trackerId
                    )
                } else {
                    setString(R.string.preferenceKeyTrackerId, "")
                }
            }
        }

    fun getTrackerId(fallback: Boolean): String {
        val tid = getStringOrDefault(R.string.preferenceKeyTrackerId, R.string.valEmpty)
        return tid.ifEmpty { if (fallback) trackerIdDefault else "" }
    }

    // defaults to the last two characters of configured deviceId.
    // Empty trackerId won't be included in the message.
    private val trackerIdDefault: String
        get() {
            val deviceId: String = deviceId
            return if (deviceId.length >= 2) {
                deviceId.substring(deviceId.length - 2) // defaults to the last two characters of configured deviceId.
            } else {
                "na" // Empty trackerId won't be included in the message.
            }
        }

    @get:Export(keyResId = R.string.preferenceKeyPort, exportModeMqtt = true)
    @set:Import(keyResId = R.string.preferenceKeyPort)
    var port: Int
        get() = getIntOrDefault(R.string.preferenceKeyPort, R.integer.valPort)
        set(port) {
            if (port in 1..65535) setInt(R.string.preferenceKeyPort, port) else setPortDefault()
        }

    val portWithHintSupport: String
        get() = getIntWithHintSupport(R.string.preferenceKeyPort)

    fun setPortDefault() {
        clearKey(R.string.preferenceKeyPort)
    }

    @get:Export(keyResId = R.string.preferenceKeyMqttProtocolLevel, exportModeMqtt = true)
    @set:Import(keyResId = R.string.preferenceKeyMqttProtocolLevel)
    var mqttProtocolLevel: Int
        get() = getIntOrDefault(
            R.string.preferenceKeyMqttProtocolLevel,
            R.integer.valMqttProtocolLevel
        )
        set(mqttProtocolLevel) {
            setInt(
                R.string.preferenceKeyMqttProtocolLevel,
                if (
                    mqttProtocolLevel == MqttConnectOptions.MQTT_VERSION_DEFAULT ||
                    mqttProtocolLevel == MqttConnectOptions.MQTT_VERSION_3_1 ||
                    mqttProtocolLevel == MqttConnectOptions.MQTT_VERSION_3_1_1
                ) {
                    mqttProtocolLevel
                } else {
                    MqttConnectOptions.MQTT_VERSION_DEFAULT
                }
            )
        }

    // Unit is seconds
    // Minimum time is 15minutes because work manager cannot schedule any faster
    @get:Export(keyResId = R.string.preferenceKeyKeepalive, exportModeMqtt = true)
    @set:Import(keyResId = R.string.preferenceKeyKeepalive)
    var keepalive: Int
        get() {
            if (isExperimentalFeatureEnabled(EXPERIMENTAL_FEATURE_ALLOW_SMALL_KEEPALIVE)) {
                return getIntOrDefault(
                    R.string.preferenceKeyKeepalive,
                    R.integer.valKeepalive
                ).coerceAtLeast(1)
            }
            return getIntOrDefault(
                R.string.preferenceKeyKeepalive,
                R.integer.valKeepalive
            ).coerceAtLeast(
                TimeUnit.MILLISECONDS.toSeconds(Scheduler.MIN_PERIODIC_INTERVAL_MILLIS).toInt()
            )
        }
        set(value) {
            when {
                isExperimentalFeatureEnabled(EXPERIMENTAL_FEATURE_ALLOW_SMALL_KEEPALIVE) -> {
                    setInt(R.string.preferenceKeyKeepalive, value.coerceAtLeast(1))
                }
                keepAliveInRange(value) -> setInt(R.string.preferenceKeyKeepalive, value)
                else -> setKeepaliveDefault()
            }
        }

    fun keepAliveInRange(i: Int): Boolean =
        i >= TimeUnit.MILLISECONDS.toSeconds(Scheduler.MIN_PERIODIC_INTERVAL_MILLIS)

    val minimumKeepalive = TimeUnit.MILLISECONDS.toSeconds(Scheduler.MIN_PERIODIC_INTERVAL_MILLIS)

    val keepaliveWithHintSupport: String
        get() = getIntWithHintSupport(R.string.preferenceKeyKeepalive)

    fun setKeepaliveDefault() {
        clearKey(R.string.preferenceKeyKeepalive)
    }

    @get:Export(
        keyResId = R.string.preferenceKeyNotificationEvents,
        exportModeMqtt = true,
        exportModeHttp = true
    )
    @set:Import(keyResId = R.string.preferenceKeyNotificationEvents)
    var notificationEvents: Boolean
        get() = getBooleanOrDefault(
            R.string.preferenceKeyNotificationEvents,
            R.bool.valNotificationEvents
        )
        set(notificationEvents) {
            setBoolean(R.string.preferenceKeyNotificationEvents, notificationEvents)
        }

    @get:Export(
        keyResId = R.string.preferenceKeyLocatorDisplacement,
        exportModeMqtt = true,
        exportModeHttp = true
    )
    @set:Import(keyResId = R.string.preferenceKeyLocatorDisplacement)
    var locatorDisplacement: Int
        get() = getIntOrDefault(
            R.string.preferenceKeyLocatorDisplacement,
            R.integer.valLocatorDisplacement
        )
        set(anInt) {
            setInt(R.string.preferenceKeyLocatorDisplacement, anInt)
        }

    @get:Export(
        keyResId = R.string.preferenceKeyLocatorPriority,
        exportModeMqtt = true,
        exportModeHttp = true
    )
    @set:Import(keyResId = R.string.preferenceKeyLocatorPriority)
    var locatorPriority: Int
        get() = getIntOrDefault(R.string.preferenceKeyLocatorPriority, R.integer.valLocatorPriority)
        set(anInt) {
            if (anInt in 0..3) {
                setInt(R.string.preferenceKeyLocatorPriority, anInt)
            } else {
                Timber.e("invalid locator priority specified %s", anInt)
            }
        }

    @get:Export(keyResId = R.string.preferenceKeyTLSCaCrt, exportModeMqtt = true)
    @set:Import(keyResId = R.string.preferenceKeyTLSCaCrt)
    var tlsCaCrt: String
        get() = getStringOrDefault(R.string.preferenceKeyTLSCaCrt, R.string.valEmpty)
        set(name) {
            setString(R.string.preferenceKeyTLSCaCrt, name)
        }
    var userDeclinedEnableNotificationPermissions: Boolean
        get() = getBooleanOrDefault(
            R.string.preferenceKeyUserDeclinedEnableNotificationPermissions,
            R.bool.valFalse
        )
        set(value) {
            setBoolean(R.string.preferenceKeyUserDeclinedEnableNotificationPermissions, value)
        }

    @get:Export(keyResId = R.string.preferenceKeyHost, exportModeMqtt = true)
    @set:Import(keyResId = R.string.preferenceKeyHost)
    var host: String
        get() = getStringOrDefault(R.string.preferenceKeyHost, R.string.valEmpty)
        set(value) {
            setString(R.string.preferenceKeyHost, value)
        }

    @get:Export(
        keyResId = R.string.preferenceKeyPassword,
        exportModeMqtt = true,
        exportModeHttp = true
    )
    @set:Import(keyResId = R.string.preferenceKeyPassword)
    var password: String
        get() = getStringOrDefault(R.string.preferenceKeyPassword, R.string.valEmpty)
        set(password) {
            setString(R.string.preferenceKeyPassword, password)
        }

    @get:Export(keyResId = R.string.preferenceKeyTLS, exportModeMqtt = true)
    @set:Import(keyResId = R.string.preferenceKeyTLS)
    var tls: Boolean
        get() = getBooleanOrDefault(R.string.preferenceKeyTLS, R.bool.valTls)
        set(tlsSpecifier) {
            setBoolean(R.string.preferenceKeyTLS, tlsSpecifier)
        }

    @get:Export(keyResId = R.string.preferenceKeyWS, exportModeMqtt = true)
    @set:Import(keyResId = R.string.preferenceKeyWS)
    var ws: Boolean
        get() = getBooleanOrDefault(R.string.preferenceKeyWS, R.bool.valWs)
        set(wsEnable) {
            setBoolean(R.string.preferenceKeyWS, wsEnable)
        }

    @get:Export(keyResId = R.string.preferenceKeyTLSClientCrt, exportModeMqtt = true)
    @set:Import(keyResId = R.string.preferenceKeyTLSClientCrt)
    var tlsClientCrt: String
        get() = getStringOrDefault(R.string.preferenceKeyTLSClientCrt, R.string.valEmpty)
        set(name) {
            setString(R.string.preferenceKeyTLSClientCrt, name)
        }

    @get:Export(
        keyResId = R.string.preferenceKeyNotificationHigherPriority,
        exportModeMqtt = true,
        exportModeHttp = true
    )
    @set:Import(keyResId = R.string.preferenceKeyNotificationHigherPriority)
    var notificationHigherPriority: Boolean
        get() = getBooleanOrDefault(
            R.string.preferenceKeyNotificationHigherPriority,
            R.bool.valNotificationHigherPriority
        )
        set(newValue) {
            setBoolean(R.string.preferenceKeyNotificationHigherPriority, newValue)
        }

    @get:Export(
        keyResId = R.string.preferenceKeyNotificationLocation,
        exportModeMqtt = true,
        exportModeHttp = true
    )
    @set:Import(keyResId = R.string.preferenceKeyNotificationLocation)
    var notificationLocation: Boolean
        get() = getBooleanOrDefault(
            R.string.preferenceKeyNotificationLocation,
            R.bool.valNotificationLocation
        )
        set(newValue) {
            setBoolean(R.string.preferenceKeyNotificationLocation, newValue)
        }

    @get:Export(keyResId = R.string.preferenceKeyPubQos, exportModeMqtt = true)
    @set:Import(keyResId = R.string.preferenceKeyPubQos)
    var pubQos: Int
        get() = getIntOrDefault(R.string.preferenceKeyPubQos, R.integer.valPubQos).coerceAtMost(
            MQTT_MAX_QOS
        ).coerceAtLeast(MQTT_MIN_QOS)
        set(anInt) {
            setInt(
                R.string.preferenceKeyPubQos,
                anInt.coerceAtMost(MQTT_MAX_QOS).coerceAtLeast(MQTT_MIN_QOS)
            )
        }

    @get:Export(keyResId = R.string.preferenceKeyPubRetain, exportModeMqtt = true)
    @set:Import(keyResId = R.string.preferenceKeyPubRetain)
    var pubRetain: Boolean
        get() = getBooleanOrDefault(R.string.preferenceKeyPubRetain, R.bool.valPubRetain)
        set(newValue) {
            setBoolean(R.string.preferenceKeyPubRetain, newValue)
        }

    @get:Export(keyResId = R.string.preferenceKeySubQos, exportModeMqtt = true)
    @set:Import(keyResId = R.string.preferenceKeySubQos)
    var subQos: Int
        get() = getIntOrDefault(R.string.preferenceKeySubQos, R.integer.valSubQos).coerceAtMost(
            MQTT_MAX_QOS
        ).coerceAtLeast(MQTT_MIN_QOS)
        set(anInt) {
            setInt(
                R.string.preferenceKeySubQos,
                anInt.coerceAtMost(MQTT_MAX_QOS).coerceAtLeast(MQTT_MIN_QOS)
            )
        }

    @get:Export(
        keyResId = R.string.preferenceKeyAutostartOnBoot,
        exportModeMqtt = true,
        exportModeHttp = true
    )
    @set:Import(keyResId = R.string.preferenceKeyAutostartOnBoot)
    var autostartOnBoot: Boolean
        get() = getBooleanOrDefault(
            R.string.preferenceKeyAutostartOnBoot,
            R.bool.valAutostartOnBoot
        )
        set(newValue) {
            setBoolean(R.string.preferenceKeyAutostartOnBoot, newValue)
        }

    @get:Export(keyResId = R.string.preferenceKeyInfo, exportModeMqtt = true)
    @set:Import(keyResId = R.string.preferenceKeyInfo)
    var info: Boolean
        get() = getBooleanOrDefault(R.string.preferenceKeyInfo, R.bool.valInfo)
        set(info) {
            setBoolean(R.string.preferenceKeyInfo, info)
        }

    @get:Export(keyResId = R.string.preferenceKeyTLSClientCrtPassword, exportModeMqtt = true)
    @set:Import(keyResId = R.string.preferenceKeyTLSClientCrtPassword)
    var tlsClientCrtPassword: String
        get() = getStringOrDefault(R.string.preferenceKeyTLSClientCrtPassword, R.string.valEmpty)
        set(password) {
            setString(R.string.preferenceKeyTLSClientCrtPassword, password)
        }

    @get:Export(keyResId = R.string.preferenceKeyURL, exportModeHttp = true)
    @set:Import(keyResId = R.string.preferenceKeyURL)
    var url: String
        get() = getStringOrDefault(R.string.preferenceKeyURL, R.string.valEmpty)
        set(url) {
            setString(R.string.preferenceKeyURL, url)
        }

    @get:Export(
        keyResId = R.string.preferenceKeyFusedRegionDetection,
        exportModeMqtt = true,
        exportModeHttp = true
    )
    @set:Import(keyResId = R.string.preferenceKeyFusedRegionDetection)
    var fusedRegionDetection: Boolean
        get() = getBooleanOrDefault(R.string.preferenceKeyFusedRegionDetection, R.bool.valTrue)
        set(newValue) {
            setBoolean(R.string.preferenceKeyFusedRegionDetection, newValue)
        }

    @get:Export(
        keyResId = R.string.preferenceKeyDebugLog,
        exportModeMqtt = true,
        exportModeHttp = true
    )
    @set:Import(keyResId = R.string.preferenceKeyDebugLog)
    var debugLog: Boolean
        get() = getBooleanOrDefault(R.string.preferenceKeyDebugLog, R.bool.valFalse)
        set(debug) {
            setBoolean(R.string.preferenceKeyDebugLog, debug)
        }

    @get:Export(
        keyResId = R.string.preferenceKeyRemoteConfiguration,
        exportModeMqtt = true,
        exportModeHttp = true
    )
    @set:Import(keyResId = R.string.preferenceKeyRemoteConfiguration)
    var remoteConfiguration: Boolean
        get() = getBooleanOrDefault(R.string.preferenceKeyRemoteConfiguration, R.bool.valFalse)
        set(newValue) {
            setBoolean(R.string.preferenceKeyRemoteConfiguration, newValue)
        }

    @get:Export(
        keyResId = R.string.preferenceKeyNotificationGeocoderErrors,
        exportModeMqtt = true,
        exportModeHttp = true
    )
    @set:Import(keyResId = R.string.preferenceKeyNotificationGeocoderErrors)
    var notificationGeocoderErrors: Boolean
        get() = getBooleanOrDefault(
            R.string.preferenceKeyNotificationGeocoderErrors,
            R.bool.valNotificationGeocoderErrors
        )
        set(newValue) {
            setBoolean(R.string.preferenceKeyNotificationGeocoderErrors, newValue)
        }

    @get:Export(
        keyResId = R.string.preferenceKeyReverseGeocodeProvider,
        exportModeMqtt = true,
        exportModeHttp = true
    )
    @set:Import(keyResId = R.string.preferenceKeyReverseGeocodeProvider)
    var reverseGeocodeProvider: String
        get() {
            val currentValue = getStringOrDefault(
                R.string.preferenceKeyReverseGeocodeProvider,
                R.string.valDefaultGeocoder
            )
            return if (!REVERSE_GEOCODE_PROVIDERS.contains(currentValue)) {
                val default = getStringResource(R.string.valDefaultGeocoder)
                reverseGeocodeProvider = default
                default
            } else {
                currentValue
            }
        }
        set(newValue) {
            if (REVERSE_GEOCODE_PROVIDERS.contains(newValue)) {
                setString(R.string.preferenceKeyReverseGeocodeProvider, newValue)
            } else {
                setString(
                    R.string.preferenceKeyReverseGeocodeProvider,
                    getStringResource(R.string.valDefaultGeocoder)
                )
            }
        }

    @get:Export(
        keyResId = R.string.preferenceKeyExperimentalFeatures,
        exportModeMqtt = true,
        exportModeHttp = true
    )
    @set:Import(keyResId = R.string.preferenceKeyExperimentalFeatures)
    var experimentalFeatures: Collection<String>
        get() = getStringSet(R.string.preferenceKeyExperimentalFeatures).toSortedSet()
        set(value) {
            setStringSet(R.string.preferenceKeyExperimentalFeatures, value.toSet())
            if (value.contains(EXPERIMENTAL_FEATURE_ENABLE_APP_SHORTCUTS)) {
                appShortcuts.enableLogViewerShortcut(applicationContext)
            } else {
                appShortcuts.disableLogViewerShortcut(applicationContext)
            }
        }

    @get:Export(keyResId = R.string.preferenceKeyTheme, exportModeMqtt = true)
    @set:Import(keyResId = R.string.preferenceKeyTheme)
    var theme: Int
        get() = getIntOrDefault(R.string.preferenceKeyTheme, R.integer.defaultTheme)
        set(value) {
            val actualValue = if (!NIGHT_MODES.contains(value)) {
                R.integer.defaultTheme
            } else {
                value
            }
            setInt(R.string.preferenceKeyTheme, actualValue)
            when (actualValue) {
                NIGHT_MODE_AUTO -> AppCompatDelegate.setDefaultNightMode(SYSTEM_NIGHT_AUTO_MODE)
                NIGHT_MODE_ENABLE -> AppCompatDelegate.setDefaultNightMode(
                    AppCompatDelegate.MODE_NIGHT_YES
                )
                NIGHT_MODE_DISABLE -> AppCompatDelegate.setDefaultNightMode(
                    AppCompatDelegate.MODE_NIGHT_NO
                )
            }
        }

    @get:Export(
        keyResId = R.string.preferenceKeyShowRegionsOnMap,
        exportModeMqtt = true,
        exportModeHttp = true
    )
    @set:Import(keyResId = R.string.preferenceKeyShowRegionsOnMap)
    var showRegionsOnMap: Boolean
        get() = getBooleanOrDefault(
            R.string.preferenceKeyShowRegionsOnMap,
            R.bool.valShowRegionsOnMap
        )
        set(newValue) {
            setBoolean(R.string.preferenceKeyShowRegionsOnMap, newValue)
        }

    @get:Export(
        keyResId = R.string.preferenceKeyPegLocatorFastestIntervalToInterval,
        exportModeMqtt = true,
        exportModeHttp = true
    )
    @set:Import(keyResId = R.string.preferenceKeyPegLocatorFastestIntervalToInterval)
    var pegLocatorFastestIntervalToInterval: Boolean
        get() = getBooleanOrDefault(
            R.string.preferenceKeyPegLocatorFastestIntervalToInterval,
            R.bool.valPegLocatorFastestIntervalToInterval
        )
        set(newValue) {
            setBoolean(R.string.preferenceKeyPegLocatorFastestIntervalToInterval, newValue)
        }

    @get:Export(
        keyResId = R.string.preferenceKeyMapLayerStyle,
        exportModeMqtt = true,
        exportModeHttp = true
    )
    @set:Import(keyResId = R.string.preferenceKeyMapLayerStyle)
    var mapLayerStyle: MapLayerStyle
        get() = MapLayerStyle.values().firstOrNull {
            it.name == getStringOrDefault(
                R.string.preferenceKeyMapLayerStyle,
                R.string.valDefaultMapLayerStyle
            )
        } ?: MapLayerStyle.OpenStreetMapNormal
        set(newValue) {
            setString(R.string.preferenceKeyMapLayerStyle, newValue.name)
        }

    @get:Export(
        keyResId = R.string.preferenceKeyOsmTileScaleFactor,
        exportModeMqtt = true,
        exportModeHttp = true
    )
    @set:Import(keyResId = R.string.preferenceKeyOsmTileScaleFactor)
    var osmTileScaleFactor: Float
        get() = getFloatOrDefault(R.string.preferenceKeyOsmTileScaleFactor, 1.0f)
        set(newValue) {
            setFloat(R.string.preferenceKeyOsmTileScaleFactor, newValue)
        }

    @get:Export(
        keyResId = R.string.preferenceKeyEnableMapRotation,
        exportModeMqtt = true,
        exportModeHttp = true
    )
    @set:Import(keyResId = R.string.preferenceKeyEnableMapRotation)
    var enableMapRotation: Boolean
        get() = getBooleanOrDefault(
            R.string.preferenceKeyEnableMapRotation,
            R.bool.valEnableMapRotation
        )
        set(newValue) {
            setBoolean(R.string.preferenceKeyEnableMapRotation, newValue)
        }

    // Not used on public, as many people might use the same device type
    private val deviceIdDefault: String
        get() = // Use device name (Mako, Surnia, etc. and strip all non alpha digits)
            Build.DEVICE?.replace(" ", "-")?.replace("[^a-zA-Z0-9]+".toRegex(), "")
                ?.lowercase(Locale.getDefault())
                ?: "unknown"

    private val clientIdDefault: String
        get() = (username + deviceId).replace("\\W".toRegex(), "").lowercase(Locale.getDefault())

    val pubTopicLocations: String
        get() = pubTopicBase

    val pubTopicWaypoints: String
        get() = pubTopicBase + pubTopicWaypointsPart

    val pubTopicWaypointsPart: String
        get() = "/waypoints"

    val pubTopicEvents: String
        get() = pubTopicBase + pubTopicEventsPart

    val pubTopicEventsPart: String
        get() = "/event"

    val pubTopicInfoPart: String
        get() = "/info"

    val pubTopicCommands: String
        get() = pubTopicBase + pubTopicCommandsPart

    val pubTopicCommandsPart: String
        get() = "/cmd"

    // Maybe make this configurable
// For now it makes things easier to change
    val pubQosEvents: Int
        get() = pubQos

    val pubRetainEvents: Boolean
        get() = false

    val pubQosWaypoints: Int
        get() = 0

    val pubRetainWaypoints: Boolean
        get() = false

    val pubQosLocations: Int
        get() = pubQos

    val pubRetainLocations: Boolean
        get() = pubRetain

    val encryptionKey: String
        get() = getStringOrDefault(R.string.preferenceKeyEncryptionKey, R.string.valEmpty)

    // sharedPreferences because the value is independent from the selected mode
    val isSetupCompleted: Boolean
        get() = // sharedPreferences because the value is independent from the selected mode
            !preferencesStore.getBoolean(
                getPreferenceKey(R.string.preferenceKeySetupNotCompleted),
                true
            )

    fun setSetupCompleted() {
        preferencesStore.putBoolean(
            getPreferenceKey(R.string.preferenceKeySetupNotCompleted),
            false
        )
        isFirstStart = false
    }

    val isObjectboxMigrated: Boolean
        get() = isFirstStart || preferencesStore.getBoolean(
            getPreferenceKey(R.string.preferenceKeyObjectboxMigrated),
            false
        )

    var userDeclinedEnableLocationPermissions: Boolean
        get() = preferencesStore.getBoolean(
            preferenceKeyUserDeclinedEnableLocationPermissions,
            false
        )
        set(value) = preferencesStore.putBoolean(
            preferenceKeyUserDeclinedEnableLocationPermissions,
            value
        )

    var userDeclinedEnableLocationServices: Boolean
        get() = preferencesStore.getBoolean(preferenceKeyUserDeclinedEnableLocationServices, false)
        set(value) = preferencesStore.putBoolean(
            preferenceKeyUserDeclinedEnableLocationServices,
            value
        )

    fun setObjectBoxMigrated() {
        preferencesStore.putBoolean(getPreferenceKey(R.string.preferenceKeyObjectboxMigrated), true)
    }

    private fun getBooleanOrDefault(resKeyId: Int, defId: Int): Boolean {
        return preferencesStore.getBoolean(getPreferenceKey(resKeyId), getBooleanResource(defId))
    }

    private fun getBooleanResource(resId: Int): Boolean {
        return applicationContext.resources.getBoolean(resId)
    }

    private fun getIntOrDefault(resKeyId: Int, defId: Int): Int {
        return try {
            preferencesStore.getInt(getPreferenceKey(resKeyId), getIntResource(defId))
        } catch (e: ClassCastException) {
            Timber.e(
                "Error retrieving string preference %s, returning default",
                getPreferenceKey(resKeyId)
            )
            val default = getIntResource(defId)
            preferencesStore.putInt(getPreferenceKey(resKeyId), default)
            default
        }
    }

    private fun getIntResource(resId: Int): Int {
        return applicationContext.resources.getInteger(resId)
    }

    private fun getIntWithHintSupport(resKeyId: Int): String {
        val i = getIntOrDefault(resKeyId, R.integer.valInvalid)
        return if (i == -1) {
            ""
        } else {
            i.toString()
        }
    }

    private fun getStringOrDefault(resKeyId: Int, defId: Int): String {
        val key = getPreferenceKey(resKeyId)
        return try {
            val s = preferencesStore.getString(key, "")
            if ("" == s) getStringResource(defId) else s!!
        } catch (e: ClassCastException) {
            Timber.e("Error retriving string preference %s, returning default", key)
            getStringResource(defId)
        }
    }

    private fun getStringResource(resId: Int): String {
        return applicationContext.resources.getString(resId)
    }

    private fun setString(resKeyId: Int, value: String) {
        preferencesStore.putString(getPreferenceKey(resKeyId), value)
    }

    private fun setInt(resKeyId: Int, value: Int) {
        preferencesStore.putInt(getPreferenceKey(resKeyId), value)
    }

    private fun setBoolean(resKeyId: Int, value: Boolean) {
        preferencesStore.putBoolean(getPreferenceKey(resKeyId), value)
    }

    private fun setStringSet(resKeyId: Int, value: Set<String>) {
        preferencesStore.putStringSet(getPreferenceKey(resKeyId), value)
    }

    private fun getStringSet(resKeyId: Int): Set<String> {
        return preferencesStore.getStringSet(getPreferenceKey(resKeyId))
    }

    private fun setFloat(resKeyId: Int, value: Float) {
        preferencesStore.putFloat(getPreferenceKey(resKeyId), value)
    }

    private fun getFloatOrDefault(resKeyId: Int, default: Float): Float {
        return preferencesStore.getFloat(getPreferenceKey(resKeyId), default)
    }

    private fun clearKey(key: String?) {
        preferencesStore.remove(key!!)
    }

    private fun clearKey(resKeyId: Int) {
        clearKey(getPreferenceKey(resKeyId))
    }

    fun isExperimentalFeatureEnabled(feature: String): Boolean {
        return when {
            else -> experimentalFeatures.contains(feature)
        }
    }

    @Retention(AnnotationRetention.RUNTIME)
    @Target(
        AnnotationTarget.FUNCTION,
        AnnotationTarget.PROPERTY_GETTER,
        AnnotationTarget.PROPERTY_SETTER
    )
    annotation class Export(
        val keyResId: Int = 0,
        val exportModeMqtt: Boolean = false,
        val exportModeHttp: Boolean = false
    )

    @Retention(AnnotationRetention.RUNTIME)
    @Target(
        AnnotationTarget.FUNCTION,
        AnnotationTarget.PROPERTY_GETTER,
        AnnotationTarget.PROPERTY_SETTER
    )
    annotation class Import(val keyResId: Int = 0)

    fun getPreferenceKey(res: Int): String {
        return getStringResource(res)
    }

    init {
        val modePreferenceKey = getPreferenceKey(R.string.preferenceKeyModeId)
        val initMode = preferencesStore.getInitMode(
            modePreferenceKey,
            getIntResource(R.integer.valModeId)
        )
        if (initMode in listOf(
                MessageProcessorEndpointMqtt.MODE_ID,
                MessageProcessorEndpointHttp.MODE_ID
            )
        ) {
            setMode(initMode, true)
        } else {
            preferencesStore.putInt(modePreferenceKey, getIntResource(R.integer.valModeId))
            setMode(getIntResource(R.integer.valModeId), true)
        }

        // Migrations
        if (preferencesStore.hasKey(getPreferenceKey(R.string.preferenceKeyGeocodeEnabled))) {
            val oldEnabledValue = preferencesStore.getBoolean(
                getPreferenceKey(R.string.preferenceKeyGeocodeEnabled),
                false
            )

            val opencageApiKey = preferencesStore.getString(
                getPreferenceKey(R.string.preferenceKeyOpencageGeocoderApiKey),
                ""
            )

            reverseGeocodeProvider = if (oldEnabledValue && opencageApiKey.isNullOrBlank()) {
                REVERSE_GEOCODE_PROVIDER_DEVICE
            } else if (oldEnabledValue && !opencageApiKey.isNullOrBlank()) {
                REVERSE_GEOCODE_PROVIDER_OPENCAGE
            } else {
                REVERSE_GEOCODE_PROVIDER_NONE
            }
            preferencesStore.remove(getPreferenceKey(R.string.preferenceKeyGeocodeEnabled))
        }
        // Migrate old "google" reverse geocoder value to "device"
        if (preferencesStore.hasKey(getPreferenceKey(R.string.preferencesReverseGeocodeProvider)) && preferencesStore.getString(
                getPreferenceKey(R.string.preferencesReverseGeocodeProvider),
                ""
            ) == "Google"
        ) {
            preferencesStore.putString(
                getPreferenceKey(R.string.preferencesReverseGeocodeProvider),
                REVERSE_GEOCODE_PROVIDER_DEVICE
            )
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
        const val REVERSE_GEOCODE_PROVIDER_NONE = "None"
        const val REVERSE_GEOCODE_PROVIDER_DEVICE = "Device"
        const val REVERSE_GEOCODE_PROVIDER_OPENCAGE = "OpenCage"
        val REVERSE_GEOCODE_PROVIDERS = setOf(
            REVERSE_GEOCODE_PROVIDER_NONE,
            REVERSE_GEOCODE_PROVIDER_DEVICE,
            REVERSE_GEOCODE_PROVIDER_OPENCAGE
        )

        const val NIGHT_MODE_DISABLE = 0
        const val NIGHT_MODE_ENABLE = 1
        const val NIGHT_MODE_AUTO = 2
        val NIGHT_MODES = setOf(
            NIGHT_MODE_AUTO,
            NIGHT_MODE_DISABLE,
            NIGHT_MODE_ENABLE
        )
        val SYSTEM_NIGHT_AUTO_MODE by lazy {
            if (SDK_INT > Build.VERSION_CODES.Q) MODE_NIGHT_FOLLOW_SYSTEM else MODE_NIGHT_AUTO_BATTERY
        }

        const val MQTT_MIN_QOS = 0
        const val MQTT_MAX_QOS = 2

        // Preference Keys
        const val preferenceKeyUserDeclinedEnableLocationPermissions =
            "userDeclinedEnableLocationPermissions"
        const val preferenceKeyUserDeclinedEnableLocationServices =
            "userDeclinedEnableLocationServices"
    }
}
