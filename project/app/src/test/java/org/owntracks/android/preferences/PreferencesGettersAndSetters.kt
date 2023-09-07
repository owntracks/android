package org.owntracks.android.preferences

import android.content.Context
import android.content.pm.ShortcutManager
import kotlin.reflect.KClass
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.owntracks.android.model.messages.MessageConfiguration
import org.owntracks.android.preferences.types.ConnectionMode
import org.owntracks.android.preferences.types.MonitoringMode
import org.owntracks.android.preferences.types.MqttProtocolLevel
import org.owntracks.android.preferences.types.MqttQos
import org.owntracks.android.preferences.types.ReverseGeocodeProvider
import org.owntracks.android.preferences.types.StringMaxTwoAlphaNumericChars
import org.owntracks.android.support.SimpleIdlingResource

@RunWith(Parameterized::class)
class PreferencesGettersAndSetters(private val parameter: Parameter) {
    private lateinit var mockContext: Context
    private lateinit var preferencesStore: PreferencesStore
    private lateinit var shortcutService: ShortcutManager
    private val mockIdlingResource = SimpleIdlingResource("mock", true)

    @Before
    fun createMocks() {
        shortcutService = mock {}
        mockContext = mock {
            on { packageName } doReturn javaClass.canonicalName
            on { getSystemService(Context.SHORTCUT_SERVICE) } doReturn shortcutService
        }
        preferencesStore = InMemoryPreferencesStore()
    }

    @Test
    fun `when setting a preference ensure that the preference is set correctly on export`() {
        val preferences = Preferences(preferencesStore, mockIdlingResource)
        val setter =
            Preferences::class.java.getMethod("set${parameter.preferenceMethodName}", parameter.preferenceType.java)
        if (parameter.httpOnlyMode) {
            preferences.mode = ConnectionMode.HTTP
        }
        setter.invoke(preferences, parameter.preferenceValue)
        val messageConfiguration = preferences.exportToMessage()
        assertEquals(parameter.preferenceValueExpected, messageConfiguration[parameter.preferenceName])
    }

    @Test
    fun `when importing a configuration ensure that the supplied preference is set to the given value`() {
        val preferences = Preferences(preferencesStore, mockIdlingResource)
        val messageConfiguration = MessageConfiguration()
        messageConfiguration[parameter.preferenceName] = parameter.preferenceValueInConfiguration
        preferences.importConfiguration(messageConfiguration)
        val getter = Preferences::class.java.getMethod("get${parameter.preferenceMethodName}")
        assertEquals(parameter.preferenceValueExpected, getter.invoke(preferences))
    }

    data class Parameter(
        val preferenceMethodName: String,
        val preferenceName: String,
        val preferenceValue: Any,
        val preferenceType: KClass<out Any>,
        val httpOnlyMode: Boolean,
        val preferenceValueExpected: Any = preferenceValue,
        val preferenceValueInConfiguration: Any = preferenceValue
    )

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "{index}: {0} (sets={2}, expected={3})")
        fun data(): Iterable<Parameter> {
            return arrayListOf(
                Parameter(
                    "AutostartOnBoot",
                    "autostartOnBoot",
                    true,
                    Boolean::class,
                    false
                ),
                Parameter(
                    "CleanSession",
                    "cleanSession",
                    true,
                    Boolean::class,
                    false
                ),
                Parameter(
                    "ClientId", // Method name
                    "clientId", // Preference name
                    "testClientId", // Given preference value
                    String::class, // Preference type
                    false // HTTP only
                ),
                Parameter(
                    "ConnectionTimeoutSeconds",
                    "connectionTimeoutSeconds",
                    20,
                    Int::class,
                    false
                ),
                Parameter(
                    "ConnectionTimeoutSeconds",
                    "connectionTimeoutSeconds",
                    -5,
                    Int::class,
                    false,
                    preferenceValueExpected = 1
                ),
                Parameter(
                    "DebugLog",
                    "debugLog",
                    true,
                    Boolean::class,
                    false
                ),
                Parameter(
                    "DeviceId",
                    "deviceId",
                    "deviceId",
                    String::class,
                    false
                ),
                Parameter(
                    "DontReuseHttpClient",
                    "dontReuseHttpClient",
                    true,
                    Boolean::class,
                    true
                ),
                Parameter(
                    "EnableMapRotation",
                    "enableMapRotation",
                    true,
                    Boolean::class,
                    false
                ),
                Parameter(
                    "ExperimentalFeatures",
                    "experimentalFeatures",
                    setOf("this", "that", "other"),
                    Set::class,
                    false
                ),
                Parameter(
                    "FusedRegionDetection",
                    "fusedRegionDetection",
                    true,
                    Boolean::class,
                    false
                ),
                Parameter(
                    "ReverseGeocodeProvider",
                    "reverseGeocodeProvider",
                    ReverseGeocodeProvider.DEVICE,
                    ReverseGeocodeProvider::class,
                    false,
                    preferenceValueInConfiguration = "Device"
                ),
                Parameter(
                    "ReverseGeocodeProvider",
                    "reverseGeocodeProvider",
                    ReverseGeocodeProvider.OPENCAGE,
                    ReverseGeocodeProvider::class,
                    false,
                    preferenceValueInConfiguration = "OpenCage"
                ),
                Parameter(
                    "ReverseGeocodeProvider",
                    "reverseGeocodeProvider",
                    ReverseGeocodeProvider.NONE,
                    ReverseGeocodeProvider::class,
                    false,
                    preferenceValueInConfiguration = "None"
                ),
                Parameter(
                    "ReverseGeocodeProvider",
                    "reverseGeocodeProvider",
                    ReverseGeocodeProvider.NONE,
                    ReverseGeocodeProvider::class,
                    false,
                    preferenceValueInConfiguration = "Nonsense"
                ),
                Parameter(
                    "Host",
                    "host",
                    "testHost",
                    String::class,
                    false
                ),
                Parameter(
                    "IgnoreInaccurateLocations",
                    "ignoreInaccurateLocations",
                    123,
                    Int::class,
                    false
                ),
                Parameter(
                    "IgnoreStaleLocations",
                    "ignoreStaleLocations",
                    456f,
                    Float::class,
                    false
                ),
                Parameter(
                    "Info",
                    "info",
                    true,
                    Boolean::class,
                    false
                ),
                Parameter(
                    "Keepalive",
                    "keepalive",
                    1500,
                    Int::class,
                    false
                ),
                Parameter(
                    "Keepalive",
                    "keepalive",
                    900,
                    Int::class,
                    false
                ),
                Parameter(
                    "Keepalive",
                    "keepalive",
                    899,
                    Int::class,
                    false,
                    preferenceValueExpected = 900
                ),
                Parameter(
                    "Keepalive",
                    "keepalive",
                    0,
                    Int::class,
                    false,
                    preferenceValueExpected = 900
                ),
                Parameter(
                    "Keepalive",
                    "keepalive",
                    -1,
                    Int::class,
                    false,
                    preferenceValueExpected = 900
                ),
                Parameter(
                    "LocatorDisplacement",
                    "locatorDisplacement",
                    1690,
                    Int::class,
                    false
                ),
                Parameter(
                    "LocatorInterval",
                    "locatorInterval",
                    1000,
                    Int::class,
                    false
                ),
                Parameter(
                    "Mode",
                    "mode",
                    ConnectionMode.HTTP,
                    ConnectionMode::class,
                    false,
                    preferenceValueInConfiguration = 3
                ),
                Parameter(
                    "Mode",
                    "mode",
                    ConnectionMode.MQTT,
                    ConnectionMode::class,
                    false,
                    preferenceValueInConfiguration = 0
                ),
                Parameter(
                    "Mode",
                    "mode",
                    ConnectionMode.MQTT,
                    ConnectionMode::class,
                    false,
                    preferenceValueInConfiguration = -1
                ),
                Parameter(
                    "Monitoring",
                    "monitoring",
                    MonitoringMode.SIGNIFICANT,
                    MonitoringMode::class,
                    false,
                    preferenceValueInConfiguration = 1
                ),
                Parameter(
                    "Monitoring",
                    "monitoring",
                    MonitoringMode.SIGNIFICANT,
                    MonitoringMode::class,
                    false,
                    preferenceValueInConfiguration = -5
                ),
                Parameter(
                    "Monitoring",
                    "monitoring",
                    MonitoringMode.QUIET,
                    MonitoringMode::class,
                    false,
                    preferenceValueInConfiguration = -1
                ),
                Parameter(
                    "MoveModeLocatorInterval",
                    "moveModeLocatorInterval",
                    1500,
                    Int::class,
                    false
                ),
                Parameter(
                    "MqttProtocolLevel",
                    "mqttProtocolLevel",
                    MqttProtocolLevel.MQTT_3_1,
                    MqttProtocolLevel::class,
                    false,
                    preferenceValueInConfiguration = 3
                ),
                Parameter(
                    "MqttProtocolLevel",
                    "mqttProtocolLevel",
                    MqttProtocolLevel.MQTT_3_1,
                    MqttProtocolLevel::class,
                    false,
                    preferenceValueInConfiguration = -5
                ),
                Parameter(
                    "NotificationEvents",
                    "notificationEvents",
                    true,
                    Boolean::class,
                    false
                ),
                Parameter(
                    "NotificationHigherPriority",
                    "notificationHigherPriority",
                    true,
                    Boolean::class,
                    false
                ),
                Parameter(
                    "NotificationLocation",
                    "notificationLocation",
                    true,
                    Boolean::class,
                    false
                ),
                Parameter(
                    "NotificationGeocoderErrors",
                    "notificationGeocoderErrors",
                    false,
                    Boolean::class,
                    false
                ),
                Parameter(
                    "OpencageApiKey",
                    "opencageApiKey",
                    "testOpencageAPIKey",
                    String::class,
                    false
                ),
                Parameter(
                    "OsmTileScaleFactor",
                    "osmTileScaleFactor",
                    1.3f,
                    Float::class,
                    false
                ),
                Parameter(
                    "Password",
                    "password",
                    "testPassword!\"£",
                    String::class,
                    false
                ),
                Parameter(
                    "PegLocatorFastestIntervalToInterval",
                    "pegLocatorFastestIntervalToInterval",
                    false,
                    Boolean::class,
                    false
                ),
                Parameter(
                    "Ping",
                    "ping",
                    400,
                    Int::class,
                    false
                ),
                Parameter(
                    "Port",
                    "port",
                    9999,
                    Int::class,
                    false
                ),
                Parameter(
                    "Port",
                    "port",
                    -50,
                    Int::class,
                    false,
                    preferenceValueInConfiguration = -50,
                    preferenceValueExpected = 1
                ),
                Parameter(
                    "Port",
                    "port",
                    65536,
                    Int::class,
                    false,
                    preferenceValueExpected = 65535,
                    preferenceValueInConfiguration = 65536
                ),
                Parameter(
                    "Port",
                    "port",
                    65535,
                    Int::class,
                    false
                ),
                Parameter(
                    "PubExtendedData",
                    "pubExtendedData",

                    true,
                    Boolean::class,
                    false
                ),
                Parameter(
                    "PubQos",
                    "pubQos",
                    MqttQos.ONE,
                    MqttQos::class,
                    false,
                    preferenceValueInConfiguration = 1
                ),
                Parameter(
                    "PubQos",
                    "pubQos",
                    MqttQos.ZERO,
                    MqttQos::class,
                    false,
                    preferenceValueInConfiguration = 0
                ),
                Parameter(
                    "PubQos",
                    "pubQos",
                    MqttQos.TWO,
                    MqttQos::class,
                    false,
                    preferenceValueInConfiguration = 2
                ),
                Parameter(
                    "PubQos",
                    "pubQos",
                    MqttQos.ONE,
                    MqttQos::class,
                    false,
                    preferenceValueInConfiguration = 5
                ),
                Parameter(
                    "PubRetain",
                    "pubRetain",
                    true,
                    Boolean::class,
                    false
                ),
                Parameter(
                    "PubTopicBase",
                    "pubTopicBase",
                    "testDeviceTopic",
                    String::class,
                    false
                ),
                Parameter(
                    "Cmd",
                    "cmd",
                    true,
                    Boolean::class,
                    false
                ),
                Parameter(
                    "RemoteConfiguration",
                    "remoteConfiguration",
                    true,
                    Boolean::class,
                    false
                ),
                Parameter(
                    "PublishLocationOnConnect",
                    "publishLocationOnConnect",
                    true,
                    Boolean::class,
                    false
                ),
                Parameter(
                    "Sub",
                    "sub",
                    true,
                    Boolean::class,
                    false
                ),
                Parameter(
                    "SubQos",
                    "subQos",
                    MqttQos.ONE,
                    MqttQos::class,
                    false,
                    preferenceValueInConfiguration = 1
                ),
                Parameter(
                    "SubTopic",
                    "subTopic",
                    "testSubTopic",
                    String::class,
                    false
                ),
                Parameter(
                    "Tls",
                    "tls",
                    true,
                    Boolean::class,
                    false
                ),
                Parameter(
                    "TlsCaCrt",
                    "tlsCaCrt",
                    "caCertName",
                    String::class,
                    false
                ),
                Parameter(
                    "TlsClientCrt",
                    "tlsClientCrt",
                    "clientCertName",
                    String::class,
                    false
                ),
                Parameter(
                    "TlsClientCrtPassword",
                    "tlsClientCrtPassword",
                    "clientCrtPassword",
                    String::class,
                    false
                ),
                Parameter(
                    "Tid",
                    "tid",
                    StringMaxTwoAlphaNumericChars("t1"),
                    StringMaxTwoAlphaNumericChars::class,
                    false,
                    preferenceValueInConfiguration = "t1"
                ),
                Parameter(
                    "Tid",
                    "tid",
                    StringMaxTwoAlphaNumericChars("trackerId"),
                    StringMaxTwoAlphaNumericChars::class,
                    false,
                    preferenceValueExpected = StringMaxTwoAlphaNumericChars("tr"),
                    preferenceValueInConfiguration = "trackerId"
                ),
                Parameter(
                    "Url",
                    "url",
                    "https://www.example.com",
                    String::class,
                    true
                ),
                Parameter(
                    "Username",
                    "username",
                    "testUser",
                    String::class,
                    false
                ),
                Parameter(
                    "Ws",
                    "ws",
                    true,
                    Boolean::class,
                    false
                )
            ).toList()
        }
    }
}
