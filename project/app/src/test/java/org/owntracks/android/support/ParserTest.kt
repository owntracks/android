package org.owntracks.android.support

import com.fasterxml.jackson.core.JsonParseException
import com.fasterxml.jackson.databind.exc.InvalidFormatException
import com.nhaarman.mockitokotlin2.doReturn
import com.nhaarman.mockitokotlin2.mock
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.Mockito
import org.owntracks.android.location.geofencing.Geofence
import org.owntracks.android.model.BatteryStatus
import org.owntracks.android.model.CommandAction
import org.owntracks.android.model.messages.*
import org.owntracks.android.services.LocationProcessor.MONITORING_SIGNIFICANT
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

    //region Location Messages
    private val locationWithRegionsJSON =
        "{\"_type\":\"location\",\"BSSID\":\"12:34:56:78\",\"SSID\":\"Wifi SSID\",\"acc\":10,\"alt\":20,\"batt\":30,\"bs\":2,\"conn\":\"TestConn\",\"created_at\":25,\"inregions\":[\"Testregion1\",\"Testregion2\"],\"lat\":50.1,\"lon\":60.2,\"m\":1,\"tst\":123456789,\"vac\":1,\"vel\":5}"
    private val locationWithRegionsJSONWithTopic =
        "{\"_type\":\"location\",\"BSSID\":\"12:34:56:78\",\"SSID\":\"Wifi SSID\",\"acc\":10,\"alt\":20,\"batt\":30,\"bs\":2,\"conn\":\"TestConn\",\"created_at\":25,\"inregions\":[\"Testregion1\",\"Testregion2\"],\"lat\":50.1,\"lon\":60.2,\"m\":1,\"topic\":\"owntracks/testUsername/testDevice\",\"tst\":123456789,\"vac\":1,\"vel\":5}"

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
            monitoringMode = MONITORING_SIGNIFICANT
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
        val expected = """
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
        val expected = """
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
        Mockito.`when`(encryptionProvider.isPayloadEncryptionEnabled).thenReturn(false)
        val parser = Parser(encryptionProvider)
        val input =
            """
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
        Mockito.`when`(encryptionProvider.isPayloadEncryptionEnabled).thenReturn(false)
        val parser = Parser(encryptionProvider)
        val input = extendedMessageLocation
        val serialized = input.toJson(parser)
        assertEquals(locationWithRegionsJSON, serialized)
    }

    @Test
    fun `Parser can serialize a location message with the topic visible`() {
        Mockito.`when`(encryptionProvider.isPayloadEncryptionEnabled).thenReturn(false)
        val parser = Parser(encryptionProvider)
        val input = extendedMessageLocation
        input.addMqttPreferences(testPreferences)
        input.setTopicVisible()
        val serialized = input.toJson(parser)
        assertEquals(locationWithRegionsJSONWithTopic, serialized)
    }

    @Test
    fun `Parser can deserialize an encrypted location message`() {
        Mockito.`when`(encryptionProvider.isPayloadEncryptionEnabled).thenReturn(true)
        val messageLocationJSON =
            "{\"_type\":\"location\",\"tid\":\"s5\",\"acc\":1600,\"alt\":0.0,\"batt\":99,\"bs\":1,\"conn\":\"w\",\"lat\":52.3153748,\"lon\":5.0408462,\"t\":\"p\",\"tst\":1514455575,\"vac\":0,\"vel\":2}"
        Mockito.`when`(encryptionProvider.decrypt("TestCipherText")).thenReturn(messageLocationJSON)
        val parser = Parser(encryptionProvider)
        val input = "{\"_type\":\"encrypted\",\"data\":\"TestCipherText\"}"
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
        Mockito.`when`(encryptionProvider.isPayloadEncryptionEnabled).thenReturn(true)
        Mockito.`when`(encryptionProvider.encrypt(locationWithRegionsJSON))
            .thenReturn("TestCipherText")
        val parser = Parser(encryptionProvider)
        val input = extendedMessageLocation
        val serialized = input.toJson(parser)
        val expected = """{"_type":"encrypted","data":"TestCipherText"}"""
        assertEquals(expected, serialized)
    }

    @Test(expected = EncryptionException::class)
    fun `Parser should raise an exception when given an encrypted message with encryption disabled`() {
        Mockito.`when`(encryptionProvider.isPayloadEncryptionEnabled).thenReturn(false)
        val parser = Parser(encryptionProvider)
        val input = "{\"_type\":\"encrypted\",\"data\":\"TestCipherText\"}"
        parser.fromJson(input)
    }

    @Test
    fun `Parser can deserialize multiple location messages in same document`() {
        val multipleMessageLocationJSON =
            "[{\"_type\":\"location\",\"tid\":\"s5\",\"acc\":1600,\"alt\":0.0,\"batt\":99,\"conn\":\"w\",\"lat\":52.3153748,\"lon\":5.0408462,\"t\":\"p\",\"tst\":1514455575,\"vac\":0},{\"_type\":\"location\",\"tid\":\"s5\",\"acc\":95,\"alt\":0.0,\"batt\":99,\"conn\":\"w\",\"lat\":12.3153748,\"lon\":15.0408462,\"t\":\"p\",\"tst\":1514455579,\"vac\":0}]"
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
        val input = "{\"_type\":\"cmd\", \"action\":\"reportLocation\"}"
        val messageBase = parser.fromJson(input)
        messageBase.topic = "owntracks/username/device/cmd"
        assertEquals(MessageCmd::class.java, messageBase.javaClass)
        val messageCmd = messageBase as MessageCmd
        assertTrue(messageCmd.isValidMessage())
        assertEquals(CommandAction.REPORT_LOCATION, messageCmd.action)
        assertEquals("owntracks/username/device", messageCmd.contactKey)
    }

    @Test
    fun `Parser can deserialize a restart cmd message`() {
        val parser = Parser(encryptionProvider)
        val input = "{\"_type\":\"cmd\", \"action\":\"restart\"}"
        val messageBase = parser.fromJson(input)
        assertEquals(MessageCmd::class.java, messageBase.javaClass)
        val messageCmd = messageBase as MessageCmd
        assertTrue(messageCmd.isValidMessage())
        assertEquals(CommandAction.RESTART, messageCmd.action)
    }

    @Test
    fun `Parser can deserialize a setWaypoints cmd message`() {
        val parser = Parser(encryptionProvider)
        val input =
            "{\"_type\":\"cmd\",\"action\":\"setWaypoints\",\"waypoints\":{\"_type\":\"waypoints\",\"waypoints\":[]}}"
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
        val input =
            "{\"_type\":\"cmd\",\"action\":\"setConfiguration\",\"configuration\":{\"_type\":\"configuration\",\"host\":\"newHost\"}}"
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
        val input = "{\"_type\":\"cmd\", \"action\":\"nope\", \"sometgi\":\"parp\"}"
        parser.fromJson(input)
    }

    //endregion

    //region Transition Messages
    @Test
    fun `Parser can deserialize a transition message`() {
        val parser = Parser(encryptionProvider)
        val input =
            "{\"_type\":\"transition\",\"acc\":3.075,\"desc\":\"myregion\",\"event\":\"leave\",\"lat\":52.71234,\"lon\":-1.61234123,\"t\":\"l\",\"tid\":\"ce\",\"tst\":1603209966,\"wtst\":1558351273}"
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
        Mockito.`when`(encryptionProvider.isPayloadEncryptionEnabled).thenReturn(false)
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
        val expected =
            "{\"_type\":\"transition\",\"acc\":3.075,\"desc\":\"myregion\",\"event\":\"leave\",\"lat\":52.71234,\"lon\":-1.61234123,\"t\":\"l\",\"tid\":\"ce\",\"tst\":1603209966,\"wtst\":1558351273}"
        assertEquals(expected, serialized)
    }
    //endregion

    //region Configuration messages
    @Test
    fun `Parser can deserialize a configuration message`() {
        val parser = Parser(encryptionProvider)
        val input =
            "{\"_type\":\"configuration\",\"waypoints\":[{\"_type\":\"waypoint\",\"desc\":\"work\",\"lat\":51.504778900000005,\"lon\":-0.023851299999999995,\"rad\":150,\"tst\":1505910709000},{\"_type\":\"waypoint\",\"desc\":\"home\",\"lat\":53.6776261,\"lon\":-1.58268,\"rad\":100,\"tst\":1558351273}],\"auth\":true,\"autostartOnBoot\":true,\"cleanSession\":false,\"clientId\":\"emulator\",\"cmd\":true,\"debugLog\":true,\"deviceId\":\"testdevice\",\"fusedRegionDetection\":true,\"geocodeEnabled\":true,\"host\":\"127.0.0.1\",\"ignoreInaccurateLocations\":150,\"ignoreStaleLocations\":0.0,\"keepalive\":900,\"locatorDisplacement\":5,\"locatorInterval\":60,\"locatorPriority\":2,\"mode\":0,\"monitoring\":1,\"moveModeLocatorInterval\":10,\"mqttProtocolLevel\":3,\"notificationHigherPriority\":false,\"notificationLocation\":true,\"opencageApiKey\":\"testkey\",\"password\":\"testpassword\",\"ping\":30,\"port\":1883,\"pubExtendedData\":true,\"pubQos\":1,\"pubRetain\":true,\"pubTopicBase\":\"owntracks/%u/%d\",\"remoteConfiguration\":true,\"sub\":true,\"subQos\":2,\"subTopic\":\"owntracks/+/+\",\"tls\":false,\"usePassword\":true,\"username\":\"testusername\",\"ws\":false}"
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
        Mockito.`when`(encryptionProvider.isPayloadEncryptionEnabled).thenReturn(false)
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
        val expected =
            "{\"_type\":\"configuration\",\"waypoints\":[{\"_type\":\"waypoint\",\"desc\":\"Test waypoint\",\"lat\":51.0,\"lon\":-20.0,\"rad\":45,\"tst\":123456789}],\"TestBoolKey\":true,\"TestFloatKey\":13487.0,\"TestIntKey\":13487,\"TestStringKey\":\"testString\"}"
        assertEquals(expected, serialized)
    }
    //endregion

    //region Waypoint Messages
    @Test
    fun `Parser can deserialize a waypoint message`() {
        val parser = Parser(encryptionProvider)
        val input =
            "{\"_type\":\"waypoint\",\"desc\":\"mypoint\",\"lat\":52.0027789,\"lon\":-1.0829312,\"rad\":150,\"tst\":1558351273}"
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
        Mockito.`when`(encryptionProvider.isPayloadEncryptionEnabled).thenReturn(false)
        val parser = Parser(encryptionProvider)
        val message = MessageWaypoint().apply {
            latitude = 52.0027789
            longitude = -1.0829312
            description = "mypoint"
            radius = 150
            timestamp = 1558351273
        }
        val serialized = message.toJson(parser)
        val expected =
            "{\"_type\":\"waypoint\",\"desc\":\"mypoint\",\"lat\":52.0027789,\"lon\":-1.0829312,\"rad\":150,\"tst\":1558351273}"
        assertEquals(expected, serialized)
    }
    //endregion

    //region Clear Messages
    @Test
    fun `Parser can serialize a clear messages`() {
        Mockito.`when`(encryptionProvider.isPayloadEncryptionEnabled).thenReturn(false)
        val parser = Parser(encryptionProvider)
        val message = MessageClear()
        val serialized = message.toJson(parser)
        assertEquals("", serialized)
    }

    @Test
    fun `Parser can serialize a clear messages to a byte array`() {
        Mockito.`when`(encryptionProvider.isPayloadEncryptionEnabled).thenReturn(false)
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
        val input =
            "{\"_type\": \"card\",\"face\": \"iVBORw0KGgoAAAANSUhEUgAAACgAAAAoCAYAAACM/rhtAAABIElEQVRYhe2XsQ2DMBBFvQkrUNEihvEOTMEU9IzAAuxAwxz3UxCIIxw58n0LC/lXURI/Xs65MxhkHnO3QChFUJvnCxpjLq9FRIv9MDWLx3H0Q42hSUYLhgTcymoSTamqivKdUKIF/6nQuq6x+M91ohcWwfd1YheGmkREKJ2sarWu635+tiyLBn1GPQu2bYO1FsBeNWttPoPaDVPKDVWQNZzd0IiHXN/3LOTOpYESVA8gCYoI5nlmoC6h/uyjUZjVpJCGYWBgvKEINk0DEUHbtud70zQx0Nwt9t1dq5lqgCPiNsrtd9ReWM6DGsDXf5CV5z92XoDkbabRUmwvkOAsFhHUdc3C8gTdEZPdUZcyyQSzOup8MtkcdalTBLUpgtq8ACxgjcQLy0DfAAAAAElFTkSuQmCC\", \"name\":\"MyName!\"}"
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
        val message = parser.fromJson("{\"some\":\"invalid message\"}")
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