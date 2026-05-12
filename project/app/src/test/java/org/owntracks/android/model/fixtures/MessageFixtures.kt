package org.owntracks.android.model.fixtures

import java.io.FileNotFoundException

object MessageFixtures {
  fun getFixtureContent(filename: String): String {
    val resource =
        this::class.java.classLoader?.getResourceAsStream("fixtures/$filename")
            ?: throw FileNotFoundException("Fixture not found: fixtures/$filename")
    return resource.bufferedReader().use { it.readText() }
  }

  // Location messages
  val LOCATION_EMPTY: String
    get() = getFixtureContent("location_empty.json")

  val VALID_LOCATION_MESSAGE: String
    get() = getFixtureContent("valid_location_message.json")

  val VALID_LOCATION_MESSAGE_WITH_TOPIC: String
    get() = getFixtureContent("valid_location_message_with_topic.json")

  val INVALID_LOCATION_MESSAGE_ZERO_TIMESTAMP: String
    get() = getFixtureContent("invalid_location_message_zero_timestamp.json")

  val INVALID_LOCATION_MESSAGE_MISSING_TID_AND_TOPIC: String
    get() = getFixtureContent("invalid_location_message_missing_tid_and_topic.json")

  val LOCATION_DESERIALIZE_EXTENDED: String
    get() = getFixtureContent("location_deserialize_extended.json")

  val LOCATION_TIMER_TRIGGER: String
    get() = getFixtureContent("location_timer_trigger.json")

  val LOCATION_BEACON_TRIGGER: String
    get() = getFixtureContent("location_beacon_trigger.json")

  val LOCATION_ENCRYPTED_DECRYPTED: String
    get() = getFixtureContent("location_encrypted_decrypted.json")

  val LOCATION_MULTIPLE_MESSAGES: String
    get() = getFixtureContent("location_multiple_messages.json")

  val LOCATION_WITH_CREATED_AT: String
    get() = getFixtureContent("location_with_created_at.json")

  // Encrypted messages
  val ENCRYPTED_MESSAGE: String
    get() = getFixtureContent("encrypted_message.json")

  // Cmd messages
  val CMD_REPORT_LOCATION: String
    get() = getFixtureContent("cmd_report_location.json")

  val CMD_SET_WAYPOINTS: String
    get() = getFixtureContent("cmd_set_waypoints.json")

  val CMD_SET_CONFIGURATION: String
    get() = getFixtureContent("cmd_set_configuration.json")

  val CMD_STATUS: String
    get() = getFixtureContent("cmd_status.json")

  val CMD_INVALID_ACTION: String
    get() = getFixtureContent("cmd_invalid_action.json")

  // Transition messages
  val TRANSITION_MESSAGE: String
    get() = getFixtureContent("transition_message.json")

  // Configuration messages
  val CONFIGURATION_MESSAGE: String
    get() = getFixtureContent("configuration_message.json")

  // Waypoint messages
  val WAYPOINT_MESSAGE: String
    get() = getFixtureContent("waypoint_message.json")

  // Card messages
  val CARD_MESSAGE_WITH_FACE: String
    get() = getFixtureContent("card_message_with_face.json")

  val CARD_MESSAGE_WITH_TID: String
    get() = getFixtureContent("card_message_with_tid.json")

  val VALID_CARD_MESSAGE: String
    get() = getFixtureContent("valid_card_message.json")

  // Unknown/Invalid messages
  val UNKNOWN_MESSAGE: String
    get() = getFixtureContent("unknown_message.json")
}
