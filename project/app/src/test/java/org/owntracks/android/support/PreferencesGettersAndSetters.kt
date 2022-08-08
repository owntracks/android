package org.owntracks.android.support

import android.content.Context
import android.content.pm.ShortcutManager
import android.content.res.Resources
import org.eclipse.paho.client.mqttv3.MqttConnectOptions.MQTT_VERSION_3_1_1
import org.eclipse.paho.client.mqttv3.MqttConnectOptions.MQTT_VERSION_DEFAULT
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.owntracks.android.R
import org.owntracks.android.model.messages.MessageConfiguration
import org.owntracks.android.services.MessageProcessorEndpointHttp
import org.owntracks.android.services.MessageProcessorEndpointMqtt
import org.owntracks.android.support.preferences.PreferencesStore
import org.owntracks.android.ui.NoopAppShortcuts
import org.owntracks.android.ui.map.MapLayerStyle
import kotlin.reflect.KClass

@RunWith(Parameterized::class)
class PreferencesGettersAndSetters(
    private val preferenceMethodName: String,
    private val preferenceName: String,
    private val preferenceValue: Any,
    private val preferenceValueExpected: Any,
    private val preferenceType: KClass<Any>,
    private val httpOnlyMode: Boolean
) {
    private lateinit var mockResources: Resources
    private lateinit var mockContext: Context
    private lateinit var preferencesStore: PreferencesStore
    private lateinit var shortcutService: ShortcutManager

    @Before
    fun createMocks() {
        shortcutService = mock<ShortcutManager> {}
        mockResources = getMockResources()
        mockContext = mock {
            on { resources } doReturn mockResources
            on { packageName } doReturn javaClass.canonicalName
            on { getSystemService(Context.SHORTCUT_SERVICE) } doReturn shortcutService
        }
        preferencesStore = InMemoryPreferencesStore()
    }

    @Test
    fun `when setting a preference ensure that the preference is set correctly on export`() {
        val preferences = Preferences(mockContext, null, preferencesStore, NoopAppShortcuts())
        val setter =
            Preferences::class.java.getMethod("set$preferenceMethodName", preferenceType.java)
        if (httpOnlyMode) {
            preferences.mode = MessageProcessorEndpointHttp.MODE_ID
        }
        setter.invoke(preferences, preferenceValue)
        val messageConfiguration = preferences.exportToMessage()
        assertEquals(preferenceValueExpected, messageConfiguration[preferenceName])
    }

    @Test
    fun `when importing a configuration ensure that the supplied preference is set to the given value`() {
        val preferences = Preferences(mockContext, null, preferencesStore, NoopAppShortcuts())
        val messageConfiguration = MessageConfiguration()
        messageConfiguration[preferenceName] = preferenceValue
        preferences.importFromMessage(messageConfiguration)
        val getter = Preferences::class.java.getMethod("get$preferenceMethodName")
        assertEquals(preferenceValueExpected, getter.invoke(preferences))
    }

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "{index}: {0} (sets={2}, expected={3})")
        fun data(): Iterable<Array<Any>> {
            return arrayListOf(
                arrayOf("AutostartOnBoot", "autostartOnBoot", true, true, Boolean::class, false),
                arrayOf("CleanSession", "cleanSession", true, true, Boolean::class, false),
                arrayOf(
                    "ClientId", // Method name
                    "clientId", // Preference name
                    "testClientId", // Given preference value
                    "testClientId", // Expected preference value
                    String::class, // Preference type
                    false // HTTP only
                ),
                arrayOf(
                    "ConnectionTimeoutSeconds",
                    "connectionTimeoutSeconds",
                    20,
                    20,
                    Int::class,
                    false
                ),
                arrayOf(
                    "ConnectionTimeoutSeconds",
                    "connectionTimeoutSeconds",
                    -5,
                    1,
                    Int::class,
                    false
                ),
                arrayOf("DebugLog", "debugLog", true, true, Boolean::class, false),
                arrayOf("DeviceId", "deviceId", "deviceId", "deviceId", String::class, false),
                arrayOf(
                    "DontReuseHttpClient",
                    "dontReuseHttpClient",
                    true,
                    true,
                    Boolean::class,
                    true
                ),
                arrayOf(
                    "ExperimentalFeatures",
                    "experimentalFeatures",
                    setOf("this", "that", "other"),
                    setOf("this", "that", "other"),
                    Collection::class,
                    false
                ),
                arrayOf(
                    "FusedRegionDetection",
                    "fusedRegionDetection",
                    true,
                    true,
                    Boolean::class,
                    false
                ),
                arrayOf(
                    "ReverseGeocodeProvider",
                    "reverseGeocodeProvider",
                    "Device",
                    "Device",
                    String::class,
                    false
                ),
                arrayOf(
                    "ReverseGeocodeProvider",
                    "reverseGeocodeProvider",
                    "OpenCage",
                    "OpenCage",
                    String::class,
                    false
                ),
                arrayOf(
                    "ReverseGeocodeProvider",
                    "reverseGeocodeProvider",
                    "None",
                    "None",
                    String::class,
                    false
                ),
                arrayOf(
                    "ReverseGeocodeProvider",
                    "reverseGeocodeProvider",
                    "Nonsense",
                    "None",
                    String::class,
                    false
                ),
                arrayOf("Host", "host", "testHost", "testHost", String::class, false),
                arrayOf(
                    "IgnoreInaccurateLocations",
                    "ignoreInaccurateLocations",
                    123,
                    123,
                    Int::class,
                    false
                ),
                arrayOf(
                    "IgnoreStaleLocations",
                    "ignoreStaleLocations",
                    456.0,
                    456.0,
                    Double::class,
                    false
                ),
                arrayOf("Info", "info", true, true, Boolean::class, false),
                arrayOf("Keepalive", "keepalive", 1500, 1500, Int::class, false),
                arrayOf("Keepalive", "keepalive", 900, 900, Int::class, false),
                arrayOf("Keepalive", "keepalive", 899, 900, Int::class, false),
                arrayOf("Keepalive", "keepalive", 0, 900, Int::class, false),
                arrayOf("Keepalive", "keepalive", -1, 900, Int::class, false),
                arrayOf(
                    "LocatorDisplacement",
                    "locatorDisplacement",
                    1690,
                    1690,
                    Int::class,
                    false
                ),
                arrayOf("LocatorInterval", "locatorInterval", 1000, 1000, Int::class, false),
                arrayOf("LocatorPriority", "locatorPriority", 2, 2, Int::class, false),
                arrayOf(
                    "MapLayerStyle",
                    "mapLayerStyle",
                    MapLayerStyle.GoogleMapHybrid,
                    MapLayerStyle.GoogleMapHybrid,
                    MapLayerStyle::class,
                    false
                ),
                arrayOf(
                    "Mode",
                    "mode",
                    MessageProcessorEndpointHttp.MODE_ID,
                    MessageProcessorEndpointHttp.MODE_ID,
                    Int::class,
                    false
                ),
                arrayOf(
                    "Mode",
                    "mode",
                    MessageProcessorEndpointMqtt.MODE_ID,
                    MessageProcessorEndpointMqtt.MODE_ID,
                    Int::class,
                    false
                ),
                arrayOf(
                    "Mode",
                    "mode",
                    -1,
                    MessageProcessorEndpointMqtt.MODE_ID,
                    Int::class,
                    false
                ),
                arrayOf(
                    "Monitoring", "monitoring",
                    MonitoringMode.SIGNIFICANT,
                    MonitoringMode.SIGNIFICANT,
                    MonitoringMode::class,
                    false
                ),
                arrayOf(
                    "MoveModeLocatorInterval",
                    "moveModeLocatorInterval",
                    1500,
                    1500,
                    Int::class,
                    false
                ),
                arrayOf(
                    "MqttProtocolLevel",
                    "mqttProtocolLevel",
                    MQTT_VERSION_3_1_1,
                    MQTT_VERSION_3_1_1,
                    Int::class,
                    false
                ),
                arrayOf(
                    "MqttProtocolLevel",
                    "mqttProtocolLevel",
                    -1,
                    MQTT_VERSION_DEFAULT,
                    Int::class,
                    false
                ),
                arrayOf(
                    "NotificationEvents",
                    "notificationEvents",
                    true,
                    true,
                    Boolean::class,
                    false
                ),
                arrayOf(
                    "NotificationHigherPriority",
                    "notificationHigherPriority",
                    true,
                    true,
                    Boolean::class,
                    false
                ),
                arrayOf(
                    "NotificationLocation",
                    "notificationLocation",
                    true,
                    true,
                    Boolean::class,
                    false
                ),
                arrayOf(
                    "NotificationGeocoderErrors",
                    "notificationGeocoderErrors",
                    false,
                    false,
                    Boolean::class,
                    false
                ),
                arrayOf(
                    "OpenCageGeocoderApiKey",
                    "opencageApiKey",
                    "testOpencageAPIKey",
                    "testOpencageAPIKey",
                    String::class,
                    false
                ),
                arrayOf(
                    "OsmTileScaleFactor",
                    "osmTileScaleFactor",
                    1.3f,
                    1.3f,
                    Float::class,
                    false
                ),
                arrayOf(
                    "Password",
                    "password",
                    "testPassword!\"£",
                    "testPassword!\"£",
                    String::class,
                    false
                ),
                arrayOf(
                    "PegLocatorFastestIntervalToInterval",
                    "pegLocatorFastestIntervalToInterval",
                    false,
                    false,
                    Boolean::class,
                    false
                ),
                arrayOf("Ping", "ping", 400, 400, Int::class, false),
                arrayOf("Port", "port", 9999, 9999, Int::class, false),
                arrayOf("Port", "port", -50, 0, Int::class, false),
                arrayOf("Port", "port", 65536, 0, Int::class, false),
                arrayOf("Port", "port", 65535, 65535, Int::class, false),
                arrayOf(
                    "PubLocationExtendedData",
                    "pubExtendedData",
                    true,
                    true,
                    Boolean::class,
                    false
                ),
                arrayOf("PubQos", "pubQos", 1, 1, Int::class, false),
                arrayOf("PubRetain", "pubRetain", true, true, Boolean::class, false),
                arrayOf(
                    "PubTopicBaseFormatString",
                    "pubTopicBase",
                    "testDeviceTopic",
                    "testDeviceTopic",
                    String::class,
                    false
                ),
                arrayOf("RemoteCommand", "cmd", true, true, Boolean::class, false),
                arrayOf(
                    "RemoteConfiguration",
                    "remoteConfiguration",
                    true,
                    true,
                    Boolean::class,
                    false
                ),
                arrayOf("Sub", "sub", true, true, Boolean::class, false),
                arrayOf("SubQos", "subQos", 1, 1, Int::class, false),
                arrayOf(
                    "SubTopic",
                    "subTopic",
                    "testSubTopic",
                    "testSubTopic",
                    String::class,
                    false
                ),
                arrayOf("Tls", "tls", true, true, Boolean::class, false),
                arrayOf("TlsCaCrt", "tlsCaCrt", "caCertName", "caCertName", String::class, false),
                arrayOf(
                    "TlsClientCrt",
                    "tlsClientCrt",
                    "clientCertName",
                    "clientCertName",
                    String::class,
                    false
                ),
                arrayOf(
                    "TlsClientCrtPassword",
                    "tlsClientCrtPassword",
                    "clientCrtPassword",
                    "clientCrtPassword",
                    String::class,
                    false
                ),
                arrayOf("TrackerId", "tid", "t1", "t1", String::class, false),
                arrayOf("TrackerId", "tid", "trackerId", "tr", String::class, false),
                arrayOf(
                    "Url",
                    "url",
                    "https://www.example.com",
                    "https://www.example.com",
                    String::class,
                    true
                ),
                arrayOf("Username", "username", "testUser", "testUser", String::class, false),
                arrayOf("Ws", "ws", true, true, Boolean::class, false)
            ).toList()
        }

        fun getMockResources(): Resources {
            return mock {
                on { getString(any()) } doReturn ""
                on { getString(eq(R.string.valEmpty)) } doReturn ""
                on { getString(eq(R.string.preferenceKeyAuth)) } doReturn "auth"
                on { getString(eq(R.string.preferenceKeyAutostartOnBoot)) } doReturn "autostartOnBoot"
                on { getString(eq(R.string.preferenceKeyCleanSession)) } doReturn "cleanSession"
                on { getString(eq(R.string.preferenceKeyClientId)) } doReturn "clientId"
                on { getString(eq(R.string.preferenceKeyConnectionTimeoutSeconds)) } doReturn "connectionTimeoutSeconds"
                on { getString(eq(R.string.preferenceKeyDebugLog)) } doReturn "debugLog"
                on { getString(eq(R.string.preferenceKeyDeviceId)) } doReturn "deviceId"
                on { getString(eq(R.string.preferenceKeyDontReuseHttpClient)) } doReturn "dontReuseHttpClient"
                on { getString(eq(R.string.preferenceKeyEncryptionKey)) } doReturn "encryptionKey"
                on { getString(eq(R.string.preferenceKeyExperimentalFeatures)) } doReturn "experimentalFeatures"
                on { getString(eq(R.string.preferenceKeyFirstStart)) } doReturn "firstStart"
                on { getString(eq(R.string.preferenceKeyFusedRegionDetection)) } doReturn "fusedRegionDetection"
                on { getString(eq(R.string.preferenceKeyGeocodeEnabled)) } doReturn "geocodeEnabled"
                on { getString(eq(R.string.preferenceKeyHost)) } doReturn "host"
                on { getString(eq(R.string.preferenceKeyIgnoreInaccurateLocations)) } doReturn "ignoreInaccurateLocations"
                on { getString(eq(R.string.preferenceKeyIgnoreStaleLocations)) } doReturn "ignoreStaleLocations"
                on { getString(eq(R.string.preferenceKeyInfo)) } doReturn "info"
                on { getString(eq(R.string.preferenceKeyKeepalive)) } doReturn "keepalive"
                on { getString(eq(R.string.preferenceKeyLocatorDisplacement)) } doReturn "locatorDisplacement"
                on { getString(eq(R.string.preferenceKeyLocatorInterval)) } doReturn "locatorInterval"
                on { getString(eq(R.string.preferenceKeyLocatorPriority)) } doReturn "locatorPriority"
                on { getString(eq(R.string.preferenceKeyModeId)) } doReturn "mode"
                on { getString(eq(R.string.preferenceKeyMonitoring)) } doReturn "monitoring"
                on { getString(eq(R.string.preferenceKeyMoveModeLocatorInterval)) } doReturn "moveModeLocatorInterval"
                on { getString(eq(R.string.preferenceKeyMqttProtocolLevel)) } doReturn "mqttProtocolLevel"
                on { getString(eq(R.string.preferenceKeyNotificationEvents)) } doReturn "notificationEvents"
                on { getString(eq(R.string.preferenceKeyNotificationHigherPriority)) } doReturn "notificationHigherPriority"
                on { getString(eq(R.string.preferenceKeyNotificationLocation)) } doReturn "notificationLocation"
                on { getString(eq(R.string.preferenceKeyNotificationGeocoderErrors)) } doReturn "notificationGeocoderErrors"
                on { getString(eq(R.string.preferenceKeyObjectboxMigrated)) } doReturn "_objectboxMigrated"
                on { getString(eq(R.string.preferenceKeyOpencageGeocoderApiKey)) } doReturn "opencageApiKey"
                on { getString(eq(R.string.preferenceKeyOsmTileScaleFactor)) } doReturn "osmTileScaleFactor"
                on { getString(eq(R.string.preferenceKeyPassword)) } doReturn "password"
                on { getString(eq(R.string.preferenceKeyPing)) } doReturn "ping"
                on { getString(eq(R.string.preferenceKeyPort)) } doReturn "port"
                on { getString(eq(R.string.preferenceKeyPublishExtendedData)) } doReturn "pubExtendedData"
                on { getString(eq(R.string.preferenceKeyPubQos)) } doReturn "pubQos"
                on { getString(eq(R.string.preferenceKeyPubRetain)) } doReturn "pubRetain"
                on { getString(eq(R.string.preferenceKeyPubTopicBase)) } doReturn "pubTopicBase"
                on { getString(eq(R.string.preferenceKeyRemoteCommand)) } doReturn "cmd"
                on { getString(eq(R.string.preferenceKeyRemoteConfiguration)) } doReturn "remoteConfiguration"
                on { getString(eq(R.string.preferenceKeyReverseGeocodeProvider)) } doReturn "reverseGeocodeProvider"
                on { getString(eq(R.string.preferenceKeySetupNotCompleted)) } doReturn "setupNotCompleted"
                on { getString(eq(R.string.preferenceKeySub)) } doReturn "sub"
                on { getString(eq(R.string.preferenceKeySubQos)) } doReturn "subQos"
                on { getString(eq(R.string.preferenceKeySubTopic)) } doReturn "subTopic"
                on { getString(eq(R.string.preferenceKeyTLS)) } doReturn "tls"
                on { getString(eq(R.string.preferenceKeyTLSCaCrt)) } doReturn "tlsCaCrt"
                on { getString(eq(R.string.preferenceKeyTLSClientCrt)) } doReturn "tlsClientCrt"
                on { getString(eq(R.string.preferenceKeyTLSClientCrtPassword)) } doReturn "tlsClientCrtPassword"
                on { getString(eq(R.string.preferenceKeyTrackerId)) } doReturn "tid"
                on { getString(eq(R.string.preferenceKeyURL)) } doReturn "url"
                on { getString(eq(R.string.preferenceKeyUsepassword)) } doReturn "usePassword"
                on { getString(eq(R.string.preferenceKeyUsername)) } doReturn "username"
                on { getString(eq(R.string.preferenceKeyVersion)) } doReturn "_build"
                on { getString(eq(R.string.preferenceKeyWS)) } doReturn "ws"
                on { getString(eq(R.string.valIgnoreStaleLocations)) } doReturn "0"
                on { getString(eq(R.string.defaultSubTopic)) } doReturn "owntracks/+/+"
                on { getString(eq(R.string.valPubTopic)) } doReturn "owntracks/%u/%d"
                on { getString(eq(R.string.preferenceKeyShowRegionsOnMap)) } doReturn "showRegionsOnMap"
                on { getString(eq(R.string.preferenceKeyPegLocatorFastestIntervalToInterval)) } doReturn "pegLocatorFastestIntervalToInterval"
                on { getString(eq(R.string.preferenceKeyMapLayerStyle)) } doReturn "mapLayerStyle"
                on { getString(eq(R.string.valDefaultGeocoder)) } doReturn "None"
                on { getInteger(any()) } doReturn 0
                on { getBoolean(any()) } doReturn false
            }
        }
    }
}
