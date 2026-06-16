package org.owntracks.android.model

import java.io.ByteArrayInputStream
import java.io.IOException
import kotlin.time.Instant
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.boolean
import kotlinx.serialization.json.double
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.long
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.mockito.ArgumentMatchers.anyString
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.spy
import org.mockito.kotlin.whenever
import org.owntracks.android.location.geofencing.Geofence
import org.owntracks.android.model.Parser.EncryptionException
import org.owntracks.android.model.fixtures.MessageFixtures
import org.owntracks.android.model.messages.AddMessageStatus
import org.owntracks.android.model.messages.MessageCard
import org.owntracks.android.model.messages.MessageClear
import org.owntracks.android.model.messages.MessageCmd
import org.owntracks.android.model.messages.MessageConfiguration
import org.owntracks.android.model.messages.MessageCreatedAtNow
import org.owntracks.android.model.messages.MessageLocation
import org.owntracks.android.model.messages.MessageStatus
import org.owntracks.android.model.messages.MessageTransition
import org.owntracks.android.model.messages.MessageUnknown
import org.owntracks.android.model.messages.MessageWaypoint
import org.owntracks.android.preferences.Preferences
import org.owntracks.android.preferences.types.MonitoringMode
import org.owntracks.android.preferences.types.MqttQos
import kotlin.time.ExperimentalTime

@OptIn(ExperimentalTime::class)
class ParserTest {
  private val testId = "dummyTestId"
  private val extendedMessageLocation =
      MessageLocation(MessageCreatedAtNow(FakeFixedClock())).apply {
        accuracy = 10
        altitude = 20
        latitude = 50.1
        longitude = 60.2
        timestamp = 123456789
        bearing = 56
        velocity = 5.6.toInt()
        verticalAccuracy = 1.7.toInt()
        messageId = testId
        inregions = listOf("Testregion1", "Testregion2")
        battery = 30
        batteryStatus = BatteryStatus.CHARGING
        bssid = "12:34:56:78"
        conn = "TestConn"
        monitoringMode = MonitoringMode.Significant
        ssid = "Wifi SSID"
      }
  private val messageLocation =
      MessageLocation(MessageCreatedAtNow(FakeFixedClock())).apply {
        accuracy = 10
        altitude = 20
        latitude = 50.1
        messageId = testId
        longitude = 60.2
        timestamp = 123456789
        bearing = 56
        velocity = 5.6.toInt()
        verticalAccuracy = 1.7.toInt()
        inregions = listOf("Testregion1", "Testregion2")
      }

  // Going to need a way of getting JSON strings into trees
  private val json = Json { ignoreUnknownKeys = true }

  private val testPreferences: Preferences = mock {
    on { encryptionKey } doReturn ""
    on { pubTopicLocations } doReturn "owntracks/testUsername/testDevice"
    on { pubQosLocations } doReturn MqttQos.One
  }

  private val encryptionProvider = spy(EncryptionProvider(testPreferences))

  @Test
  fun `Parser can serialize extended location message to a pretty JSON message`() {
    val parser = Parser(null)

    val expected =
        """
        {
            "_type": "location",
            "created_at": 25,
            "_id": "dummyTestId",
            "batt": 30,
            "bs": 2,
            "acc": 10,
            "vac": 1,
            "lat": 50.1,
            "lon": 60.2,
            "alt": 20,
            "vel": 5,
            "cog": 56,
            "tst": 123456789,
            "m": 1,
            "conn": "TestConn",
            "inregions": [
                "Testregion1",
                "Testregion2"
            ],
            "BSSID": "12:34:56:78",
            "SSID": "Wifi SSID"
        }
        """
            .trimIndent()
    assertEquals(expected, parser.toUnencryptedJsonPretty(extendedMessageLocation))
  }

  @Test
  fun `Parser can serialize non-extended location message to a pretty JSON message`() {
    val parser = Parser(null)

    val expected =
        """
        {
            "_type": "location",
            "created_at": 25,
            "_id": "dummyTestId",
            "acc": 10,
            "vac": 1,
            "lat": 50.1,
            "lon": 60.2,
            "alt": 20,
            "vel": 5,
            "cog": 56,
            "tst": 123456789,
            "inregions": [
                "Testregion1",
                "Testregion2"
            ]
        }
        """
            .trimIndent()
    // language=JSON
    assertEquals(expected, parser.toUnencryptedJsonPretty(messageLocation))
  }

  @Test
  fun `Parser can deserialize a location message`() {

    val parser = Parser(encryptionProvider)

    val input = MessageFixtures.LOCATION_DESERIALIZE_EXTENDED
    val messageBase = parser.fromJson(input)
    assertEquals(MessageLocation::class.java, messageBase.javaClass)
    val message = messageBase as MessageLocation
    assertEquals(1514455575L, message.timestamp)
    assertEquals("s5", message.trackerId)
    assertEquals(1600, message.accuracy.toLong())
    assertEquals(0.0, message.altitude.toDouble(), 0.0)
    assertEquals(99, message.battery)
    assertEquals(BatteryStatus.FULL, message.batteryStatus)
    assertEquals("w", message.conn)
    assertEquals(52.3153748, message.latitude, 0.0)
    assertEquals(5.0408462, message.longitude, 0.0)
    assertEquals(56, message.bearing)
    assertEquals(MessageLocation.ReportType.PING, message.trigger)
    assertEquals(0f, message.verticalAccuracy.toFloat(), 0f)
    assertEquals(2, message.inregions?.size)
    assertEquals("inputTestId", message.messageId)
  }

  @Test
  fun `a location message with a timer trigger can be parsed`() {

    val parser = Parser(encryptionProvider)
    val input = MessageFixtures.LOCATION_TIMER_TRIGGER
    val messageBase = parser.fromJson(input)
    assertEquals(MessageLocation::class.java, messageBase.javaClass)
    val message = messageBase as MessageLocation
    assertEquals(MessageLocation.ReportType.TIMER, message.trigger)
  }

  @Test
  fun `a location message with a beacon trigger can be parsed`() {

    val parser = Parser(encryptionProvider)
    val input = MessageFixtures.LOCATION_BEACON_TRIGGER
    val messageBase = parser.fromJson(input)
    assertEquals(MessageLocation::class.java, messageBase.javaClass)
    val message = messageBase as MessageLocation
    assertEquals(MessageLocation.ReportType.BEACON, message.trigger)
  }

  @Test
  fun `Parser can serialize a location message`() {
    val parser = Parser(encryptionProvider)
    val input = extendedMessageLocation
    val serialized = input.toJson(parser)!!
    val jsonElement = json.parseToJsonElement(serialized).jsonObject
    assertTrue(jsonElement.isNotEmpty())
    assertEquals("location", jsonElement["_type"]?.jsonPrimitive?.content)
    assertEquals("12:34:56:78", jsonElement["BSSID"]?.jsonPrimitive?.content)
    assertEquals("Wifi SSID", jsonElement["SSID"]?.jsonPrimitive?.content)
    assertEquals(10, jsonElement["acc"]?.jsonPrimitive?.int)
    assertEquals(20, jsonElement["alt"]?.jsonPrimitive?.int)
    assertEquals(30, jsonElement["batt"]?.jsonPrimitive?.int)
    assertEquals(2, jsonElement["bs"]?.jsonPrimitive?.int)
    assertEquals(56, jsonElement["cog"]?.jsonPrimitive?.int)
    assertEquals("TestConn", jsonElement["conn"]?.jsonPrimitive?.content)
    assertEquals(25, jsonElement["created_at"]?.jsonPrimitive?.int)
    assertTrue(jsonElement["inregions"]?.jsonArray?.size == 2)
    assertEquals("Testregion1", jsonElement["inregions"]?.jsonArray?.get(0)?.jsonPrimitive?.content)
    assertEquals("Testregion2", jsonElement["inregions"]?.jsonArray?.get(1)?.jsonPrimitive?.content)
    assertEquals(50.1, jsonElement["lat"]?.jsonPrimitive?.double!!, 0.0001)
    assertEquals(60.2, jsonElement["lon"]?.jsonPrimitive?.double!!, 0.0001)
    assertEquals(1, jsonElement["m"]?.jsonPrimitive?.int)
    assertEquals(123456789L, jsonElement["tst"]?.jsonPrimitive?.long)
    assertEquals(1, jsonElement["vac"]?.jsonPrimitive?.int)
    assertEquals(5, jsonElement["vel"]?.jsonPrimitive?.int)
  }

  @Test
  fun `Parser can serialize a location message with the topic visible`() {
    val parser = Parser(encryptionProvider)
    val input = extendedMessageLocation
    input.annotateFromPreferences(testPreferences)
    input.setTopicVisible()
    val serialized = input.toJson(parser)!!
    val jsonElement = json.parseToJsonElement(serialized).jsonObject
    assertTrue(jsonElement.isNotEmpty())
    assertEquals("location", jsonElement["_type"]?.jsonPrimitive?.content)
    assertEquals("12:34:56:78", jsonElement["BSSID"]?.jsonPrimitive?.content)
    assertEquals("Wifi SSID", jsonElement["SSID"]?.jsonPrimitive?.content)
    assertEquals(10, jsonElement["acc"]?.jsonPrimitive?.int)
    assertEquals(20, jsonElement["alt"]?.jsonPrimitive?.int)
    assertEquals(30, jsonElement["batt"]?.jsonPrimitive?.int)
    assertEquals(2, jsonElement["bs"]?.jsonPrimitive?.int)
    assertEquals(56, jsonElement["cog"]?.jsonPrimitive?.int)
    assertEquals("TestConn", jsonElement["conn"]?.jsonPrimitive?.content)
    assertEquals(25, jsonElement["created_at"]?.jsonPrimitive?.int)
    assertTrue(jsonElement["inregions"]?.jsonArray?.size == 2)
    assertEquals("Testregion1", jsonElement["inregions"]?.jsonArray?.get(0)?.jsonPrimitive?.content)
    assertEquals("Testregion2", jsonElement["inregions"]?.jsonArray?.get(1)?.jsonPrimitive?.content)
    assertEquals(50.1, jsonElement["lat"]?.jsonPrimitive?.double!!, 0.0001)
    assertEquals(60.2, jsonElement["lon"]?.jsonPrimitive?.double!!, 0.0001)
    assertEquals(1, jsonElement["m"]?.jsonPrimitive?.int)
    assertEquals("owntracks/testUsername/testDevice", jsonElement["topic"]?.jsonPrimitive?.content)
    assertEquals(123456789L, jsonElement["tst"]?.jsonPrimitive?.long)
    assertEquals(1, jsonElement["vac"]?.jsonPrimitive?.int)
    assertEquals(5, jsonElement["vel"]?.jsonPrimitive?.int)
  }

  @Test
  fun `Parser can deserialize an encrypted location message`() {
    doReturn(true).whenever(encryptionProvider).isPayloadEncryptionEnabled
    val parser = Parser(encryptionProvider)
    val messageLocationJSON = MessageFixtures.LOCATION_ENCRYPTED_DECRYPTED
    doReturn(messageLocationJSON).whenever(encryptionProvider).decrypt("TestCipherText")

    val input = MessageFixtures.ENCRYPTED_MESSAGE
    val messageBase = parser.fromJson(input)
    assertEquals(MessageLocation::class.java, messageBase.javaClass)
    val messageLocation = messageBase as MessageLocation
    assertEquals(1514455575L, messageLocation.timestamp)
    assertEquals("s5", messageLocation.trackerId)
    assertEquals(1600, messageLocation.accuracy.toLong())
    assertEquals(0.0, messageLocation.altitude.toDouble(), 0.0)
    assertEquals(99, messageLocation.battery)
    assertEquals(56, messageLocation.bearing)
    assertEquals("w", messageLocation.conn)
    assertEquals(52.3153748, messageLocation.latitude, 0.0)
    assertEquals(5.0408462, messageLocation.longitude, 0.0)
    assertEquals(MessageLocation.ReportType.PING, messageLocation.trigger)
    assertEquals(0f, messageLocation.verticalAccuracy.toFloat(), 0f)
    assertEquals(2f, messageLocation.velocity.toFloat(), 0f)
  }

  @Test
  fun `Parser can serialize an encrypted location message`() {
    val dummyCipherText = "TestCipherText"
    doReturn(true).whenever(encryptionProvider).isPayloadEncryptionEnabled
    doReturn(dummyCipherText).whenever(encryptionProvider).encrypt(anyString())
    val parser = Parser(encryptionProvider)
    val input = extendedMessageLocation
    val serialized = input.toJson(parser)!!
    val jsonElement = json.parseToJsonElement(serialized).jsonObject
    assertTrue(jsonElement.isNotEmpty())
    assertEquals("encrypted", jsonElement["_type"]?.jsonPrimitive?.content)
    assertEquals(dummyCipherText, jsonElement["data"]?.jsonPrimitive?.content)
  }

  @Test(expected = EncryptionException::class)
  fun `Parser should raise an exception when given an encrypted message with encryption disabled`() {
    doReturn(false).whenever(encryptionProvider).isPayloadEncryptionEnabled
    val parser = Parser(encryptionProvider)

    val input = MessageFixtures.ENCRYPTED_MESSAGE
    parser.fromJson(input)
  }

  @Test
  fun `Parser can deserialize multiple location messages in same document`() {
    val multipleMessageLocationJSON = MessageFixtures.LOCATION_MULTIPLE_MESSAGES
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

  // endregion

  // region Command Messages
  @Test
  fun `Parser can deserialize a reportLocation cmd message`() {
    val parser = Parser(encryptionProvider)

    val input = MessageFixtures.CMD_REPORT_LOCATION
    val messageBase = parser.fromJson(input)
    messageBase.topic = "owntracks/username/device/cmd"
    assertEquals(MessageCmd::class.java, messageBase.javaClass)
    val messageCmd = messageBase as MessageCmd
    assertTrue(messageCmd.isValidMessage())
    assertEquals(CommandAction.REPORT_LOCATION, messageCmd.action)
    assertEquals("owntracks/username/device", messageCmd.getContactId())
  }

  @Test
  fun `Parser can deserialize a setWaypoints cmd message`() {
    val parser = Parser(encryptionProvider)

    val input = MessageFixtures.CMD_SET_WAYPOINTS
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

    val input = MessageFixtures.CMD_SET_CONFIGURATION
    val messageBase = parser.fromJson(input)
    assertEquals(MessageCmd::class.java, messageBase.javaClass)
    val messageCmd = messageBase as MessageCmd
    assertTrue(messageCmd.isValidMessage())
    assertEquals(CommandAction.SET_CONFIGURATION, messageCmd.action)
    assertEquals("newHost", messageCmd.configuration!!["host"])
  }

  @Test
  fun `Parser can deserialize a status cmd message`() {
    val parser = Parser(encryptionProvider)

    val input = MessageFixtures.CMD_STATUS
    val messageBase = parser.fromJson(input)
    messageBase.topic = "owntracks/username/device/cmd"
    assertEquals(MessageCmd::class.java, messageBase.javaClass)
    val messageCmd = messageBase as MessageCmd
    assertTrue(messageCmd.isValidMessage())
    assertEquals(CommandAction.STATUS, messageCmd.action)
    assertEquals("owntracks/username/device", messageCmd.getContactId())
  }

  @Test(expected = SerializationException::class)
  fun `Parser throws exception when given cmd with invalid action`() {
    val parser = Parser(encryptionProvider)

    val input = MessageFixtures.CMD_INVALID_ACTION
    parser.fromJson(input)
  }

  // endregion

  // region Transition Messages
  @Test
  fun `Parser can deserialize a transition message`() {
    val parser = Parser(encryptionProvider)

    val input = MessageFixtures.TRANSITION_MESSAGE
    val messageBase = parser.fromJson(input)
    messageBase.topic = "owntracks/username/device/event"
    assertEquals(MessageTransition::class.java, messageBase.javaClass)
    val message = messageBase as MessageTransition
    assertTrue(message.isValidMessage())
    assertEquals(2, message.getTransition())
    assertEquals(3, message.accuracy)
    assertEquals("myregion", message.description)
    assertEquals(52.71234, message.latitude, 0.0)
    assertEquals(-1.61234123, message.longitude, 0.0)
    assertEquals(1603209966, message.timestamp)
    assertEquals(1558351273, message.waypointTimestamp)
    assertEquals("ce", message.trackerId)
    assertEquals("l", message.trigger)
    assertEquals("owntracks/username/device", message.getContactId())
  }

  @Test
  fun `Parser can serialize a transition message`() {
    val parser = Parser(encryptionProvider)
    val message =
        MessageTransition().apply {
          latitude = 52.71234
          longitude = -1.61234123
          description = "myregion"
          setTransition(Geofence.GEOFENCE_TRANSITION_EXIT)
          accuracy = 4
          timestamp = 1603209966
          waypointTimestamp = 1558351273
          trackerId = "ce"
          trigger = "l"
        }
    val serialized = message.toJson(parser)!!
    val jsonNode = json.parseToJsonElement(serialized).jsonObject
    assertTrue(jsonNode.isNotEmpty())
    assertEquals("transition", jsonNode["_type"]?.jsonPrimitive?.content)
    assertEquals(message.accuracy, jsonNode["acc"]?.jsonPrimitive?.int)
    assertEquals(message.description, jsonNode["desc"]?.jsonPrimitive?.content)
    assertEquals("leave", jsonNode["event"]?.jsonPrimitive?.content)
    assertEquals(message.latitude, jsonNode["lat"]?.jsonPrimitive?.double!!, 0.001)
    assertEquals(message.longitude, jsonNode["lon"]?.jsonPrimitive?.double!!, 0.001)
    assertEquals(message.trigger, jsonNode["t"]?.jsonPrimitive?.content)
    assertEquals(message.trackerId, jsonNode["tid"]?.jsonPrimitive?.content)
    assertEquals(message.timestamp, jsonNode["tst"]?.jsonPrimitive?.long)
    assertEquals(message.waypointTimestamp, jsonNode["wtst"]?.jsonPrimitive?.long)
  }

  // endregion

  // region Configuration messages
  @Test
  fun `Parser can deserialize a configuration message`() {
    val parser = Parser(encryptionProvider)

    val input = MessageFixtures.CONFIGURATION_MESSAGE
    val messageBase = parser.fromJson(input)
    assertEquals(MessageConfiguration::class.java, messageBase.javaClass)
    val message = messageBase as MessageConfiguration
    assertTrue(message.isValidMessage())
    assertFalse(message.waypoints.isEmpty())
    assertEquals(2, message.waypoints.size)
    assertEquals(true, message["autostartOnBoot"])
    assertEquals(5, message["locatorDisplacement"])
  }

  @Test
  fun `Parser can serialize a configuration message`() {
    val parser = Parser(encryptionProvider)
    val message = MessageConfiguration()
    message["TestBoolKey"] = true
    message["TestStringKey"] = "testString"
    message["TestIntKey"] = 13487
    message["TestFloatKey"] = 13487f
    val waypoint =
        MessageWaypoint().apply {
          latitude = 51.0
          longitude = -20.0
          description = "Test waypoint"
          radius = 45
          timestamp = 123456789
        }
    message.waypoints.add(waypoint)
    val serialized = message.toJson(parser)!!
    val jsonNode = json.parseToJsonElement(serialized).jsonObject

    assertTrue(jsonNode.isNotEmpty())
    assertEquals("configuration", jsonNode["_type"]?.jsonPrimitive?.content)
    assertTrue(jsonNode["waypoints"]?.jsonArray?.isNotEmpty() == true)
    assertEquals(1, jsonNode["waypoints"]?.jsonArray?.size)
    assertTrue(jsonNode["waypoints"]?.jsonArray?.get(0)?.jsonObject?.isNotEmpty() == true)
    assertEquals(
        "waypoint",
        jsonNode["waypoints"]?.jsonArray?.get(0)?.jsonObject?.get("_type")?.jsonPrimitive?.content)
    assertEquals(
        "Test waypoint",
        jsonNode["waypoints"]?.jsonArray?.get(0)?.jsonObject?.get("desc")?.jsonPrimitive?.content)
    assertEquals(
        51.0,
        jsonNode["waypoints"]?.jsonArray?.get(0)?.jsonObject?.get("lat")?.jsonPrimitive?.double!!,
        0.00001)
    assertEquals(
        -20.0,
        jsonNode["waypoints"]?.jsonArray?.get(0)?.jsonObject?.get("lon")?.jsonPrimitive?.double!!,
        0.00001)
    assertEquals(
        45, jsonNode["waypoints"]?.jsonArray?.get(0)?.jsonObject?.get("rad")?.jsonPrimitive?.int)
    assertEquals(
        123456789,
        jsonNode["waypoints"]?.jsonArray?.get(0)?.jsonObject?.get("tst")?.jsonPrimitive?.int)
    assertTrue(jsonNode["TestBoolKey"]?.jsonPrimitive?.boolean ?: false)
    assertEquals(13487.0, jsonNode["TestFloatKey"]?.jsonPrimitive?.double ?: 0.0, 0.0001)
    assertEquals(13487, jsonNode["TestIntKey"]?.jsonPrimitive?.int)
    assertEquals("testString", jsonNode["TestStringKey"]?.jsonPrimitive?.content)
  }

  // endregion

  // region Waypoint Messages
  @Test
  fun `Parser can deserialize a waypoint message`() {
    val parser = Parser(encryptionProvider)

    val input = MessageFixtures.WAYPOINT_MESSAGE
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
    val parser = Parser(encryptionProvider)
    val message =
        MessageWaypoint().apply {
          latitude = 52.0027789
          longitude = -1.0829312
          description = "mypoint"
          radius = 150
          timestamp = 1558351273
        }
    val serialized = message.toJson(parser)!!
    val jsonNode = json.parseToJsonElement(serialized).jsonObject
    assertTrue(jsonNode.isNotEmpty())
    assertEquals("waypoint", jsonNode["_type"]?.jsonPrimitive?.content)
    assertEquals("mypoint", jsonNode["desc"]?.jsonPrimitive?.content)
    assertEquals(message.latitude, jsonNode["lat"]?.jsonPrimitive?.double!!, 0.00001)
    assertEquals(message.longitude, jsonNode["lon"]?.jsonPrimitive?.double!!, 0.00001)
    assertEquals(message.radius, jsonNode["rad"]?.jsonPrimitive?.int)
    assertEquals(message.timestamp, jsonNode["tst"]?.jsonPrimitive?.long)
  }

  // endregion

  // region Status Messages
  @Test
  fun `Parser can serialize a status message`() {
    doReturn(false).whenever(encryptionProvider).isPayloadEncryptionEnabled
    val parser = Parser(encryptionProvider)
    val message =
        MessageStatus().apply {
          android =
              AddMessageStatus().apply {
                wifistate = 1
                powerSave = 1
                batteryOptimizations = 1
                appHibernation = 1
                locationPermission = -3
              }
        }
    val serialized = message.toJson(parser)!!
    val jsonNode = json.parseToJsonElement(serialized).jsonObject
    assertTrue(jsonNode.isNotEmpty())
    assertEquals("status", jsonNode["_type"]?.jsonPrimitive?.content)
    assertEquals(
        message.android?.wifistate,
        jsonNode["android"]?.jsonObject?.get("wifi")?.jsonPrimitive?.int)
    assertEquals(
        message.android?.powerSave, jsonNode["android"]?.jsonObject?.get("ps")?.jsonPrimitive?.int)
    assertEquals(
        message.android?.batteryOptimizations,
        jsonNode["android"]?.jsonObject?.get("bo")?.jsonPrimitive?.int)
    assertEquals(
        message.android?.appHibernation,
        jsonNode["android"]?.jsonObject?.get("hib")?.jsonPrimitive?.int)
    assertEquals(
        message.android?.locationPermission,
        jsonNode["android"]?.jsonObject?.get("loc")?.jsonPrimitive?.int)
  }

  // endregion

  // region Clear Messages
  @Test
  fun `Parser can serialize a clear messages`() {
    val parser = Parser(encryptionProvider)
    val message = MessageClear()
    val serialized = message.toJson(parser)
    assertEquals("", serialized)
  }

  @Test
  fun `Parser can serialize a clear messages to a byte array`() {
    val parser = Parser(encryptionProvider)
    val message = MessageClear()
    val serialized = message.toJsonBytes(parser)
    assertEquals(0, serialized.size)
  }

  // endregion

  // Card Messages
  @Test
  fun `Parser can deserialize a MessageCard`() {
    val parser = Parser(encryptionProvider)

    val input = MessageFixtures.CARD_MESSAGE_WITH_FACE
    val messageBase = parser.fromJson(input)
    messageBase.topic = "owntracks/user/device/info"
    assertEquals(MessageCard::class.java, messageBase.javaClass)
    val messageCard = messageBase as MessageCard
    assertTrue(messageCard.isValidMessage())
    assertEquals("MyName!", messageCard.name)
    assertEquals("owntracks/user/device", messageCard.getContactId())
  }

  @Test
  fun `Parser can deserialize a MessageCard with a trackerId field`() {
    val parser = Parser(encryptionProvider)
    val input = MessageFixtures.CARD_MESSAGE_WITH_TID
    val messageBase = parser.fromJson(input)
    assertEquals(MessageCard::class.java, messageBase.javaClass)
    val messageCard = messageBase as MessageCard
    assertTrue(messageCard.isValidMessage())
    assertEquals("MyName!", messageCard.name)
    assertEquals("overridden-topic", messageCard.trackerId)
  }

  // endregion

  // region  Invalid messages
  @Test(expected = IOException::class)
  fun `Parser should throw exception given an empty array`() {
    val parser = Parser(encryptionProvider)
    val byteArrayInputStream = ByteArrayInputStream(ByteArray(0))
    parser.fromJson(byteArrayInputStream)
  }

  @Test
  fun `Parser can deserialize an Unknown message`() {
    val parser = Parser(encryptionProvider)

    val message = parser.fromJson(MessageFixtures.UNKNOWN_MESSAGE)
    assertEquals(MessageUnknown::class.java, message.javaClass)
  }

  @Test(expected = SerializationException::class)
  fun `Parser throws exception when given invalid JSON as message body`() {
    val parser = Parser(encryptionProvider)
    parser.fromJson("not JSON")
  }

  // endregion

  @Test
  fun `Given a serialized MessageLocation, when deserializing then the created_at field is correctly parsed`() {
    val msg = MessageFixtures.LOCATION_WITH_CREATED_AT
    val parser = Parser(encryptionProvider)
    val parsed = parser.fromJson(msg) as MessageLocation
    assertEquals(Instant.parse("2024-02-27T12:30:52Z"), parsed.createdAt)
  }

  @Test
  fun `Parser can deserialize a minimal valid location message with tracker id`() {
    val parser = Parser(encryptionProvider)
    val input = MessageFixtures.VALID_LOCATION_MESSAGE
    val message = parser.fromJson(input) as MessageLocation
    assertEquals(1778266175L, message.timestamp)
    assertEquals("xx", message.trackerId)
    assertEquals(0.0, message.latitude, 0.0)
    assertEquals(0.0, message.longitude, 0.0)
    assertEquals(15, message.accuracy)
    assertEquals(300, message.altitude)
    assertEquals(98, message.battery)
    assertTrue(message.isValidMessage())
  }

  @Test
  fun `Parser can deserialize a minimal valid location message with topic`() {
    val parser = Parser(encryptionProvider)
    val input = MessageFixtures.VALID_LOCATION_MESSAGE_WITH_TOPIC
    val message = parser.fromJson(input) as MessageLocation
    assertEquals(1778266175L, message.timestamp)
    assertEquals(0.0, message.latitude, 0.0)
    assertEquals(0.0, message.longitude, 0.0)
    assertEquals("owntracks/user/device", message.visibleTopic)
    assertEquals(15, message.accuracy)
    assertEquals(300, message.altitude)
    assertTrue(message.isValidMessage())
  }

  @Test(expected = SerializationException::class)
  fun `Parser can deserialize an empty location message without crashing`() {
    val parser = Parser(encryptionProvider)
    val input: String = MessageFixtures.LOCATION_EMPTY
    parser.fromJson(input) as MessageLocation
  }

  @Test(expected = SerializationException::class)
  fun `Parser can deserialize a location message with zero timestamp and marks it invalid`() {
    val parser = Parser(encryptionProvider)
    val input: String = MessageFixtures.INVALID_LOCATION_MESSAGE_ZERO_TIMESTAMP
    parser.fromJson(input) as MessageLocation
  }

  @Test(expected = SerializationException::class)
  fun `Parser can deserialize a location message missing tracker id and topic and marks it invalid`() {
    val parser = Parser(encryptionProvider)
    val input: String = MessageFixtures.INVALID_LOCATION_MESSAGE_MISSING_TID_AND_TOPIC
    parser.fromJson(input) as MessageLocation
  }
}
