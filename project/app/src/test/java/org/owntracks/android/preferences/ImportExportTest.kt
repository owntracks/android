package org.owntracks.android.preferences

import android.content.Context
import com.fasterxml.jackson.databind.ObjectMapper
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.mock
import org.owntracks.android.model.messages.MessageConfiguration
import org.owntracks.android.preferences.types.ConnectionMode
import org.owntracks.android.preferences.types.MonitoringMode
import org.owntracks.android.preferences.types.MqttProtocolLevel
import org.owntracks.android.preferences.types.MqttQos
import org.owntracks.android.support.Parser
import org.owntracks.android.support.SimpleIdlingResource

class ImportExportTest {
    private lateinit var mockContext: Context
    private lateinit var preferencesStore: PreferencesStore
    private val mockIdlingResource = SimpleIdlingResource("mock", true)

    @Before
    fun createMocks() {
        mockContext = mock { }
        preferencesStore = InMemoryPreferencesStore()
    }

    @Test
    fun `given a JSON configuration message, when importing it, then the preferences values are set to the same values`() {
        //language=JSON
        val input = """
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
        """.trimIndent()
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
            assertEquals(ConnectionMode.MQTT, mode)
            assertEquals(MonitoringMode.SIGNIFICANT, monitoring)
            assertEquals(10, moveModeLocatorInterval)
            assertEquals(MqttProtocolLevel.MQTT_3_1, mqttProtocolLevel)
            assert(!notificationHigherPriority)
            assert(notificationLocation)
            assertEquals("testkey", opencageApiKey)
            assertEquals("testpassword", password)
            assertEquals(30, ping)
            assertEquals(1883, port)
            assert(extendedData)
            assertEquals(MqttQos.ONE, pubQos)
            assert(pubRetain)
            assertEquals("owntracks/%u/%d", pubTopicBase)
            assert(remoteConfiguration)
            assert(sub)
            assertEquals(MqttQos.TWO, subQos)
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
            mode = ConnectionMode.MQTT
            monitoring = MonitoringMode.SIGNIFICANT
            moveModeLocatorInterval = 10
            mqttProtocolLevel = MqttProtocolLevel.MQTT_3_1
            notificationHigherPriority = false
            notificationLocation = true
            opencageApiKey = "testkey"
            password = "testpassword"
            ping = 30
            port = 1883
            extendedData = true
            pubQos = MqttQos.ONE
            pubRetain = true
            pubTopicBase = "owntracks/%u/%d"
            remoteConfiguration = true
            sub = true
            subQos = MqttQos.TWO
            subTopic = "owntracks/+/+"
            tls = false
            username = "testusername"
            ws = false
        }
        val message = preferences.exportToMessage()
        val parser = Parser(null)
        val json = parser.toUnencryptedJsonPretty(message)

        val jsonNode = ObjectMapper().readTree(json)
        assertEquals("configuration", jsonNode.get("_type").asText())
        assertTrue(jsonNode.has("id"))
        assertEquals(0, jsonNode.get("waypoints").size())
        assertTrue(jsonNode.get("autostartOnBoot").asBoolean())
        assertFalse(jsonNode.get("cleanSession").asBoolean())
        assertEquals("emulator", jsonNode.get("clientId").asText())
        assertTrue(jsonNode.get("cmd").asBoolean())
        assertEquals(30,jsonNode.get("connectionTimeoutSeconds").asInt())
        assertTrue(jsonNode.get("debugLog").asBoolean())
        assertEquals("testdevice", jsonNode.get("deviceId").asText())
        assertTrue(jsonNode.get("enableMapRotation").asBoolean())
        assertEquals("", jsonNode.get("encryptionKey").asText())
        assertEquals(0, jsonNode.get("experimentalFeatures").size())
        assertTrue(jsonNode.get("fusedRegionDetection").asBoolean())
        assertEquals("127.0.0.1", jsonNode.get("host").asText())
        assertEquals(150, jsonNode.get("ignoreInaccurateLocations").asInt())
        assertEquals(0.0, jsonNode.get("ignoreStaleLocations").asDouble(), 0.000001)
        assertTrue(jsonNode.get("info").asBoolean())
        assertEquals(900, jsonNode.get("keepalive").asInt())
        assertEquals(5, jsonNode.get("locatorDisplacement").asInt())
        assertEquals(60, jsonNode.get("locatorInterval").asInt())
        assertEquals(defaultMapLayerStyle.name, jsonNode.get("mapLayerStyle").asText())
        assertEquals(0, jsonNode.get("mode").asInt())
        assertEquals(1, jsonNode.get("monitoring").asInt())
        assertEquals(10, jsonNode.get("moveModeLocatorInterval").asInt())
        assertEquals(3, jsonNode.get("mqttProtocolLevel").asInt())
        assertTrue(jsonNode.get("notificationEvents").asBoolean())
        assertTrue(jsonNode.get("notificationGeocoderErrors").asBoolean())
        assertFalse(jsonNode.get("notificationHigherPriority").asBoolean())
        assertTrue(jsonNode.get("notificationLocation").asBoolean())
        assertEquals("testkey", jsonNode.get("opencageApiKey").asText())
        assertEquals(1.0, jsonNode.get("osmTileScaleFactor").asDouble(), 0.000001)
        assertEquals("testpassword", jsonNode.get("password").asText())
        assertFalse(jsonNode.get("pegLocatorFastestIntervalToInterval").asBoolean())
        assertEquals(30, jsonNode.get("ping").asInt())
        assertEquals(1883, jsonNode.get("port").asInt())
        assertTrue(jsonNode.get("extendedData").asBoolean())
        assertEquals(1, jsonNode.get("pubQos").asInt())
        assertTrue(jsonNode.get("pubRetain").asBoolean())
        assertEquals("owntracks/%u/%d", jsonNode.get("pubTopicBase").asText())
        assertFalse(jsonNode.get("publishLocationOnConnect").asBoolean())
        assertTrue(jsonNode.get("remoteConfiguration").asBoolean())
        assertEquals(defaultReverseGeocodeProvider.value, jsonNode.get("reverseGeocodeProvider").asText())
        assertFalse(jsonNode.get("showRegionsOnMap").asBoolean())
        assertTrue(jsonNode.get("sub").asBoolean())
        assertEquals(2, jsonNode.get("subQos").asInt())
        assertEquals("owntracks/+/+", jsonNode.get("subTopic").asText())
        assertEquals(2, jsonNode.get("theme").asInt())
        assertEquals("wn", jsonNode.get("tid").asText())
        assertFalse(jsonNode.get("tls").asBoolean())
        assertEquals("", jsonNode.get("tlsClientCrt").asText())
        assertEquals("testusername", jsonNode.get("username").asText())
        assertFalse(jsonNode.get("ws").asBoolean())
    }
}
