package org.owntracks.android.preferences

import android.content.Context
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.mock
import org.owntracks.android.BuildConfig
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
              "pubExtendedData": true,
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
            assert(pubExtendedData)
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
            pubExtendedData = true
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

        //language=JSON
        val expected = """
            {
              "_type" : "configuration",
              "waypoints" : [ ],
              "_build" : ${BuildConfig.VERSION_CODE},
              "autostartOnBoot" : true,
              "cleanSession" : false,
              "clientId" : "emulator",
              "cmd" : true,
              "connectionTimeoutSeconds" : 30,
              "debugLog" : true,
              "deviceId" : "testdevice",
              "enableMapRotation" : true,
              "encryptionKey" : "",
              "experimentalFeatures" : [ ],
              "fusedRegionDetection" : true,
              "host" : "127.0.0.1",
              "ignoreInaccurateLocations" : 150,
              "ignoreStaleLocations" : 0.0,
              "info" : true,
              "keepalive" : 900,
              "locatorDisplacement" : 5,
              "locatorInterval" : 60,
              "mapLayerStyle" : "${defaultMapLayerStyle.name}",
              "mode" : 0,
              "monitoring" : 1,
              "moveModeLocatorInterval" : 10,
              "mqttProtocolLevel" : 3,
              "notificationEvents" : true,
              "notificationGeocoderErrors" : true,
              "notificationHigherPriority" : false,
              "notificationLocation" : true,
              "opencageApiKey" : "testkey",
              "osmTileScaleFactor" : 1.0,
              "password" : "testpassword",
              "pegLocatorFastestIntervalToInterval" : false,
              "ping" : 30,
              "port" : 1883,
              "pubExtendedData" : true,
              "pubQos" : 1,
              "pubRetain" : true,
              "pubTopicBase" : "owntracks/%u/%d",
              "publishLocationOnConnect" : false,
              "remoteConfiguration" : true,
              "reverseGeocodeProvider" : "${defaultReverseGeocodeProvider.value}",
              "showRegionsOnMap" : false,
              "sub" : true,
              "subQos" : 2,
              "subTopic" : "owntracks/+/+",
              "theme" : 2,
              "tid" : "wn",
              "tls" : false,
              "tlsClientCrt" : "",
              "tlsClientCrtPassword" : "",
              "username" : "testusername",
              "ws" : false
            }
        """.trimIndent()
        assertEquals(expected, json)
    }
}
