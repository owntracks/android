package org.owntracks.android.support

import com.fasterxml.jackson.core.JsonParseException
import com.fasterxml.jackson.databind.exc.InvalidFormatException
import com.nhaarman.mockitokotlin2.doReturn
import com.nhaarman.mockitokotlin2.mock
import org.junit.Assert
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.mockito.ArgumentMatchers
import org.mockito.Mock
import org.mockito.Mockito
import org.owntracks.android.messages.MessageCmd
import org.owntracks.android.messages.MessageLocation
import org.owntracks.android.messages.MessageUnknown
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
        encryptionProvider = mock { on { isPayloadEncryptionEnabled } doReturn true }
    }

    @Test
    @Throws(Exception::class)
    fun parserCorrectlyConvertsLocationToPrettyJSON() {
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
    @Throws(Exception::class)
    fun parserCorrectlyConvertsLocationToPlainJSON() {
        val parser = Parser(null)
        assertEquals(locationWithRegionsJSON, parser.toJsonPlain(messageLocation!!))
    }

    @Test
    @Throws(Exception::class)
    fun parserCorrectlyConvertsLocationToJSONWithEncryption() {
        Mockito.`when`(encryptionProvider.encrypt(ArgumentMatchers.anyString())).thenReturn("TestCipherText")
        val parser = Parser(encryptionProvider)
        val expected = "{\"_type\":\"encrypted\",\"data\":\"TestCipherText\"}"
        assertEquals(expected, parser.toJson(messageLocation!!))
    }

    @Test
    @Throws(Exception::class)
    fun parserCorrectlyConvertsLocationToJSONWithEncryptionDisabled() {
        Mockito.`when`(encryptionProvider.isPayloadEncryptionEnabled).thenReturn(false)
        Mockito.`when`(encryptionProvider.encrypt(ArgumentMatchers.anyString())).thenReturn("TestCipherText")
        val parser = Parser(encryptionProvider)
        assertEquals(locationWithRegionsJSON, parser.toJson(messageLocation!!))
    }

    @Test
    fun parserCanDeserializeLocationMessage() {
        Mockito.`when`(encryptionProvider.isPayloadEncryptionEnabled).thenReturn(false)
        val parser = Parser(encryptionProvider)
        val input = "{\"_type\":\"location\",\"tid\":\"s5\",\"acc\":1600,\"alt\":0.0,\"batt\":99,\"bs\":3,\"conn\":\"w\",\"lat\":52.3153748,\"lon\":5.0408462,\"t\":\"p\",\"tst\":1514455575,\"vac\":0,\"inregions\":[\"Testregion1\",\"Testregion2\"]}"
        val messageBase = parser.fromJson(input)
        assertEquals(MessageLocation::class.java, messageBase.javaClass)
        val messageLocation = messageBase as MessageLocation
        assertEquals(1514455575L, messageLocation.tst)
        assertEquals("s5", messageLocation.tid)
        assertEquals(1600, messageLocation.acc.toLong())
        assertEquals(0.0, messageLocation.alt.toDouble(), 0.0)
        assertEquals(99, messageLocation.battery.toLong())
        assertEquals(BatteryStatus.FULL, messageLocation.batteryStatus)
        assertEquals("w", messageLocation.conn)
        assertEquals(52.3153748, messageLocation.latitude, 0.0)
        assertEquals(5.0408462, messageLocation.longitude, 0.0)
        assertEquals("p", messageLocation.t)
        assertEquals(0f, messageLocation.vac.toFloat(), 0f)
        assertEquals(2, messageLocation.inRegions.size.toLong())
    }

    @Test
    fun parserCanSerializeLocationMessage() {
        Mockito.`when`(encryptionProvider.isPayloadEncryptionEnabled).thenReturn(false)
        val parser = Parser(encryptionProvider)
        val input = messageLocation
        val serialized = parser.toJson(input!!)
        assertEquals(locationWithRegionsJSON, serialized)
    }

    @Test
    fun parserCanDeserializeEncryptedLocationMessage() {
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
    fun parserCanSerializeEncryptedLocationMessage() {
        Mockito.`when`(encryptionProvider.isPayloadEncryptionEnabled).thenReturn(true)
        Mockito.`when`(encryptionProvider.encrypt(locationWithRegionsJSON)).thenReturn("TestCipherText")
        val parser = Parser(encryptionProvider)
        val input = messageLocation
        val serialized = parser.toJson(input!!)
        val expected = """{"_type":"encrypted","data":"TestCipherText"}"""
        assertEquals(expected, serialized)
    }

    @Test(expected = EncryptionException::class)
    @Throws(Exception::class)
    fun parserShouldThrowExceptionWhenGivenEncryptedMessageWithEncryptionDisabled() {
        Mockito.`when`(encryptionProvider.isPayloadEncryptionEnabled).thenReturn(false)
        val parser = Parser(encryptionProvider)
        val input = "{\"_type\":\"encrypted\",\"data\":\"TestCipherText\"}"
        parser.fromJson(input)
    }

    @Test
    @Throws(Exception::class)
    fun parserShouldDecodeStreamOfMultipleMessageLocationsCorrectly() {
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

    @Test
    @Throws(Exception::class)
    fun parserReturnsMessageLocationFromValidCmdReportLocationInput() {
        val parser = Parser(encryptionProvider)
        val input = "{\"_type\":\"cmd\", \"action\":\"reportLocation\"}"
        val messageBase = parser.fromJson(input)
        assertEquals(MessageCmd::class.java, messageBase.javaClass)
        val messageCmd = messageBase as MessageCmd
        Assert.assertTrue(messageCmd.isValidMessage)
        assertEquals(CommandAction.REPORT_LOCATION, messageCmd.action)
    }

    @Test
    @Throws(Exception::class)
    fun parserReturnsMessageLocationFromValidCmdRestartInput() {
        val parser = Parser(encryptionProvider)
        val input = "{\"_type\":\"cmd\", \"action\":\"restart\"}"
        val messageBase = parser.fromJson(input)
        assertEquals(MessageCmd::class.java, messageBase.javaClass)
        val messageCmd = messageBase as MessageCmd
        Assert.assertTrue(messageCmd.isValidMessage)
        assertEquals(CommandAction.RESTART, messageCmd.action)
    }

    @Test
    @Throws(Exception::class)
    fun parserReturnsMessageLocationFromValidCmdWaypointsInput() {
        val parser = Parser(encryptionProvider)
        val input = "{\"_type\":\"cmd\",\"action\":\"setWaypoints\",\"waypoints\":{\"_type\":\"waypoints\",\"waypoints\":[]}}"
        val messageBase = parser.fromJson(input)
        assertEquals(MessageCmd::class.java, messageBase.javaClass)
        val messageCmd = messageBase as MessageCmd
        Assert.assertTrue(messageCmd.isValidMessage)
        assertEquals(CommandAction.SET_WAYPOINTS, messageCmd.action)
        assertEquals(0, messageCmd.waypoints!!.waypoints!!.size)
    }

    @Test
    @Throws(Exception::class)
    fun parserReturnsMessageLocationFromValidCmdConfigInput() {
        val parser = Parser(encryptionProvider)
        val input = "{\"_type\":\"cmd\",\"action\":\"setConfiguration\",\"configuration\":{\"_type\":\"configuration\",\"host\":\"newHost\"}}"
        val messageBase = parser.fromJson(input)
        assertEquals(MessageCmd::class.java, messageBase.javaClass)
        val messageCmd = messageBase as MessageCmd
        Assert.assertTrue(messageCmd.isValidMessage)
        assertEquals(CommandAction.SET_CONFIGURATION, messageCmd.action)
        assertEquals("newHost", messageCmd.configuration!!["host"])
    }

    @Test(expected = InvalidFormatException::class)
    @Throws(Exception::class)
    fun parserReturnsMessageLocationFromInValidCmdInput() {
        val parser = Parser(encryptionProvider)
        val input = "{\"_type\":\"cmd\", \"action\":\"nope\", \"sometgi\":\"parp\"}"
        parser.fromJson(input)
    }

    /* Invalid messages */
    @Test(expected = IOException::class)
    @Throws(Exception::class)
    fun parserShouldThrowExceptionOnEmptyArray() {
        val parser = Parser(encryptionProvider)
        val byteArrayInputStream = ByteArrayInputStream(ByteArray(0))
        parser.fromJson(byteArrayInputStream)
    }

    @Test
    @Throws(Exception::class)
    fun parserReturnsMessageUnknownOnValidOtherJSON() {
        val parser = Parser(encryptionProvider)
        val message = parser.fromJson("{\"some\":\"invalid message\"}")
        assertEquals(MessageUnknown::class.java, message.javaClass)
    }

    @Test(expected = JsonParseException::class)
    @Throws(Exception::class)
    fun parserThrowsCorrectExceptionWhenGivenInvalidJSON() {
        val parser = Parser(encryptionProvider)
        parser.fromJson("not JSON")
    }
}