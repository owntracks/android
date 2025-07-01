package org.owntracks.android.preferences

import android.content.Context
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.boolean
import kotlinx.serialization.json.double
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.mock
import org.owntracks.android.location.LocatorPriority
import org.owntracks.android.model.Parser
import org.owntracks.android.model.messages.MessageConfiguration
import org.owntracks.android.preferences.types.ConnectionMode
import org.owntracks.android.preferences.types.MonitoringMode
import org.owntracks.android.preferences.types.MqttProtocolLevel
import org.owntracks.android.preferences.types.MqttQos
import org.owntracks.android.test.SimpleIdlingResource

class ImportExportTest {
  private lateinit var mockContext: Context
  private lateinit var preferencesStore: PreferencesStore
  private val mockIdlingResource = SimpleIdlingResource("mock", true)

  @Before
  fun createMocks() {
    mockContext = mock {}
    preferencesStore = InMemoryPreferencesStore()
  }

  @Test
  fun `given a JSON configuration message, when importing it, then the preferences values are set to the same values`() {
    // language=JSON
    val input =
        """
            {
              "_type": "configuration",
              "waypoints": [
                {
                  "_type": "waypoint",
                  "desc": "work",
                  "lat": 51.504778900000005,
                  "lon": -0.023851299999999995,
                  "rad": 150,
                  "tst": 1505910709000
                },
                {
                  "_type": "waypoint",
                  "desc": "home",
                  "lat": 53.6776261,
                  "lon": -1.58268,
                  "rad": 100,
                  "tst": 1558351273
                }
              ],
              "auth": true,
              "autostartOnBoot": true,
              "cleanSession": false,
              "clientId": "emulator",
              "cmd": true,
              "debugLog": true,
              "deviceId": "testdevice",
              "fusedRegionDetection": true,
              "geocodeEnabled": true,
              "host": "127.0.0.1",
              "ignoreInaccurateLocations": 150,
              "ignoreStaleLocations": 0.0,
              "keepalive": 900,
              "locatorDisplacement": 5,
              "locatorInterval": 60,
              "locatorPriority": "LowPower",
              "mode": 0,
              "monitoring": 1,
              "moveModeLocatorInterval": 10,
              "mqttProtocolLevel": 3,
              "notificationHigherPriority": false,
              "notificationLocation": true,
              "opencageApiKey": "testkey",
              "password": "testpassword",
              "ping": 30,
              "port": 1883,
              "extendedData": true,
              "pubQos": 1,
              "pubRetain": true,
              "pubTopicBase": "owntracks/%u/%d",
              "remoteConfiguration": true,
              "sub": true,
              "subQos": 2,
              "subTopic": "owntracks/+/+",
              "tls": false,
              "usePassword": true,
              "username": "testusername",
              "ws": false
            }
        """
            .trimIndent()
    val preferences = Preferences(preferencesStore, mockIdlingResource)
    val parser = Parser(null)
    preferences.importConfiguration(parser.fromJson(input) as MessageConfiguration)
    preferences.run {
      assert(autostartOnBoot)
      assert(!cleanSession)
      assertEquals("emulator", clientId)
      assert(cmd)
      assert(debugLog)
      assertEquals("testdevice", deviceId)
      assert(fusedRegionDetection)
      assertEquals("127.0.0.1", host)
      assertEquals(150, ignoreInaccurateLocations)
      assertEquals(0.0f, ignoreStaleLocations)
      assertEquals(900, keepalive)
      assertEquals(5, locatorDisplacement)
      assertEquals(60, locatorInterval)
      assertEquals(LocatorPriority.LowPower, locatorPriority)
      assertEquals(ConnectionMode.MQTT, mode)
      assertEquals(MonitoringMode.Significant, monitoring)
      assertEquals(10, moveModeLocatorInterval)
      assertEquals(MqttProtocolLevel.MQTT_3_1, mqttProtocolLevel)
      assert(!notificationHigherPriority)
      assert(notificationLocation)
      assertEquals("testkey", opencageApiKey)
      assertEquals("testpassword", password)
      assertEquals(30, ping)
      assertEquals(1883, port)
      assert(extendedData)
      assertEquals(MqttQos.One, pubQos)
      assert(pubRetain)
      assertEquals("owntracks/%u/%d", pubTopicBase)
      assert(remoteConfiguration)
      assert(sub)
      assertEquals(MqttQos.Two, subQos)
      assertEquals("owntracks/+/+", subTopic)
      assert(!tls)
      assertEquals("testusername", username)
      assert(!ws)
    }
  }

  @Test
  fun `given a preferences instance, when exporting it, then the exported JSON message has the same values set`() {
    val preferences = Preferences(preferencesStore, mockIdlingResource)
    preferences.run {
      autostartOnBoot = true
      cleanSession = false
      clientId = "emulator"
      cmd = true
      debugLog = true
      deviceId = "testdevice"
      fusedRegionDetection = true
      host = "127.0.0.1"
      ignoreInaccurateLocations = 150
      ignoreStaleLocations = 0.0f
      keepalive = 900
      locatorDisplacement = 5
      locatorInterval = 60
      locatorPriority = null
      mode = ConnectionMode.MQTT
      monitoring = MonitoringMode.Significant
      moveModeLocatorInterval = 10
      mqttProtocolLevel = MqttProtocolLevel.MQTT_3_1
      notificationHigherPriority = false
      notificationLocation = true
      opencageApiKey = "testkey"
      password = "testpassword"
      ping = 30
      port = 1883
      extendedData = true
      pubQos = MqttQos.One
      pubRetain = true
      pubTopicBase = "owntracks/%u/%d"
      remoteConfiguration = true
      sub = true
      subQos = MqttQos.Two
      subTopic = "owntracks/+/+"
      tls = false
      username = "testusername"
      ws = false
    }
    val message = preferences.exportToMessage()
    val parser = Parser(null)
    val json = parser.toUnencryptedJsonPretty(message)

    val jsonNode = Json.parseToJsonElement(json).jsonObject
    assertEquals("configuration", jsonNode["_type"]?.jsonPrimitive?.content)
    assertTrue(jsonNode.containsKey("_id"))
    assertEquals(0, jsonNode["waypoints"]?.jsonArray?.size)
    assertTrue(jsonNode["autostartOnBoot"]?.jsonPrimitive?.boolean == true)
    assertFalse(jsonNode["cleanSession"]?.jsonPrimitive?.boolean == true)
    assertEquals("emulator", jsonNode["clientId"]?.jsonPrimitive?.content)
    assertTrue(jsonNode["cmd"]?.jsonPrimitive?.boolean == true)
    assertEquals(30, jsonNode["connectionTimeoutSeconds"]?.jsonPrimitive?.int)
    assertTrue(jsonNode["debugLog"]?.jsonPrimitive?.boolean == true)
    assertEquals("testdevice", jsonNode["deviceId"]?.jsonPrimitive?.content)
    assertTrue(jsonNode["enableMapRotation"]?.jsonPrimitive?.boolean == true)
    assertEquals("", jsonNode["encryptionKey"]?.jsonPrimitive?.content)
    assertEquals(0, jsonNode["experimentalFeatures"]?.jsonArray?.size)
    assertTrue(jsonNode["fusedRegionDetection"]?.jsonPrimitive?.boolean == true)
    assertEquals("127.0.0.1", jsonNode["host"]?.jsonPrimitive?.content)
    assertEquals(150, jsonNode["ignoreInaccurateLocations"]?.jsonPrimitive?.int)
    assertEquals(0.0, jsonNode["ignoreStaleLocations"]?.jsonPrimitive?.double, 0.000001)
    assertTrue(jsonNode["info"]?.jsonPrimitive?.boolean == true)
    assertEquals(900, jsonNode["keepalive"]?.jsonPrimitive?.int)
    assertEquals(5, jsonNode["locatorDisplacement"]?.jsonPrimitive?.int)
    assertEquals(60, jsonNode["locatorInterval"]?.jsonPrimitive?.int)
    assertFalse(jsonNode.containsKey("locatorPriority"))
    assertEquals(defaultMapLayerStyle.name, jsonNode["mapLayerStyle"]?.jsonPrimitive?.content)
    assertEquals(0, jsonNode["mode"]?.jsonPrimitive?.int)
    assertEquals(1, jsonNode["monitoring"]?.jsonPrimitive?.int)
    assertEquals(10, jsonNode["moveModeLocatorInterval"]?.jsonPrimitive?.int)
    assertEquals(3, jsonNode["mqttProtocolLevel"]?.jsonPrimitive?.int)
    assertTrue(jsonNode["notificationEvents"]?.jsonPrimitive?.boolean == true)
    assertTrue(jsonNode["notificationGeocoderErrors"]?.jsonPrimitive?.boolean == true)
    assertFalse(jsonNode["notificationHigherPriority"]?.jsonPrimitive?.boolean == true)
    assertTrue(jsonNode["notificationLocation"]?.jsonPrimitive?.boolean == true)
    assertEquals("testkey", jsonNode["opencageApiKey"]?.jsonPrimitive?.content)
    assertEquals(1.0, jsonNode["osmTileScaleFactor"]?.jsonPrimitive?.double, 0.000001)
    assertEquals("testpassword", jsonNode["password"]?.jsonPrimitive?.content)
    assertFalse(jsonNode["pegLocatorFastestIntervalToInterval"]?.jsonPrimitive?.boolean == true)
    assertEquals(30, jsonNode["ping"]?.jsonPrimitive?.int)
    assertEquals(1883, jsonNode["port"]?.jsonPrimitive?.int)
    assertTrue(jsonNode["extendedData"]?.jsonPrimitive?.boolean == true)
    assertEquals(1, jsonNode["pubQos"]?.jsonPrimitive?.int)
    assertTrue(jsonNode["pubRetain"]?.jsonPrimitive?.boolean == true)
    assertEquals("owntracks/%u/%d", jsonNode["pubTopicBase"]?.jsonPrimitive?.content)
    assertFalse(jsonNode["publishLocationOnConnect"]?.jsonPrimitive?.boolean == true)
    assertTrue(jsonNode["remoteConfiguration"]?.jsonPrimitive?.boolean == true)
    assertEquals(
        defaultReverseGeocodeProvider.name,
        jsonNode["reverseGeocodeProvider"]?.jsonPrimitive?.content)
    assertFalse(jsonNode["showRegionsOnMap"]?.jsonPrimitive?.boolean == true)
    assertTrue(jsonNode["sub"]?.jsonPrimitive?.boolean == true)
    assertEquals(2, jsonNode["subQos"]?.jsonPrimitive?.int)
    assertEquals("owntracks/+/+", jsonNode["subTopic"]?.jsonPrimitive?.content)
    assertEquals("Auto", jsonNode["theme"]?.jsonPrimitive?.content)
    assertEquals("wn", jsonNode["tid"]?.jsonPrimitive?.content)
    assertFalse(jsonNode["tls"]?.jsonPrimitive?.boolean == true)
    assertEquals("", jsonNode["tlsClientCrt"]?.jsonPrimitive?.content)
    assertEquals("testusername", jsonNode["username"]?.jsonPrimitive?.content)
    assertFalse(jsonNode["ws"]?.jsonPrimitive?.boolean == true)
  }

  @Test
  fun `given a preferences instance with locatorPriority set, when exporting it, then the exported JSON message has a value for locatorPriority`() {
    val preferences = Preferences(preferencesStore, mockIdlingResource)
    preferences.run { locatorPriority = LocatorPriority.HighAccuracy }
    val message = preferences.exportToMessage()
    val parser = Parser(null)
    val json = parser.toUnencryptedJsonPretty(message)

    val jsonNode = Json.parseToJsonElement(json).jsonObject
    assertTrue(jsonNode.containsKey("locatorPriority"))
    assertEquals(0, jsonNode["locatorPriority"]?.jsonPrimitive?.int)
  }
}
