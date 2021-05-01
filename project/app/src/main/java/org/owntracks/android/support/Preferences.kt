package org.owntracks.android.support

import android.annotation.SuppressLint
import android.content.Context
import android.os.Build
import org.eclipse.paho.client.mqttv3.MqttConnectOptions
import org.greenrobot.eventbus.EventBus
import org.owntracks.android.BuildConfig
import org.owntracks.android.R
import org.owntracks.android.injection.qualifier.AppContext
import org.owntracks.android.model.messages.MessageConfiguration
import org.owntracks.android.services.LocationProcessor
import org.owntracks.android.services.MessageProcessorEndpointHttp
import org.owntracks.android.services.MessageProcessorEndpointMqtt
import org.owntracks.android.services.worker.Scheduler
import org.owntracks.android.support.Events.ModeChanged
import org.owntracks.android.support.Events.MonitoringChanged
import org.owntracks.android.support.preferences.OnModeChangedPreferenceChangedListener
import org.owntracks.android.support.preferences.PreferencesStore
import timber.log.Timber
import java.lang.reflect.InvocationTargetException
import java.lang.reflect.Method
import java.lang.reflect.ParameterizedType
import java.lang.reflect.Type
import java.util.*
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class Preferences @Inject constructor(@AppContext c: Context, private val eventBus: EventBus?, private val preferencesStore: PreferencesStore) {
    private val context: Context = c
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
                            (currentMode == MessageProcessorEndpointMqtt.MODE_ID && annotation.exportModeMqtt ||
                                    currentMode == MessageProcessorEndpointHttp.MODE_ID && annotation.exportModeHttp)
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

    fun registerOnPreferenceChangedListener(listener: OnModeChangedPreferenceChangedListener?) {
        preferencesStore.registerOnSharedPreferenceChangeListener(listener!!)
    }

    fun unregisterOnPreferenceChangedListener(listener: OnModeChangedPreferenceChangedListener?) {
        preferencesStore.unregisterOnSharedPreferenceChangeListener(listener!!)
    }

    fun checkFirstStart() {
        if (preferencesStore.getBoolean(getPreferenceKey(R.string.preferenceKeyFirstStart), true)) {
            Timber.v("Initial application launch")
            isFirstStart = true
            preferencesStore.putBoolean(getPreferenceKey(R.string.preferenceKeyFirstStart), false)
            preferencesStore.putBoolean(getPreferenceKey(R.string.preferenceKeySetupNotCompleted), true)
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
        if (t is ParameterizedType && Set::class.java.isAssignableFrom(t.rawType as Class<*>))
            return value.split(",").map { it.trim() }.filter { it.isNotBlank() }.toSortedSet()
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
            Timber.v("setting mode to %s", messageConfiguration[getPreferenceKey(R.string.preferenceKeyModeId)])
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
                .forEach {
                    Timber.d("Loading key %s from method: %s", it, methods.getValue(it).name)
                    try {
                        methods.getValue(it).invoke(this, messageConfiguration[it])
                    } catch (e: IllegalArgumentException) {
                        Timber.e("Tried to import %s but value is wrong type. Expected: %s, given %s", it, methods.getValue(it).parameterTypes.first().canonicalName, messageConfiguration[it]?.javaClass?.canonicalName)
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
        var mode = monitoring
        if (mode < LocationProcessor.MONITORING_MOVE) {
            mode++
        } else {
            mode = LocationProcessor.MONITORING_QUIET
        }
        Timber.v("setting monitoring mode %s", mode)
        monitoring = mode
    }

    @get:Export(keyResId = R.string.preferenceKeyModeId, exportModeMqtt = true, exportModeHttp = true)
    @set:Import(keyResId = R.string.preferenceKeyModeId)
    var mode: Int
        get() = currentMode
        set(active) {
            setMode(active, false)
        }


    @get:Export(keyResId = R.string.preferenceKeyMonitoring, exportModeMqtt = true, exportModeHttp = true)
    @set:Import(keyResId = R.string.preferenceKeyMonitoring)
    var monitoring: Int
        get() = getIntOrDefault(R.string.preferenceKeyMonitoring, R.integer.valMonitoring)
        set(newMode) {
            if (newMode < LocationProcessor.MONITORING_QUIET || newMode > LocationProcessor.MONITORING_MOVE) {
                Timber.e("invalid monitoring mode specified %s", newMode)
                return
            }
            if (newMode != this.monitoring) {
                setInt(R.string.preferenceKeyMonitoring, newMode)
                eventBus?.post(MonitoringChanged())
            }
        }

    @get:Export(keyResId = R.string.preferenceKeyDontReuseHttpClient, exportModeMqtt = false, exportModeHttp = true)
    @set:Import(keyResId = R.string.preferenceKeyDontReuseHttpClient)
    var dontReuseHttpClient: Boolean
        get() = getBooleanOrDefault(R.string.preferenceKeyDontReuseHttpClient, R.bool.valFalse)
        set(newValue) {
            setBoolean(R.string.preferenceKeyDontReuseHttpClient, newValue)
        }

    @get:Export(keyResId = R.string.preferenceKeyOpencageGeocoderApiKey, exportModeMqtt = true, exportModeHttp = true)
    @set:Import(keyResId = R.string.preferenceKeyOpencageGeocoderApiKey)
    var openCageGeocoderApiKey: String
        get() = getStringOrDefault(R.string.preferenceKeyOpencageGeocoderApiKey, R.string.valEmpty)
        set(key) {
            setString(R.string.preferenceKeyOpencageGeocoderApiKey, key.trim())
        }

    @get:Export(keyResId = R.string.preferenceKeyRemoteCommand, exportModeMqtt = true, exportModeHttp = true)
    @set:Import(keyResId = R.string.preferenceKeyRemoteCommand)
    var remoteCommand: Boolean
        get() = getBooleanOrDefault(R.string.preferenceKeyRemoteCommand, R.bool.valRemoteCommand)
        set(newValue) {
            setBoolean(R.string.preferenceKeyRemoteCommand, newValue)
        }

    @get:Export(keyResId = R.string.preferenceKeyCleanSession, exportModeMqtt = true, exportModeHttp = true)
    @set:Import(keyResId = R.string.preferenceKeyCleanSession)
    var cleanSession: Boolean
        get() = getBooleanOrDefault(R.string.preferenceKeyCleanSession, R.bool.valCleanSession)
        set(newValue) {
            setBoolean(R.string.preferenceKeyCleanSession, newValue)
        }

    @get:Export(keyResId = R.string.preferenceKeyPublishExtendedData, exportModeMqtt = true, exportModeHttp = true)
    @set:Import(keyResId = R.string.preferenceKeyPublishExtendedData)
    var pubLocationExtendedData: Boolean
        get() = getBooleanOrDefault(R.string.preferenceKeyPublishExtendedData, R.bool.valPubExtendedData)
        set(newValue) {
            setBoolean(R.string.preferenceKeyPublishExtendedData, newValue)
        }

    @get:Export(keyResId = R.string.preferenceKeyLocatorInterval, exportModeMqtt = true, exportModeHttp = true)
    @set:Import(keyResId = R.string.preferenceKeyLocatorInterval)
    var locatorInterval: Int
        get() = getIntOrDefault(R.string.preferenceKeyLocatorInterval, R.integer.valLocatorInterval)
        set(anInt) {
            setInt(R.string.preferenceKeyLocatorInterval, anInt)
        }

    @get:Export(keyResId = R.string.preferenceKeyMoveModeLocatorInterval, exportModeMqtt = true, exportModeHttp = true)
    @set:Import(keyResId = R.string.preferenceKeyMoveModeLocatorInterval)
    var moveModeLocatorInterval: Int
        get() = getIntOrDefault(R.string.preferenceKeyMoveModeLocatorInterval, R.integer.valMoveModeLocatorInterval)
        set(moveModeLocatorInterval) {
            setInt(R.string.preferenceKeyMoveModeLocatorInterval, moveModeLocatorInterval)
        }

    // Unit is minutes
    // Unit is minutes
    @get:Export(keyResId = R.string.preferenceKeyPing, exportModeMqtt = true, exportModeHttp = true)
    @set:Import(keyResId = R.string.preferenceKeyPing)
    var ping: Int
        get() = getIntOrDefault(R.string.preferenceKeyPing, R.integer.valPing).coerceAtLeast(TimeUnit.MILLISECONDS.toMinutes(Scheduler.MIN_PERIODIC_INTERVAL_MILLIS).toInt())
        set(anInt) {
            setInt(R.string.preferenceKeyPing, anInt)
        }

    @get:Export(keyResId = R.string.preferenceKeyUsername, exportModeMqtt = true, exportModeHttp = true)
    @set:Import(keyResId = R.string.preferenceKeyUsername)
    var username: String
        get() = getStringOrDefault(R.string.preferenceKeyUsername, R.string.valEmpty)
        set(value) {
            setString(R.string.preferenceKeyUsername, value)
        }

    @get:Export(keyResId = R.string.preferenceKeyDeviceId, exportModeMqtt = true, exportModeHttp = true)
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

    @get:Export(keyResId = R.string.preferenceKeyIgnoreStaleLocations, exportModeMqtt = true, exportModeHttp = true)
    @set:Import(keyResId = R.string.preferenceKeyIgnoreStaleLocations)
    var ignoreStaleLocations: Double
        get() = getStringOrDefault(R.string.preferenceKeyIgnoreStaleLocations, R.string.valIgnoreStaleLocations).toDouble()
        set(days) {
            setString(R.string.preferenceKeyIgnoreStaleLocations, days.toString())
        }

    @get:Export(keyResId = R.string.preferenceKeyIgnoreInaccurateLocations, exportModeMqtt = true, exportModeHttp = true)
    @set:Import(keyResId = R.string.preferenceKeyIgnoreInaccurateLocations)
    var ignoreInaccurateLocations: Int
        get() = getIntOrDefault(R.string.preferenceKeyIgnoreInaccurateLocations, R.integer.valIgnoreInaccurateLocations)
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
        get() = pubTopicBaseFormatString.replace("%u", username).replace("%d", deviceId)

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
    @get:Export(keyResId = R.string.preferenceKeyTrackerId, exportModeMqtt = true, exportModeHttp = true)
    @set:Import(keyResId = R.string.preferenceKeyTrackerId)
    var trackerId: String
        get() = getTrackerId(false)
        set(trackerId) {
            val len = trackerId.length
            // value validation - must be max 2 characters, only letters and digits
            if (len >= 2) {
                val shortTrackerId = trackerId.substring(0, 2)
                if (Character.isLetterOrDigit(shortTrackerId[0]) && Character.isLetterOrDigit(shortTrackerId[1])) setString(R.string.preferenceKeyTrackerId, shortTrackerId)
            } else {
                if (len > 0 && Character.isLetterOrDigit(trackerId[0])) setString(R.string.preferenceKeyTrackerId, trackerId) else setString(R.string.preferenceKeyTrackerId, "")
            }
        }

    fun getTrackerId(fallback: Boolean): String {
        val tid = getStringOrDefault(R.string.preferenceKeyTrackerId, R.string.valEmpty)
        return if (tid.isEmpty()) if (fallback) trackerIdDefault else "" else tid
    }

    // defaults to the last two characters of configured deviceId.
    // Empty trackerId won't be included in the message.
    private val trackerIdDefault: String
        get() {
            val deviceId: String = deviceId
            return if (deviceId.length >= 2) deviceId.substring(deviceId.length - 2) // defaults to the last two characters of configured deviceId.
            else "na" // Empty trackerId won't be included in the message.
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
        get() = getIntOrDefault(R.string.preferenceKeyMqttProtocolLevel, R.integer.valMqttProtocolLevel)
        set(mqttProtocolLevel) {
            setInt(R.string.preferenceKeyMqttProtocolLevel, if (
                    mqttProtocolLevel == MqttConnectOptions.MQTT_VERSION_DEFAULT ||
                    mqttProtocolLevel == MqttConnectOptions.MQTT_VERSION_3_1 ||
                    mqttProtocolLevel == MqttConnectOptions.MQTT_VERSION_3_1_1) mqttProtocolLevel else MqttConnectOptions.MQTT_VERSION_DEFAULT)
        }

    // Unit is seconds
    // Minimum time is 15minutes because work manager cannot schedule any faster
    @get:Export(keyResId = R.string.preferenceKeyKeepalive, exportModeMqtt = true)
    @set:Import(keyResId = R.string.preferenceKeyKeepalive)
    var keepalive: Int
        get() {
            if (isExperimentalFeatureEnabled(EXPERIMENTAL_FEATURE_ALLOW_SMALL_KEEPALIVE)) {
                return getIntOrDefault(R.string.preferenceKeyKeepalive, R.integer.valKeepalive).coerceAtLeast(1)
            }
            return getIntOrDefault(R.string.preferenceKeyKeepalive, R.integer.valKeepalive).coerceAtLeast(TimeUnit.MILLISECONDS.toSeconds(Scheduler.MIN_PERIODIC_INTERVAL_MILLIS).toInt())
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

    fun keepAliveInRange(i: Int): Boolean = i >= TimeUnit.MILLISECONDS.toSeconds(Scheduler.MIN_PERIODIC_INTERVAL_MILLIS)

    val minimumKeepalive = TimeUnit.MILLISECONDS.toSeconds(Scheduler.MIN_PERIODIC_INTERVAL_MILLIS)

    val keepaliveWithHintSupport: String
        get() = getIntWithHintSupport(R.string.preferenceKeyKeepalive)


    fun setKeepaliveDefault() {
        clearKey(R.string.preferenceKeyKeepalive)
    }

    @get:Export(keyResId = R.string.preferenceKeyNotificationEvents, exportModeMqtt = true, exportModeHttp = true)
    @set:Import(keyResId = R.string.preferenceKeyNotificationEvents)
    var notificationEvents: Boolean
        get() = getBooleanOrDefault(R.string.preferenceKeyNotificationEvents, R.bool.valNotificationEvents)
        set(notificationEvents) {
            setBoolean(R.string.preferenceKeyNotificationEvents, notificationEvents)
        }

    @get:Export(keyResId = R.string.preferenceKeyLocatorDisplacement, exportModeMqtt = true, exportModeHttp = true)
    @set:Import(keyResId = R.string.preferenceKeyLocatorDisplacement)
    var locatorDisplacement: Int
        get() = getIntOrDefault(R.string.preferenceKeyLocatorDisplacement, R.integer.valLocatorDisplacement)
        set(anInt) {
            setInt(R.string.preferenceKeyLocatorDisplacement, anInt)
        }

    @get:Export(keyResId = R.string.preferenceKeyLocatorPriority, exportModeMqtt = true, exportModeHttp = true)
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

    @get:Export(keyResId = R.string.preferenceKeyHost, exportModeMqtt = true)
    @set:Import(keyResId = R.string.preferenceKeyHost)
    var host: String
        get() = getStringOrDefault(R.string.preferenceKeyHost, R.string.valEmpty)
        set(value) {
            setString(R.string.preferenceKeyHost, value)
        }

    @get:Export(keyResId = R.string.preferenceKeyPassword, exportModeMqtt = true, exportModeHttp = true)
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

    @get:Export(keyResId = R.string.preferenceKeyNotificationHigherPriority, exportModeMqtt = true, exportModeHttp = true)
    @set:Import(keyResId = R.string.preferenceKeyNotificationHigherPriority)
    var notificationHigherPriority: Boolean
        get() = getBooleanOrDefault(R.string.preferenceKeyNotificationHigherPriority, R.bool.valNotificationHigherPriority)
        set(newValue) {
            setBoolean(R.string.preferenceKeyNotificationHigherPriority, newValue)
        }

    @get:Export(keyResId = R.string.preferenceKeyNotificationLocation, exportModeMqtt = true, exportModeHttp = true)
    @set:Import(keyResId = R.string.preferenceKeyNotificationLocation)
    var notificationLocation: Boolean
        get() = getBooleanOrDefault(R.string.preferenceKeyNotificationLocation, R.bool.valNotificationLocation)
        set(newValue) {
            setBoolean(R.string.preferenceKeyNotificationLocation, newValue)
        }

    @get:Export(keyResId = R.string.preferenceKeyPubQos, exportModeMqtt = true)
    @set:Import(keyResId = R.string.preferenceKeyPubQos)
    var pubQos: Int
        get() = getIntOrDefault(R.string.preferenceKeyPubQos, R.integer.valPubQos)
        set(anInt) {
            setInt(R.string.preferenceKeyPubQos, anInt.coerceAtMost(2))
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
        get() = getIntOrDefault(R.string.preferenceKeySubQos, R.integer.valSubQos)
        set(anInt) {
            setInt(R.string.preferenceKeySubQos, anInt.coerceAtMost(2))
        }

    @get:Export(keyResId = R.string.preferenceKeyAutostartOnBoot, exportModeMqtt = true, exportModeHttp = true)
    @set:Import(keyResId = R.string.preferenceKeyAutostartOnBoot)
    var autostartOnBoot: Boolean
        get() = getBooleanOrDefault(R.string.preferenceKeyAutostartOnBoot, R.bool.valAutostartOnBoot)
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

    @get:Export(keyResId = R.string.preferenceKeyFusedRegionDetection, exportModeMqtt = true, exportModeHttp = true)
    @set:Import(keyResId = R.string.preferenceKeyFusedRegionDetection)
    var fusedRegionDetection: Boolean
        get() = getBooleanOrDefault(R.string.preferenceKeyFusedRegionDetection, R.bool.valTrue)
        set(newValue) {
            setBoolean(R.string.preferenceKeyFusedRegionDetection, newValue)
        }

    @get:Export(keyResId = R.string.preferenceKeyDebugLog, exportModeMqtt = true, exportModeHttp = true)
    @set:Import(keyResId = R.string.preferenceKeyDebugLog)
    var debugLog: Boolean
        get() = getBooleanOrDefault(R.string.preferenceKeyDebugLog, R.bool.valFalse)
        set(debug) {
            setBoolean(R.string.preferenceKeyDebugLog, debug)
        }

    @get:Export(keyResId = R.string.preferenceKeyRemoteConfiguration, exportModeMqtt = true, exportModeHttp = true)
    @set:Import(keyResId = R.string.preferenceKeyRemoteConfiguration)
    var remoteConfiguration: Boolean
        get() = getBooleanOrDefault(R.string.preferenceKeyRemoteConfiguration, R.bool.valFalse)
        set(newValue) {
            setBoolean(R.string.preferenceKeyRemoteConfiguration, newValue)
        }

    @Import(keyResId = R.string.preferenceKeyGeocodeEnabled)
    fun setGeocodeEnabled(newValue: Boolean) {
        reverseGeocodeProvider = if (newValue) REVERSE_GEOCODE_PROVIDER_GOOGLE else REVERSE_GEOCODE_PROVIDER_NONE
    }

    @get:Export(keyResId = R.string.preferenceKeyReverseGeocodeProvider, exportModeMqtt = true, exportModeHttp = true)
    @set:Import(keyResId = R.string.preferenceKeyReverseGeocodeProvider)
    var reverseGeocodeProvider: String
        get() = getStringOrDefault(R.string.preferenceKeyReverseGeocodeProvider, R.string.valDefaultGeocoder)
        set(newValue) {
            if (REVERSE_GEOCODE_PROVIDERS.contains(newValue)) {
                setString(R.string.preferenceKeyReverseGeocodeProvider, newValue)
            } else {
                setString(R.string.preferenceKeyReverseGeocodeProvider, REVERSE_GEOCODE_PROVIDER_NONE)
            }
        }


    @get:Export(keyResId = R.string.preferenceKeyExperimentalFeatures, exportModeMqtt = true, exportModeHttp = true)
    @set:Import(keyResId = R.string.preferenceKeyExperimentalFeatures)
    var experimentalFeatures: Set<String>
        get() = getStringSet(R.string.preferenceKeyExperimentalFeatures).toSortedSet()
        set(value) {
            setStringSet(R.string.preferenceKeyExperimentalFeatures, value)
        }


    // Not used on public, as many people might use the same device type
    private val deviceIdDefault: String
        get() = // Use device name (Mako, Surnia, etc. and strip all non alpha digits)
            Build.DEVICE?.replace(" ", "-")?.replace("[^a-zA-Z0-9]+".toRegex(), "")?.toLowerCase(Locale.getDefault())
                    ?: "unknown"


    private val clientIdDefault: String
        get() = (username + deviceId).replace("\\W".toRegex(), "").toLowerCase(Locale.getDefault())

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
        get() =// sharedPreferences because the value is independent from the selected mode
            !preferencesStore.getBoolean(getPreferenceKey(R.string.preferenceKeySetupNotCompleted), true)

    fun setSetupCompleted() {
        preferencesStore.putBoolean(getPreferenceKey(R.string.preferenceKeySetupNotCompleted), false)
        isFirstStart = false
    }

    val isObjectboxMigrated: Boolean
        get() = isFirstStart || preferencesStore.getBoolean(getPreferenceKey(R.string.preferenceKeyObjectboxMigrated), false)

    fun setObjectBoxMigrated() {
        preferencesStore.putBoolean(getPreferenceKey(R.string.preferenceKeyObjectboxMigrated), true)
    }


    private fun getBooleanOrDefault(resKeyId: Int, defId: Int): Boolean {
        return preferencesStore.getBoolean(getPreferenceKey(resKeyId), getBooleanResource(defId))
    }

    private fun getBooleanResource(resId: Int): Boolean {
        return context.resources.getBoolean(resId)
    }

    private fun getIntOrDefault(resKeyId: Int, defId: Int): Int {
        return try {
            preferencesStore.getInt(getPreferenceKey(resKeyId), getIntResource(defId))
        } catch (e: ClassCastException) {
            Timber.e("Error retriving string preference %s, returning default", getPreferenceKey(resKeyId))
            getIntResource(defId)
        }
    }

    private fun getIntResource(resId: Int): Int {
        return context.resources.getInteger(resId)
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
        return context.resources.getString(resId)
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
    @Target(AnnotationTarget.FUNCTION, AnnotationTarget.PROPERTY_GETTER, AnnotationTarget.PROPERTY_SETTER)
    annotation class Export(val keyResId: Int = 0, val exportModeMqtt: Boolean = false, val exportModeHttp: Boolean = false)

    @Retention(AnnotationRetention.RUNTIME)
    @Target(AnnotationTarget.FUNCTION, AnnotationTarget.PROPERTY_GETTER, AnnotationTarget.PROPERTY_SETTER)
    annotation class Import(val keyResId: Int = 0)

    fun getPreferenceKey(res: Int): String {
        return getStringResource(res)
    }

    init {
        val modePreferenceKey = getPreferenceKey(R.string.preferenceKeyModeId)
        val initMode = preferencesStore.getInitMode(
                modePreferenceKey,
                getIntResource(R.integer.valModeId))
        setMode(initMode, true)

        // Migrations
        if (preferencesStore.hasKey(getPreferenceKey(R.string.preferenceKeyGeocodeEnabled))) {
            val oldEnabledValue = preferencesStore.getBoolean(getPreferenceKey(R.string.preferenceKeyGeocodeEnabled), false)

            val opencageApiKey = preferencesStore.getString(getPreferenceKey(R.string.preferenceKeyOpencageGeocoderApiKey), "")

            reverseGeocodeProvider = if (oldEnabledValue && opencageApiKey.isNullOrBlank()) {
                REVERSE_GEOCODE_PROVIDER_GOOGLE
            } else if (oldEnabledValue && !opencageApiKey.isNullOrBlank()) {
                REVERSE_GEOCODE_PROVIDER_OPENCAGE
            } else {
                REVERSE_GEOCODE_PROVIDER_NONE
            }
            preferencesStore.remove(getPreferenceKey(R.string.preferenceKeyGeocodeEnabled))
        }
    }

    companion object {
        const val EXPERIMENTAL_FEATURE_ALLOW_SMALL_KEEPALIVE = "allowSmallKeepalive"
        const val EXPERIMENTAL_FEATURE_USE_AOSP_LOCATION_PROVIDER = "useAospLocationProvider"
        const val EXPERIMENTAL_FEATURE_USE_OSM_MAP = "useOSMMap"
        const val EXPERIMENTAL_FEATURE_SHOW_EXPERIMENTAL_PREFERENCE_UI = "showExperimentalPreferenceUI"
        internal val EXPERIMENTAL_FEATURES = setOf(EXPERIMENTAL_FEATURE_ALLOW_SMALL_KEEPALIVE, EXPERIMENTAL_FEATURE_USE_OSM_MAP, EXPERIMENTAL_FEATURE_USE_AOSP_LOCATION_PROVIDER, EXPERIMENTAL_FEATURE_SHOW_EXPERIMENTAL_PREFERENCE_UI)
        const val REVERSE_GEOCODE_PROVIDER_NONE = "None"
        const val REVERSE_GEOCODE_PROVIDER_GOOGLE = "Google"
        const val REVERSE_GEOCODE_PROVIDER_OPENCAGE = "OpenCage"
        val REVERSE_GEOCODE_PROVIDERS = listOf(REVERSE_GEOCODE_PROVIDER_NONE, REVERSE_GEOCODE_PROVIDER_GOOGLE, REVERSE_GEOCODE_PROVIDER_OPENCAGE)
    }
}