package org.owntracks.android.support

import com.fasterxml.jackson.core.JsonParseException
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.exc.InvalidFormatException
import org.intellij.lang.annotations.Language
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.mockito.ArgumentMatchers.anyString
import org.mockito.Mock
import org.mockito.Mockito.`when`
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.owntracks.android.location.geofencing.Geofence
import org.owntracks.android.model.BatteryStatus
import org.owntracks.android.model.CommandAction
import org.owntracks.android.model.messages.*
import org.owntracks.android.support.Parser.EncryptionException
import java.io.ByteArrayInputStream
import java.io.IOException
import java.util.*

class ParserTest {
    private lateinit var extendedMessageLocation: MessageLocation
    private lateinit var messageLocation: MessageLocation

    @Mock
    private lateinit var testPreferences: Preferences

    // Need to mock this as we can't unit test native lib off-device
    @Mock
    private lateinit var encryptionProvider: EncryptionProvider

    // Going to need a way of getting JSON strings into trees
    private val objectMapper = ObjectMapper()

    @Before
    fun setupMessageLocation() {
        val regions: MutableList<String> = LinkedList()
        regions.add("Testregion1")
        regions.add("Testregion2")
        messageLocation = MessageLocation(MessageCreatedAtNow(FakeClock())).apply {
            accuracy = 10
            altitude = 20
            latitude = 50.1
            longitude = 60.2
            timestamp = 123456789
            velocity = 5.6.toInt()
            verticalAccuracy = 1.7.toInt()
            inregions = regions
        }

        extendedMessageLocation = messageLocation.apply {
            battery = 30
            batteryStatus = BatteryStatus.CHARGING
            bssid = "12:34:56:78"
            conn = "TestConn"
            monitoringMode = MonitoringMode.SIGNIFICANT
            ssid = "Wifi SSID"
        }
    }

    @Before
    fun setupEncryptionProvider() {
        testPreferences = mock {
            on { encryptionKey } doReturn "testEncryptionKey"
            on { pubTopicLocations } doReturn "owntracks/testUsername/testDevice"
        }
        encryptionProvider = mock { on { isPayloadEncryptionEnabled } doReturn false }
    }

    @Test
    fun `Parser can serialize extended location message to a pretty JSON message`() {
        val parser = Parser(null)
        @Language("JSON") val expected = """
            {
              "_type" : "location",
              "BSSID" : "12:34:56:78",
              "SSID" : "Wifi SSID",
              "acc" : 10,
              "alt" : 20,
              "batt" : 30,
              "bs" : 2,
              "conn" : "TestConn",
              "created_at" : 25,
              "inregions" : [ "Testregion1", "Testregion2" ],
              "lat" : 50.1,
              "lon" : 60.2,
              "m" : 1,
              "tst" : 123456789,
              "vac" : 1,
              "vel" : 5
            }
        """.trimIndent()
        assertEquals(expected, parser.toUnencryptedJsonPretty(extendedMessageLocation))
    }

    fun `Parser can serialize non-extended location message to a pretty JSON message`() {
        val parser = Parser(null)
        @Language("JSON") val expected = """
            {
              "_type" : "location",
              "acc" : 10,
              "alt" : 20,
              "batt" : 30,
              "bs" : 2,
              "created_at" : 25,
              "inregions" : [ "Testregion1", "Testregion2" ],
              "lat" : 50.1,
              "lon" : 60.2,
              "tst" : 123456789,
              "vac" : 1,
              "vel" : 5
            }
        """.trimIndent()
        assertEquals(expected, parser.toUnencryptedJsonPretty(messageLocation))
    }

    @Test
    fun `Parser can deserialize a location message`() {
        `when`(encryptionProvider.isPayloadEncryptionEnabled).thenReturn(false)
        val parser = Parser(encryptionProvider)
        @Language("JSON") val input = """
            {
                "_type": "location",
                "tid": "s5",
                "acc": 1600,
                "alt": 0.0,
                "batt": 99,
                "bs": 3,
                "conn": "w",
                "lat": 52.3153748,
                "lon": 5.0408462,
                "t": "p",
                "tst": 1514455575,
                "vac": 0,
                "inregions":
                [
                    "Testregion1",
                    "Testregion2"
                ]
            }
        """.trimIndent()
        val messageBase = parser.fromJson(input)
        assertEquals(MessageLocation::class.java, messageBase.javaClass)
        val message = messageBase as MessageLocation
        assertEquals(1514455575L, message.timestamp)
        assertEquals("s5", message.trackerId)
        assertEquals(1600, message.accuracy.toLong())
        assertEquals(0.0, message.altitude.toDouble(), 0.0)
        assertEquals(99, message.battery.toLong())
        assertEquals(BatteryStatus.FULL, message.batteryStatus)
        assertEquals("w", message.conn)
        assertEquals(52.3153748, message.latitude, 0.0)
        assertEquals(5.0408462, message.longitude, 0.0)
        assertEquals("p", message.trigger)
        assertEquals(0f, message.verticalAccuracy.toFloat(), 0f)
        assertEquals(2, message.inregions?.size)
    }

    @Test
    fun `Parser can serialize a location message`() {
        `when`(encryptionProvider.isPayloadEncryptionEnabled).thenReturn(false)
        val parser = Parser(encryptionProvider)
        val input = extendedMessageLocation
        val serialized = input.toJson(parser)
        val jsonNode = objectMapper.readTree(serialized)
        assertTrue(jsonNode.isObject)
        assertEquals("location", jsonNode.get("_type").asText())
        assertEquals("12:34:56:78", jsonNode.get("BSSID").asText())
        assertEquals("Wifi SSID", jsonNode.get("SSID").asText())
        assertEquals(10, jsonNode.get("acc").asInt())
        assertEquals(20, jsonNode.get("alt").asInt())
        assertEquals(30, jsonNode.get("batt").asInt())
        assertEquals(2, jsonNode.get("bs").asInt())
        assertEquals("TestConn", jsonNode.get("conn").asText())
        assertEquals(25, jsonNode.get("created_at").asInt())
        assertTrue(jsonNode.get("inregions").isArray)
        assertEquals(2, jsonNode.get("inregions").count())
        assertEquals("Testregion1", jsonNode.get("inregions").get(0).asText())
        assertEquals("Testregion2", jsonNode.get("inregions").get(1).asText())
        assertEquals(50.1, jsonNode.get("lat").asDouble(), 0.0001)
        assertEquals(60.2, jsonNode.get("lon").asDouble(), 0.0001)
        assertEquals(1, jsonNode.get("m").asInt())
        assertEquals(123456789, jsonNode.get("tst").asLong())
        assertEquals(1, jsonNode.get("vac").asInt())
        assertEquals(5, jsonNode.get("vel").asInt())
    }

    @Test
    fun `Parser can serialize a location message with the topic visible`() {
        `when`(encryptionProvider.isPayloadEncryptionEnabled).thenReturn(false)
        val parser = Parser(encryptionProvider)
        val input = extendedMessageLocation
        input.addMqttPreferences(testPreferences)
        input.setTopicVisible()
        val serialized = input.toJson(parser)
        val jsonNode = objectMapper.readTree(serialized)
        assertTrue(jsonNode.isObject)
        assertEquals("location", jsonNode.get("_type").asText())
        assertEquals("12:34:56:78", jsonNode.get("BSSID").asText())
        assertEquals("Wifi SSID", jsonNode.get("SSID").asText())
        assertEquals(10, jsonNode.get("acc").asInt())
        assertEquals(20, jsonNode.get("alt").asInt())
        assertEquals(30, jsonNode.get("batt").asInt())
        assertEquals(2, jsonNode.get("bs").asInt())
        assertEquals("TestConn", jsonNode.get("conn").asText())
        assertEquals(25, jsonNode.get("created_at").asInt())
        assertTrue(jsonNode.get("inregions").isArray)
        assertEquals(2, jsonNode.get("inregions").count())
        assertEquals("Testregion1", jsonNode.get("inregions").get(0).asText())
        assertEquals("Testregion2", jsonNode.get("inregions").get(1).asText())
        assertEquals(50.1, jsonNode.get("lat").asDouble(), 0.0001)
        assertEquals(60.2, jsonNode.get("lon").asDouble(), 0.0001)
        assertEquals(1, jsonNode.get("m").asInt())
        assertEquals("owntracks/testUsername/testDevice", jsonNode.get("topic").asText())
        assertEquals(123456789, jsonNode.get("tst").asLong())
        assertEquals(1, jsonNode.get("vac").asInt())
        assertEquals(5, jsonNode.get("vel").asInt())
    }

    @Test
    fun `Parser can deserialize an encrypted location message`() {
        `when`(encryptionProvider.isPayloadEncryptionEnabled).thenReturn(true)
        @Language("JSON") val messageLocationJSON = """
                {
                  "_type": "location",
                  "tid": "s5",
                  "acc": 1600,
                  "alt": 0.0,
                  "batt": 99,
                  "bs": 1,
                  "conn": "w",
                  "lat": 52.3153748,
                  "lon": 5.0408462,
                  "t": "p",
                  "tst": 1514455575,
                  "vac": 0,
                  "vel": 2
                }
        """.trimIndent()
        `when`(encryptionProvider.decrypt("TestCipherText")).thenReturn(messageLocationJSON)
        val parser = Parser(encryptionProvider)
        @Language("JSON") val input = """
            {
              "_type": "encrypted",
              "data": "TestCipherText"
            }
        """.trimIndent()
        val messageBase = parser.fromJson(input)
        assertEquals(MessageLocation::class.java, messageBase.javaClass)
        val messageLocation = messageBase as MessageLocation
        assertEquals(1514455575L, messageLocation.timestamp)
        assertEquals("s5", messageLocation.trackerId)
        assertEquals(1600, messageLocation.accuracy.toLong())
        assertEquals(0.0, messageLocation.altitude.toDouble(), 0.0)
        assertEquals(99, messageLocation.battery.toLong())
        assertEquals("w", messageLocation.conn)
        assertEquals(52.3153748, messageLocation.latitude, 0.0)
        assertEquals(5.0408462, messageLocation.longitude, 0.0)
        assertEquals("p", messageLocation.trigger)
        assertEquals(0f, messageLocation.verticalAccuracy.toFloat(), 0f)
        assertEquals(2f, messageLocation.velocity.toFloat(), 0f)
    }

    @Test
    fun `Parser can serialize an encrypted location message`() {
        val dummyCipherText = "TestCipherText"
        `when`(encryptionProvider.isPayloadEncryptionEnabled).thenReturn(true)
        `when`(encryptionProvider.encrypt(anyString())).thenReturn(dummyCipherText)
        val parser = Parser(encryptionProvider)
        val input = extendedMessageLocation
        val serialized = input.toJson(parser)
        val jsonNode = objectMapper.readTree(serialized)
        assertTrue(jsonNode.isObject)
        assertEquals("encrypted", jsonNode.get("_type").asText())
        assertEquals(dummyCipherText, jsonNode.get("data").asText())
    }

    @Test(expected = EncryptionException::class)
    fun `Parser should raise an exception when given an encrypted message with encryption disabled`() {
        `when`(encryptionProvider.isPayloadEncryptionEnabled).thenReturn(false)
        val parser = Parser(encryptionProvider)
        @Language("JSON") val input = """
            {
              "_type": "encrypted",
              "data": "TestCipherText"
            }
        """.trimIndent()
        parser.fromJson(input)
    }

    @Test
    fun `Parser can deserialize multiple location messages in same document`() {
        @Language("JSON") val multipleMessageLocationJSON = """
            [
              {
                "_type": "location",
                "tid": "s5",
                "acc": 1600,
                "alt": 0.0,
                "batt": 99,
                "conn": "w",
                "lat": 52.3153748,
                "lon": 5.0408462,
                "t": "p",
                "tst": 1514455575,
                "vac": 0
              },
              {
                "_type": "location",
                "tid": "s5",
                "acc": 95,
                "alt": 0.0,
                "batt": 99,
                "conn": "w",
                "lat": 12.3153748,
                "lon": 15.0408462,
                "t": "p",
                "tst": 1514455579,
                "vac": 0
              }
            ]
        """.trimIndent()
        val parser = Parser(encryptionProvider)
        val byteArrayInputStream = ByteArrayInputStream(multipleMessageLocationJSON.toByteArray())
        val messages = parser.fromJson(byteArrayInputStream)
        assertEquals(2, messages.size.toLong())
        for (messageBase in messages) {
            assertEquals(MessageLocation::class.java, messageBase.javaClass)
        }
        val firstMessageLocation = messages[0] as MessageLocation
        assertEquals(1514455575L, firstMessageLocation.timestamp)
        val secondMessageLocation = messages[1] as MessageLocation
        assertEquals(1514455579L, secondMessageLocation.timestamp)
    }
    //endregion

    //region Command Messages
    @Test
    fun `Parser can deserialize a reportLocation cmd message`() {
        val parser = Parser(encryptionProvider)
        @Language("JSON") val input = """
            {
              "_type": "cmd",
              "action": "reportLocation"
            }
        """.trimIndent()
        val messageBase = parser.fromJson(input)
        messageBase.topic = "owntracks/username/device/cmd"
        assertEquals(MessageCmd::class.java, messageBase.javaClass)
        val messageCmd = messageBase as MessageCmd
        assertTrue(messageCmd.isValidMessage())
        assertEquals(CommandAction.REPORT_LOCATION, messageCmd.action)
        assertEquals("owntracks/username/device", messageCmd.contactKey)
    }

    @Test
    fun `Parser can deserialize a setWaypoints cmd message`() {
        val parser = Parser(encryptionProvider)
        @Language("JSON") val input =
            """
            {
              "_type": "cmd",
              "action": "setWaypoints",
              "waypoints": {
                "_type": "waypoints",
                "waypoints": []
              }
            }
            """.trimIndent()
        val messageBase = parser.fromJson(input)
        assertEquals(MessageCmd::class.java, messageBase.javaClass)
        val messageCmd = messageBase as MessageCmd
        assertTrue(messageCmd.isValidMessage())
        assertEquals(CommandAction.SET_WAYPOINTS, messageCmd.action)
        assertEquals(0, messageCmd.waypoints!!.waypoints!!.size)
    }

    @Test
    fun `Parser can deserialize a setConfiguration message`() {
        val parser = Parser(encryptionProvider)
        @Language("JSON") val input = """
            {
              "_type": "cmd",
              "action": "setConfiguration",
              "configuration": {
                "_type": "configuration",
                "host": "newHost"
              }
            }
        """.trimIndent()
        val messageBase = parser.fromJson(input)
        assertEquals(MessageCmd::class.java, messageBase.javaClass)
        val messageCmd = messageBase as MessageCmd
        assertTrue(messageCmd.isValidMessage())
        assertEquals(CommandAction.SET_CONFIGURATION, messageCmd.action)
        assertEquals("newHost", messageCmd.configuration!!["host"])
    }

    @Test(expected = InvalidFormatException::class)
    fun `Parser throws exception when given cmd with invalid action`() {
        val parser = Parser(encryptionProvider)
        @Language("JSON") val input = """
            {
              "_type": "cmd",
              "action": "nope",
              "sometgi": "parp"
            }
        """.trimIndent()
        parser.fromJson(input)
    }

    //endregion

    //region Transition Messages
    @Test
    fun `Parser can deserialize a transition message`() {
        val parser = Parser(encryptionProvider)
        @Language("JSON") val input = """
            {
              "_type": "transition",
              "acc": 3.075,
              "desc": "myregion",
              "event": "leave",
              "lat": 52.71234,
              "lon": -1.61234123,
              "t": "l",
              "tid": "ce",
              "tst": 1603209966,
              "wtst": 1558351273
            }
        """.trimIndent()
        val messageBase = parser.fromJson(input)
        messageBase.topic = "owntracks/username/device/event"
        assertEquals(MessageTransition::class.java, messageBase.javaClass)
        val message = messageBase as MessageTransition
        assertTrue(message.isValidMessage())
        assertEquals(2, message.getTransition())
        assertEquals(3.075f, message.accuracy, 0f)
        assertEquals("myregion", message.description)
        assertEquals(52.71234, message.latitude, 0.0)
        assertEquals(-1.61234123, message.longitude, 0.0)
        assertEquals(1603209966, message.timestamp)
        assertEquals(1558351273, message.waypointTimestamp)
        assertEquals("ce", message.trackerId)
        assertEquals("l", message.trigger)
        assertEquals("owntracks/username/device", message.contactKey)
    }

    @Test
    fun `Parser can serialize a transition message`() {
        `when`(encryptionProvider.isPayloadEncryptionEnabled).thenReturn(false)
        val parser = Parser(encryptionProvider)
        val message = MessageTransition().apply {
            latitude = 52.71234
            longitude = -1.61234123
            description = "myregion"
            setTransition(Geofence.GEOFENCE_TRANSITION_EXIT)
            accuracy = 3.075f
            timestamp = 1603209966
            waypointTimestamp = 1558351273
            trackerId = "ce"
            trigger = "l"
        }
        val serialized = message.toJson(parser)
        val jsonNode = objectMapper.readTree(serialized)
        assertTrue(jsonNode.isObject)
        assertEquals("transition", jsonNode.get("_type").asText())
        assertEquals(message.accuracy.toDouble(), jsonNode.get("acc").asDouble(), 0.001)
        assertEquals(message.description, jsonNode.get("desc").asText())
        assertEquals("leave", jsonNode.get("event").asText())
        assertEquals(message.latitude, jsonNode.get("lat").asDouble(), 0.001)
        assertEquals(message.longitude, jsonNode.get("lon").asDouble(), 0.001)
        assertEquals(message.trigger, jsonNode.get("t").asText())
        assertEquals(message.trackerId, jsonNode.get("tid").asText())
        assertEquals(message.timestamp, jsonNode.get("tst").asLong())
        assertEquals(message.waypointTimestamp, jsonNode.get("wtst").asLong())
    }
    //endregion

    //region Configuration messages
    @Test
    fun `Parser can deserialize a configuration message`() {
        val parser = Parser(encryptionProvider)
        @Language("JSON") val input = """
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
              "locatorPriority": 2,
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
        val messageBase = parser.fromJson(input)
        assertEquals(MessageConfiguration::class.java, messageBase.javaClass)
        val message = messageBase as MessageConfiguration
        assertTrue(message.isValidMessage())
        assertFalse(message.waypoints.isEmpty())
        assertEquals(2, message.waypoints.size)
        assertFalse(message.hasTrackerId())
        assertEquals(true, message.get("autostartOnBoot"))
        assertEquals(5, message.get("locatorDisplacement"))
    }

    @Test
    fun `Parser can serialize a configuration message`() {
        `when`(encryptionProvider.isPayloadEncryptionEnabled).thenReturn(false)
        val parser = Parser(encryptionProvider)
        val message = MessageConfiguration()
        message["TestBoolKey"] = true
        message["TestStringKey"] = "testString"
        message["TestIntKey"] = 13487
        message["TestFloatKey"] = 13487f
        val waypoint = MessageWaypoint().apply {
            latitude = 51.0
            longitude = -20.0
            description = "Test waypoint"
            radius = 45
            timestamp = 123456789
        }
        message.waypoints.add(waypoint)
        val serialized = message.toJson(parser)
        val jsonNode = objectMapper.readTree(serialized)

        assertTrue(jsonNode.isObject)
        assertEquals("configuration", jsonNode.get("_type").asText())
        assertTrue(jsonNode.get("waypoints").isArray)
        assertEquals(1, jsonNode.get("waypoints").count())
        assertTrue(jsonNode.get("waypoints").get(0).isObject)
        assertEquals("waypoint", jsonNode.get("waypoints").get(0).get("_type").asText())
        assertEquals("Test waypoint", jsonNode.get("waypoints").get(0).get("desc").asText())
        assertEquals(51.0, jsonNode.get("waypoints").get(0).get("lat").asDouble(), 0.00001)
        assertEquals(-20.0, jsonNode.get("waypoints").get(0).get("lon").asDouble(), 0.00001)
        assertEquals(45, jsonNode.get("waypoints").get(0).get("rad").asInt())
        assertEquals(123456789, jsonNode.get("waypoints").get(0).get("tst").asInt())
        assertTrue(jsonNode.get("TestBoolKey").asBoolean())
        assertEquals(13487.0, jsonNode.get("TestFloatKey").asDouble(), 0.0001)
        assertEquals(13487, jsonNode.get("TestIntKey").asInt())
        assertEquals("testString", jsonNode.get("TestStringKey").asText())
    }
    //endregion

    //region Waypoint Messages
    @Test
    fun `Parser can deserialize a waypoint message`() {
        val parser = Parser(encryptionProvider)
        @Language("JSON") val input = """
            {
              "_type": "waypoint",
              "desc": "mypoint",
              "lat": 52.0027789,
              "lon": -1.0829312,
              "rad": 150,
              "tst": 1558351273
            }
        """.trimIndent()
        val messageBase = parser.fromJson(input)
        assertEquals(MessageWaypoint::class.java, messageBase.javaClass)
        val message = messageBase as MessageWaypoint
        assertTrue(message.isValidMessage())
        assertEquals("mypoint", message.description)
        assertEquals(52.0027789, message.latitude, 0.0)
        assertEquals(-1.0829312, message.longitude, 0.0)
        assertEquals(150, message.radius)
        assertEquals(1558351273, message.timestamp)
    }

    @Test
    fun `Parser can serialize a waypoint message`() {
        `when`(encryptionProvider.isPayloadEncryptionEnabled).thenReturn(false)
        val parser = Parser(encryptionProvider)
        val message = MessageWaypoint().apply {
            latitude = 52.0027789
            longitude = -1.0829312
            description = "mypoint"
            radius = 150
            timestamp = 1558351273
        }
        val serialized = message.toJson(parser)
        val jsonNode = objectMapper.readTree(serialized)
        assertTrue(jsonNode.isObject)
        assertEquals("waypoint", jsonNode.get("_type").asText())
        assertEquals("mypoint", jsonNode.get("desc").asText())
        assertEquals(message.latitude.toDouble(), jsonNode.get("lat").asDouble(), 0.00001)
        assertEquals(message.longitude.toDouble(), jsonNode.get("lon").asDouble(), 0.00001)
        assertEquals(message.radius, jsonNode.get("rad").asInt())
        assertEquals(message.timestamp, jsonNode.get("tst").asLong())
    }
    //endregion

    //region Clear Messages
    @Test
    fun `Parser can serialize a clear messages`() {
        `when`(encryptionProvider.isPayloadEncryptionEnabled).thenReturn(false)
        val parser = Parser(encryptionProvider)
        val message = MessageClear()
        val serialized = message.toJson(parser)
        assertEquals("", serialized)
    }

    @Test
    fun `Parser can serialize a clear messages to a byte array`() {
        `when`(encryptionProvider.isPayloadEncryptionEnabled).thenReturn(false)
        val parser = Parser(encryptionProvider)
        val message = MessageClear()
        val serialized = message.toJsonBytes(parser)
        assertEquals(0, serialized.size)
    }
    //endregion

    // Card Messages
    @Test
    fun `Parser can deserialize a MessageCard`() {
        val parser = Parser(encryptionProvider)
        @Language("JSON") val input = """
            {
              "_type": "card",
              "face": "iVBORw0KGgoAAAANSUhEUgAAACgAAAAoCAYAAACM/rhtAAABIElEQVRYhe2XsQ2DMBBFvQkrUNEihvEOTMEU9IzAAuxAwxz3UxCIIxw58n0LC/lXURI/Xs65MxhkHnO3QChFUJvnCxpjLq9FRIv9MDWLx3H0Q42hSUYLhgTcymoSTamqivKdUKIF/6nQuq6x+M91ohcWwfd1YheGmkREKJ2sarWu635+tiyLBn1GPQu2bYO1FsBeNWttPoPaDVPKDVWQNZzd0IiHXN/3LOTOpYESVA8gCYoI5nlmoC6h/uyjUZjVpJCGYWBgvKEINk0DEUHbtud70zQx0Nwt9t1dq5lqgCPiNsrtd9ReWM6DGsDXf5CV5z92XoDkbabRUmwvkOAsFhHUdc3C8gTdEZPdUZcyyQSzOup8MtkcdalTBLUpgtq8ACxgjcQLy0DfAAAAAElFTkSuQmCC",
              "name": "MyName!"
            }
        """.trimIndent()
        val messageBase = parser.fromJson(input)
        messageBase.topic = "owntracks/user/device/info"
        assertEquals(MessageCard::class.java, messageBase.javaClass)
        val messageCard = messageBase as MessageCard
        assertTrue(messageCard.isValidMessage())
        assertEquals("MyName!", messageCard.name)
        assertEquals("owntracks/user/device", messageCard.contactKey)
    }
    //endregion

    //region  Invalid messages
    @Test(expected = IOException::class)
    fun `Parser should throw exception given an empty array`() {
        val parser = Parser(encryptionProvider)
        val byteArrayInputStream = ByteArrayInputStream(ByteArray(0))
        parser.fromJson(byteArrayInputStream)
    }

    @Test
    fun `Parser can deserialize an Unknown message`() {
        val parser = Parser(encryptionProvider)
        @Language("JSON") val message = parser.fromJson(
            """
            {
              "some": "invalid message"
            }
            """.trimIndent()
        )
        assertEquals(MessageUnknown::class.java, message.javaClass)
    }

    @Test(expected = JsonParseException::class)
    fun `Parser throws exception when given invalid JSON as message body`() {
        val parser = Parser(encryptionProvider)
        parser.fromJson("not JSON")
    }
    //endregion

    inner class FakeClock : Clock {
        override val time: Long = 25
    }
}
