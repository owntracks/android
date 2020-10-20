package org.owntracks.android.support

import com.fasterxml.jackson.core.JsonParseException
import com.fasterxml.jackson.databind.exc.InvalidFormatException
import com.nhaarman.mockitokotlin2.doReturn
import com.nhaarman.mockitokotlin2.mock
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.Mockito
import org.owntracks.android.messages.*
import org.owntracks.android.model.BatteryStatus
import org.owntracks.android.model.CommandAction
import org.owntracks.android.support.Parser.EncryptionException
import java.io.ByteArrayInputStream
import java.io.IOException
import java.util.*

class ParserTest {
    private var messageLocation: MessageLocation? = null

    @Mock
    private lateinit var testPreferences: Preferences

    // Need to mock this as we can't unit test native lib off-device
    @Mock
    private lateinit var encryptionProvider: EncryptionProvider

    //region Location Messages
    private val locationWithRegionsJSON = "{\"_type\":\"location\",\"acc\":10,\"alt\":20,\"batt\":30,\"bs\":2,\"conn\":\"TestConn\",\"inregions\":[\"Testregion1\",\"Testregion2\"],\"lat\":50.1,\"lon\":60.2,\"tst\":123456789,\"vac\":1,\"vel\":5}"

    @Before
    fun setupMessageLocation() {
        val regions: MutableList<String> = LinkedList()
        regions.add("Testregion1")
        regions.add("Testregion2")
        messageLocation = MessageLocation()
        messageLocation!!.acc = 10
        messageLocation!!.alt = 20
        messageLocation!!.battery = 30
        messageLocation!!.batteryStatus = BatteryStatus.CHARGING
        messageLocation!!.conn = "TestConn"
        messageLocation!!.setLat(50.1)
        messageLocation!!.setLon(60.2)
        messageLocation!!.tst = 123456789
        messageLocation!!.velocity = 5.6.toInt()
        messageLocation!!.vac = 1.7.toInt()
        messageLocation!!.inRegions = regions
    }

    @Before
    fun setupEncryptionProvider() {
        testPreferences = mock {
            on { encryptionKey } doReturn "testEncryptionKey"
        }
        encryptionProvider = mock { on { isPayloadEncryptionEnabled } doReturn false }
    }

    @Test
    fun `Parser can serialize location message to a pretty JSON message`() {
        val parser = Parser(null)
        val expected = """
            {
              "_type" : "location",
              "acc" : 10,
              "alt" : 20,
              "batt" : 30,
              "bs" : 2,
              "conn" : "TestConn",
              "inregions" : [ "Testregion1", "Testregion2" ],
              "lat" : 50.1,
              "lon" : 60.2,
              "tst" : 123456789,
              "vac" : 1,
              "vel" : 5
            }
        """.trimIndent()
        assertEquals(expected, parser.toJsonPlainPretty(messageLocation!!))
    }

    @Test
    fun `Parser can deserialize a location message`() {
        Mockito.`when`(encryptionProvider.isPayloadEncryptionEnabled).thenReturn(false)
        val parser = Parser(encryptionProvider)
        val input = "{\"_type\":\"location\",\"tid\":\"s5\",\"acc\":1600,\"alt\":0.0,\"batt\":99,\"bs\":3,\"conn\":\"w\",\"lat\":52.3153748,\"lon\":5.0408462,\"t\":\"p\",\"tst\":1514455575,\"vac\":0,\"inregions\":[\"Testregion1\",\"Testregion2\"]}"
        val messageBase = parser.fromJson(input)
        assertEquals(MessageLocation::class.java, messageBase.javaClass)
        val message = messageBase as MessageLocation
        assertEquals(1514455575L, message.tst)
        assertEquals("s5", message.tid)
        assertEquals(1600, message.acc.toLong())
        assertEquals(0.0, message.alt.toDouble(), 0.0)
        assertEquals(99, message.battery.toLong())
        assertEquals(BatteryStatus.FULL, message.batteryStatus)
        assertEquals("w", message.conn)
        assertEquals(52.3153748, message.latitude, 0.0)
        assertEquals(5.0408462, message.longitude, 0.0)
        assertEquals("p", message.t)
        assertEquals(0f, message.vac.toFloat(), 0f)
        assertEquals(2, message.inRegions.size.toLong())
    }

    @Test
    fun `Parser can serialize a location message`() {
        Mockito.`when`(encryptionProvider.isPayloadEncryptionEnabled).thenReturn(false)
        val parser = Parser(encryptionProvider)
        val input = messageLocation
        val serialized = input!!.toJson(parser)
        assertEquals(locationWithRegionsJSON, serialized)
    }

    @Test
    fun `Parser can deserialize an encrypted location message`() {
        Mockito.`when`(encryptionProvider.isPayloadEncryptionEnabled).thenReturn(true)
        val messageLocationJSON = "{\"_type\":\"location\",\"tid\":\"s5\",\"acc\":1600,\"alt\":0.0,\"batt\":99,\"bs\":1,\"conn\":\"w\",\"lat\":52.3153748,\"lon\":5.0408462,\"t\":\"p\",\"tst\":1514455575,\"vac\":0,\"vel\":2}"
        Mockito.`when`(encryptionProvider.decrypt("TestCipherText")).thenReturn(messageLocationJSON)
        val parser = Parser(encryptionProvider)
        val input = "{\"_type\":\"encrypted\",\"data\":\"TestCipherText\"}"
        val messageBase = parser.fromJson(input)
        assertEquals(MessageLocation::class.java, messageBase.javaClass)
        val messageLocation = messageBase as MessageLocation
        assertEquals(1514455575L, messageLocation.tst)
        assertEquals("s5", messageLocation.tid)
        assertEquals(1600, messageLocation.acc.toLong())
        assertEquals(0.0, messageLocation.alt.toDouble(), 0.0)
        assertEquals(99, messageLocation.battery.toLong())
        assertEquals("w", messageLocation.conn)
        assertEquals(52.3153748, messageLocation.latitude, 0.0)
        assertEquals(5.0408462, messageLocation.longitude, 0.0)
        assertEquals("p", messageLocation.t)
        assertEquals(0f, messageLocation.vac.toFloat(), 0f)
        assertEquals(2f, messageLocation.velocity.toFloat(), 0f)
    }

    @Test
    fun `Parser can serialize an encrypted location message`() {
        Mockito.`when`(encryptionProvider.isPayloadEncryptionEnabled).thenReturn(true)
        Mockito.`when`(encryptionProvider.encrypt(locationWithRegionsJSON)).thenReturn("TestCipherText")
        val parser = Parser(encryptionProvider)
        val input = messageLocation
        val serialized = input!!.toJson(parser)
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
        val multipleMessageLocationJSON = "[{\"_type\":\"location\",\"tid\":\"s5\",\"acc\":1600,\"alt\":0.0,\"batt\":99,\"conn\":\"w\",\"lat\":52.3153748,\"lon\":5.0408462,\"t\":\"p\",\"tst\":1514455575,\"vac\":0},{\"_type\":\"location\",\"tid\":\"s5\",\"acc\":95,\"alt\":0.0,\"batt\":99,\"conn\":\"w\",\"lat\":12.3153748,\"lon\":15.0408462,\"t\":\"p\",\"tst\":1514455579,\"vac\":0}]"
        val parser = Parser(encryptionProvider)
        val byteArrayInputStream = ByteArrayInputStream(multipleMessageLocationJSON.toByteArray())
        val messages = parser.fromJson(byteArrayInputStream)
        assertEquals(2, messages.size.toLong())
        for (messageBase in messages) {
            assertEquals(MessageLocation::class.java, messageBase.javaClass)
        }
        val firstMessageLocation = messages[0] as MessageLocation
        assertEquals(1514455575L, firstMessageLocation.tst)
        val secondMessageLocation = messages[1] as MessageLocation
        assertEquals(1514455579L, secondMessageLocation.tst)
    }
    //endregion

    //region Command Messages
    @Test
    fun `Parser can deserialize a reportLocation cmd message`() {
        val parser = Parser(encryptionProvider)
        val input = "{\"_type\":\"cmd\", \"action\":\"reportLocation\"}"
        val messageBase = parser.fromJson(input)
        assertEquals(MessageCmd::class.java, messageBase.javaClass)
        val messageCmd = messageBase as MessageCmd
        assertTrue(messageCmd.isValidMessage)
        assertEquals(CommandAction.REPORT_LOCATION, messageCmd.action)
    }

    @Test
    fun `Parser can deserialize a restart cmd message`() {
        val parser = Parser(encryptionProvider)
        val input = "{\"_type\":\"cmd\", \"action\":\"restart\"}"
        val messageBase = parser.fromJson(input)
        assertEquals(MessageCmd::class.java, messageBase.javaClass)
        val messageCmd = messageBase as MessageCmd
        assertTrue(messageCmd.isValidMessage)
        assertEquals(CommandAction.RESTART, messageCmd.action)
    }

    @Test
    fun `Parser can deserialize a setWaypoints cmd message`() {
        val parser = Parser(encryptionProvider)
        val input = "{\"_type\":\"cmd\",\"action\":\"setWaypoints\",\"waypoints\":{\"_type\":\"waypoints\",\"waypoints\":[]}}"
        val messageBase = parser.fromJson(input)
        assertEquals(MessageCmd::class.java, messageBase.javaClass)
        val messageCmd = messageBase as MessageCmd
        assertTrue(messageCmd.isValidMessage)
        assertEquals(CommandAction.SET_WAYPOINTS, messageCmd.action)
        assertEquals(0, messageCmd.waypoints!!.waypoints!!.size)
    }

    @Test
    fun `Parser can deserialize a setConfiguration message`() {
        val parser = Parser(encryptionProvider)
        val input = "{\"_type\":\"cmd\",\"action\":\"setConfiguration\",\"configuration\":{\"_type\":\"configuration\",\"host\":\"newHost\"}}"
        val messageBase = parser.fromJson(input)
        assertEquals(MessageCmd::class.java, messageBase.javaClass)
        val messageCmd = messageBase as MessageCmd
        assertTrue(messageCmd.isValidMessage)
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
        val input = "{\"_type\":\"transition\",\"acc\":3.075,\"desc\":\"myregion\",\"event\":\"leave\",\"lat\":52.71234,\"lon\":-1.61234123,\"t\":\"l\",\"tid\":\"ce\",\"tst\":1603209966,\"wtst\":1558351273}"
        val messageBase = parser.fromJson(input)
        assertEquals(MessageTransition::class.java, messageBase.javaClass)
        val message = messageBase as MessageTransition
        assertTrue(message.isValidMessage)
        assertEquals(2, message.transition)
        assertEquals(3.075f, message.acc, 0f)
        assertEquals("myregion", message.desc)
        assertEquals(52.71234, message.lat, 0.0)
        assertEquals(-1.61234123, message.lon, 0.0)
        assertEquals(1603209966, message.tst)
        assertEquals(1558351273, message.wtst)
        assertEquals("ce", message.tid)
        assertEquals("l", message.trigger)
    }

    @Test
    fun `Parser can serialize a transition message`() {
        Mockito.`when`(encryptionProvider.isPayloadEncryptionEnabled).thenReturn(false)
        val parser = Parser(encryptionProvider)
        val message = MessageTransition()
        message.lat = 52.71234
        message.lon = -1.61234123
        message.desc = "myregion"
        message.transition = 2
        message.acc = 3.075f
        message.tst = 1603209966
        message.wtst = 1558351273
        message.tid = "ce"
        message.trigger = "l"
        val serialized = message.toJson(parser)
        val expected = "{\"_type\":\"transition\",\"acc\":3.075,\"desc\":\"myregion\",\"event\":\"leave\",\"lat\":52.71234,\"lon\":-1.61234123,\"t\":\"l\",\"tid\":\"ce\",\"tst\":1603209966,\"wtst\":1558351273}"
        assertEquals(expected, serialized)
    }
    //endregion

    //region Waypoint Messages
    @Test
    fun `Parser can deserialize a waypoint message`() {
        val parser = Parser(encryptionProvider)
        val input = "{\"_type\":\"waypoint\",\"desc\":\"mypoint\",\"lat\":52.0027789,\"lon\":-1.0829312,\"rad\":150,\"tst\":1558351273}"
        val messageBase = parser.fromJson(input)
        assertEquals(MessageWaypoint::class.java, messageBase.javaClass)
        val message = messageBase as MessageWaypoint
        assertTrue(message.isValidMessage)
        assertEquals("mypoint", message.desc)
        assertEquals(52.0027789, message.lat, 0.0)
        assertEquals(-1.0829312, message.lon, 0.0)
        assertEquals(150, message.rad)
        assertEquals(1558351273, message.tst)
    }

    @Test
    fun `Parser can serialize a waypoint message`() {
        Mockito.`when`(encryptionProvider.isPayloadEncryptionEnabled).thenReturn(false)
        val parser = Parser(encryptionProvider)
        val message = MessageWaypoint()
        message.lat = 52.0027789
        message.lon = -1.0829312
        message.desc = "mypoint"
        message.rad = 150
        message.tst = 1558351273
        val serialized = message.toJson(parser)
        val expected = "{\"_type\":\"waypoint\",\"desc\":\"mypoint\",\"lat\":52.0027789,\"lon\":-1.0829312,\"rad\":150,\"tst\":1558351273}"
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
}