package org.owntracks.android.preferences

import android.content.Context
import android.content.pm.ShortcutManager
import kotlin.reflect.KClass
import kotlin.reflect.KMutableProperty1
import kotlin.reflect.full.declaredMemberProperties
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
    Preferences::class
        .declaredMemberProperties
        .filterIsInstance<KMutableProperty1<Preferences, Any>>()
        .first { it.name == parameter.preferenceName }
        .set(preferences, parameter.preferenceValue)
    if (parameter.httpOnlyMode) {
      preferences.mode = ConnectionMode.HTTP
    }
    val messageConfiguration = preferences.exportToMessage()
    assertEquals(parameter.preferenceValueExpected, messageConfiguration[parameter.preferenceName])
  }

  @Test
  fun `when importing a configuration ensure that the supplied preference is set to the given value`() {
    val preferences = Preferences(preferencesStore, mockIdlingResource)
    val messageConfiguration = MessageConfiguration()
    messageConfiguration[parameter.preferenceName] = parameter.preferenceValueInConfiguration
    preferences.importConfiguration(messageConfiguration)
    val answer =
        Preferences::class
            .declaredMemberProperties
            .first { it.name == parameter.preferenceName }
            .get(preferences)
    assertEquals(parameter.preferenceValueExpected, answer)
  }

  data class Parameter(
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
              Parameter("autostartOnBoot", true, Boolean::class, false),
              Parameter("cleanSession", true, Boolean::class, false),
              Parameter("clientId", "testClientId", String::class, false),
              Parameter("connectionTimeoutSeconds", 20, Int::class, false),
              Parameter(
                  "connectionTimeoutSeconds", -5, Int::class, false, preferenceValueExpected = 1),
              Parameter("debugLog", true, Boolean::class, false),
              Parameter("deviceId", "deviceId", String::class, false),
              Parameter("dontReuseHttpClient", true, Boolean::class, true),
              Parameter("enableMapRotation", true, Boolean::class, false),
              Parameter("experimentalFeatures", setOf("this", "that", "other"), Set::class, false),
              Parameter("fusedRegionDetection", true, Boolean::class, false),
              Parameter(
                  "reverseGeocodeProvider",
                  ReverseGeocodeProvider.DEVICE,
                  ReverseGeocodeProvider::class,
                  false,
                  preferenceValueInConfiguration = "Device"),
              Parameter(
                  "reverseGeocodeProvider",
                  ReverseGeocodeProvider.OPENCAGE,
                  ReverseGeocodeProvider::class,
                  false,
                  preferenceValueInConfiguration = "OpenCage"),
              Parameter(
                  "reverseGeocodeProvider",
                  ReverseGeocodeProvider.NONE,
                  ReverseGeocodeProvider::class,
                  false,
                  preferenceValueInConfiguration = "None"),
              Parameter(
                  "reverseGeocodeProvider",
                  ReverseGeocodeProvider.NONE,
                  ReverseGeocodeProvider::class,
                  false,
                  preferenceValueInConfiguration = "Nonsense"),
              Parameter("host", "testHost", String::class, false),
              Parameter("ignoreInaccurateLocations", 123, Int::class, false),
              Parameter("ignoreStaleLocations", 456f, Float::class, false),
              Parameter("info", true, Boolean::class, false),
              Parameter("keepalive", 1500, Int::class, false),
              Parameter("keepalive", 900, Int::class, false),
              Parameter("keepalive", 899, Int::class, false, preferenceValueExpected = 899),
              Parameter("keepalive", 0, Int::class, false, preferenceValueExpected = 0),
              Parameter("keepalive", -1, Int::class, false, preferenceValueExpected = 0),
              Parameter("locatorDisplacement", 1690, Int::class, false),
              Parameter("locatorInterval", 1000, Int::class, false),
              Parameter(
                  "mode",
                  ConnectionMode.HTTP,
                  ConnectionMode::class,
                  false,
                  preferenceValueInConfiguration = 3),
              Parameter(
                  "mode",
                  ConnectionMode.MQTT,
                  ConnectionMode::class,
                  false,
                  preferenceValueInConfiguration = 0),
              Parameter(
                  "mode",
                  ConnectionMode.MQTT,
                  ConnectionMode::class,
                  false,
                  preferenceValueInConfiguration = -1),
              Parameter(
                  "monitoring",
                  MonitoringMode.SIGNIFICANT,
                  MonitoringMode::class,
                  false,
                  preferenceValueInConfiguration = 1),
              Parameter(
                  "monitoring",
                  MonitoringMode.SIGNIFICANT,
                  MonitoringMode::class,
                  false,
                  preferenceValueInConfiguration = -5),
              Parameter(
                  "monitoring",
                  MonitoringMode.QUIET,
                  MonitoringMode::class,
                  false,
                  preferenceValueInConfiguration = -1),
              Parameter("moveModeLocatorInterval", 1500, Int::class, false),
              Parameter(
                  "mqttProtocolLevel",
                  MqttProtocolLevel.MQTT_3_1,
                  MqttProtocolLevel::class,
                  false,
                  preferenceValueInConfiguration = 3),
              Parameter(
                  "mqttProtocolLevel",
                  MqttProtocolLevel.MQTT_3_1,
                  MqttProtocolLevel::class,
                  false,
                  preferenceValueInConfiguration = -5),
              Parameter("notificationEvents", true, Boolean::class, false),
              Parameter("notificationHigherPriority", true, Boolean::class, false),
              Parameter("notificationLocation", true, Boolean::class, false),
              Parameter("notificationGeocoderErrors", false, Boolean::class, false),
              Parameter("opencageApiKey", "testOpencageAPIKey", String::class, false),
              Parameter("osmTileScaleFactor", 1.3f, Float::class, false),
              Parameter("password", "testPassword!\"£", String::class, false),
              Parameter("pegLocatorFastestIntervalToInterval", false, Boolean::class, false),
              Parameter("ping", 400, Int::class, false),
              Parameter("port", 9999, Int::class, false),
              Parameter(
                  "port",
                  -50,
                  Int::class,
                  false,
                  preferenceValueInConfiguration = -50,
                  preferenceValueExpected = 1),
              Parameter(
                  "port",
                  65536,
                  Int::class,
                  false,
                  preferenceValueExpected = 65535,
                  preferenceValueInConfiguration = 65536),
              Parameter("port", 65535, Int::class, false),
              Parameter("extendedData", true, Boolean::class, false),
              Parameter(
                  "pubQos", MqttQos.ONE, MqttQos::class, false, preferenceValueInConfiguration = 1),
              Parameter(
                  "pubQos",
                  MqttQos.ZERO,
                  MqttQos::class,
                  false,
                  preferenceValueInConfiguration = 0),
              Parameter(
                  "pubQos", MqttQos.TWO, MqttQos::class, false, preferenceValueInConfiguration = 2),
              Parameter(
                  "pubQos", MqttQos.ONE, MqttQos::class, false, preferenceValueInConfiguration = 5),
              Parameter("pubRetain", true, Boolean::class, false),
              Parameter("pubTopicBase", "testDeviceTopic", String::class, false),
              Parameter("cmd", true, Boolean::class, false),
              Parameter("remoteConfiguration", true, Boolean::class, false),
              Parameter("publishLocationOnConnect", true, Boolean::class, false),
              Parameter("sub", true, Boolean::class, false),
              Parameter(
                  "subQos", MqttQos.ONE, MqttQos::class, false, preferenceValueInConfiguration = 1),
              Parameter("subTopic", "testSubTopic", String::class, false),
              Parameter("tls", true, Boolean::class, false),
              Parameter("tlsClientCrt", "clientCertName", String::class, false),
              Parameter(
                  "tid",
                  StringMaxTwoAlphaNumericChars("t1"),
                  String::class,
                  false,
                  preferenceValueExpected = StringMaxTwoAlphaNumericChars("t1"),
                  preferenceValueInConfiguration = "t1"),
              Parameter(
                  "tid",
                  StringMaxTwoAlphaNumericChars("trackerId"),
                  String::class,
                  false,
                  preferenceValueExpected = StringMaxTwoAlphaNumericChars("tr"),
                  preferenceValueInConfiguration = "trackerId"),
              Parameter("url", "https://www.example.com", String::class, true),
              Parameter("username", "testUser", String::class, false),
              Parameter("ws", true, Boolean::class, false))
          .toList()
    }
  }
}
