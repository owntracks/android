package org.owntracks.android.mqtt

import androidx.core.content.edit
import androidx.preference.PreferenceManager
import androidx.test.espresso.Espresso
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import com.adevinta.android.barista.assertion.BaristaListAssertions.assertDisplayedAtPosition
import com.adevinta.android.barista.assertion.BaristaRecyclerViewAssertions.assertRecyclerViewItemCount
import com.adevinta.android.barista.assertion.BaristaVisibilityAssertions.assertDisplayed
import com.adevinta.android.barista.assertion.BaristaVisibilityAssertions.assertNotDisplayed
import com.adevinta.android.barista.interaction.BaristaClickInteractions.clickOn
import com.adevinta.android.barista.interaction.BaristaDrawerInteractions.openDrawer
import dagger.hilt.android.testing.HiltAndroidTest
import mqtt.packets.Qos
import mqtt.packets.mqtt.MQTTPublish
import mqtt.packets.mqttv5.MQTT5Properties
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.owntracks.android.R
import org.owntracks.android.model.CommandAction
import org.owntracks.android.model.Parser
import org.owntracks.android.model.messages.MessageCmd
import org.owntracks.android.model.messages.MessageConfiguration
import org.owntracks.android.model.messages.MessageLocation
import org.owntracks.android.model.messages.MessageStatus
import org.owntracks.android.model.messages.MessageUnknown
import org.owntracks.android.model.messages.MessageWaypoint
import org.owntracks.android.model.messages.MessageWaypoints
import org.owntracks.android.preferences.Preferences
import org.owntracks.android.support.MessageWaypointCollection
import org.owntracks.android.testutils.TestWithAnActivity
import org.owntracks.android.testutils.TestWithAnMQTTBroker
import org.owntracks.android.testutils.TestWithAnMQTTBrokerImpl
import org.owntracks.android.testutils.addWaypoint
import org.owntracks.android.testutils.di.setLocation
import org.owntracks.android.testutils.getPreferences
import org.owntracks.android.testutils.use
import org.owntracks.android.ui.map.MapActivity

@OptIn(ExperimentalUnsignedTypes::class)
@LargeTest
@RunWith(AndroidJUnit4::class)
@HiltAndroidTest
class MQTTRemoteCommandTests :
    TestWithAnActivity<MapActivity>(false), TestWithAnMQTTBroker by TestWithAnMQTTBrokerImpl() {

  @Test
  fun given_an_mqtt_configured_client_when_the_broker_sends_an_invalid_command_message_then_nothing_happens() {
    setupTestActivity({
      configureMQTTConnectionToLocalWithGeneratedPassword(saveConfigurationIdlingResource)
    })
    messageReceivedIdlingResource.add(MessageUnknown)
    listOf(
            // language=JSON
            """
            {
              "_type": "cmd",
              "action": "invalid"
            }
            """
                .trimIndent(),
        )
        .forEach {
          broker.publish(
              false,
              "owntracks/$mqttUsername/$deviceId/cmd",
              Qos.AT_LEAST_ONCE,
              MQTT5Properties(),
              it.toByteArray().toUByteArray(),
          )
        }
    messageReceivedIdlingResource.use {
      Espresso.onIdle()
      assertDisplayed(R.id.fabMyLocation)
    }
  }

  @Test
  fun given_an_mqtt_configured_client_when_the_broker_sends_a_reportlocation_command_message_then_a_response_location_is_sent_back_to_the_broker() {
    setupTestActivity({
      configureMQTTConnectionToLocalWithGeneratedPassword(saveConfigurationIdlingResource)
    })

    reportLocationFromMap(mockLocationIdlingResource) {
      mockLocationProviderClient.setLocation(52.0, 0.0)
    }
    clickOn(R.id.menu_monitoring)
    clickOn(R.id.fabMonitoringModeSignificantChanges)

    baristaRule.activityTestRule.activity.publishResponseMessageIdlingResource.setIdleState(false)
    listOf(MessageCmd().apply { action = CommandAction.REPORT_LOCATION })
        .map {
          messageReceivedIdlingResource.add(it)
          it
        }
        .map(Parser(null)::toJsonBytes)
        .forEach {
          broker.publish(
              false,
              "owntracks/$mqttUsername/$deviceId/cmd",
              Qos.AT_LEAST_ONCE,
              MQTT5Properties(),
              it.toUByteArray(),
          )
        }

    baristaRule.activityTestRule.activity.publishResponseMessageIdlingResource.use {
      clickOn(R.id.fabMyLocation)
    }
    baristaRule.activityTestRule.activity.outgoingQueueIdlingResource.use {
      assertTrue(
          mqttPacketsReceived
              .filterIsInstance<MQTTPublish>()
              .map { Parser(null).fromJson((it.payload)!!.toByteArray()) }
              .any { it is MessageLocation && it.trigger == MessageLocation.ReportType.RESPONSE },
      )
    }
  }

  @Test
  fun given_an_mqtt_configured_client_when_the_broker_sends_a_waypoints_response_message_then_a_waypoints_message_is_sent_back_to_the_broker() {
    setupTestActivity({
      configureMQTTConnectionToLocalWithGeneratedPassword(saveConfigurationIdlingResource)
    })

    clickOn(R.id.menu_monitoring)
    clickOn(R.id.fabMonitoringModeSignificantChanges)

    openDrawer()
    clickOn(R.string.title_activity_waypoints)

    addWaypoint("test waypoint", "51.123", "0.456", "20")
    addWaypoint("test waypoint 2", "51.00", "0.4", "25")

    openDrawer()
    clickOn(R.string.title_activity_map)

    baristaRule.activityTestRule.activity.publishResponseMessageIdlingResource.setIdleState(false)
    listOf(MessageCmd().apply { action = CommandAction.WAYPOINTS })
        .map {
          messageReceivedIdlingResource.add(it)
          it
        }
        .map(Parser(null)::toJsonBytes)
        .forEach {
          broker.publish(
              false,
              "owntracks/$mqttUsername/$deviceId/cmd",
              Qos.AT_LEAST_ONCE,
              MQTT5Properties(),
              it.toUByteArray(),
          )
        }

    baristaRule.activityTestRule.activity.publishResponseMessageIdlingResource.use {
      clickOn(R.id.fabMyLocation)
    }
    baristaRule.activityTestRule.activity.outgoingQueueIdlingResource.use {
      assertTrue(
          mqttPacketsReceived
              .filterIsInstance<MQTTPublish>()
              .map { Parser(null).fromJson((it.payload)!!.toByteArray()) }
              .any { it is MessageWaypoints && it.waypoints?.size == 2 },
      )
    }
  }

  @Test
  fun given_an_mqtt_configured_client_when_the_broker_sends_a_set_waypoints_command_message_then_the_waypoints_are_merged_with_the_existing_waypoints() {
    setupTestActivity({
      configureMQTTConnectionToLocalWithGeneratedPassword(saveConfigurationIdlingResource)
    })

    clickOn(R.id.menu_monitoring)
    clickOn(R.id.fabMonitoringModeSignificantChanges)

    openDrawer()
    clickOn(R.string.title_activity_waypoints)

    addWaypoint("test waypoint", "51.123", "0.456", "20")
    addWaypoint("test waypoint 2", "51.00", "0.4", "25")

    openDrawer()
    clickOn(R.string.title_activity_map)

    listOf(
            MessageCmd().apply {
              action = CommandAction.SET_WAYPOINTS
              waypoints =
                  MessageWaypoints().apply {
                    waypoints =
                        MessageWaypointCollection().apply {
                          addAll(
                              listOf(
                                  MessageWaypoint().apply {
                                    description = "location2"
                                    latitude = 51.5
                                    longitude = -0.02
                                    radius = 150
                                    timestamp = 1505910709
                                  },
                                  MessageWaypoint().apply {
                                    description = "home"
                                    latitude = 51.0
                                    longitude = 0.1
                                    radius = 100
                                    timestamp = 15059107010
                                  },
                              ),
                          )
                        }
                  }
            },
        )
        .map {
          messageReceivedIdlingResource.add(it)
          it
        }
        .map(Parser(null)::toJsonBytes)
        .forEach {
          broker.publish(
              false,
              "owntracks/$mqttUsername/$deviceId/cmd",
              Qos.AT_LEAST_ONCE,
              MQTT5Properties(),
              it.toUByteArray(),
          )
        }
    openDrawer()
    clickOn(R.string.title_activity_waypoints)
    assertRecyclerViewItemCount(R.id.waypointsRecyclerView, 4)
    assertDisplayedAtPosition(R.id.waypointsRecyclerView, 0, R.id.title, "test waypoint")
    assertDisplayedAtPosition(R.id.waypointsRecyclerView, 1, R.id.title, "test waypoint 2")
    assertDisplayedAtPosition(R.id.waypointsRecyclerView, 2, R.id.title, "location2")
    assertDisplayedAtPosition(R.id.waypointsRecyclerView, 3, R.id.title, "home")
  }

  @Test
  fun given_an_mqtt_configured_client_when_the_broker_sends_a_clear_waypoints_command_message_then_the_waypoints_are_cleared() {
    setupTestActivity({
      configureMQTTConnectionToLocalWithGeneratedPassword(saveConfigurationIdlingResource)
    })

    clickOn(R.id.menu_monitoring)
    clickOn(R.id.fabMonitoringModeSignificantChanges)

    openDrawer()
    clickOn(R.string.title_activity_waypoints)

    addWaypoint("test waypoint", "51.123", "0.456", "20")
    addWaypoint("test waypoint 2", "51.00", "0.4", "25")

    openDrawer()
    clickOn(R.string.title_activity_map)

    listOf(MessageCmd().apply { action = CommandAction.CLEAR_WAYPOINTS })
        .map {
          messageReceivedIdlingResource.add(it)
          it
        }
        .map(Parser(null)::toJsonBytes)
        .forEach {
          broker.publish(
              false,
              "owntracks/$mqttUsername/$deviceId/cmd",
              Qos.AT_LEAST_ONCE,
              MQTT5Properties(),
              it.toUByteArray(),
          )
        }

    messageReceivedIdlingResource.use { openDrawer() }

    clickOn(R.string.title_activity_waypoints)
    assertNotDisplayed(R.id.waypointsRecyclerView)
  }

  @Test
  fun given_an_mqtt_configured_client_when_the_broker_sends_a_clear_waypoints_command_message_to_the_wrong_topic_then_the_waypoints_are_not_cleared() {
    setupTestActivity({
      configureMQTTConnectionToLocalWithGeneratedPassword(saveConfigurationIdlingResource)
    })

    clickOn(R.id.menu_monitoring)
    clickOn(R.id.fabMonitoringModeSignificantChanges)

    openDrawer()
    clickOn(R.string.title_activity_waypoints)

    addWaypoint("test waypoint", "51.123", "0.456", "20")
    addWaypoint("test waypoint 2", "51.00", "0.4", "25")

    openDrawer()
    clickOn(R.string.title_activity_map)

    listOf(MessageCmd().apply { action = CommandAction.CLEAR_WAYPOINTS })
        .map {
          messageReceivedIdlingResource.add(it)
          it
        }
        .map(Parser(null)::toJsonBytes)
        .forEach {
          broker.publish(
              false,
              "owntracks/$mqttUsername/$deviceId",
              Qos.AT_LEAST_ONCE,
              MQTT5Properties(),
              it.toUByteArray(),
          )
        }

    messageReceivedIdlingResource.use { openDrawer() }

    clickOn(R.string.title_activity_waypoints)
    assertDisplayed(R.id.waypointsRecyclerView)
  }

  @Test
  fun given_an_mqtt_configured_client_when_the_broker_sends_a_setconfiguration_response_message_then_the_configuration_is_set_in_the_preferences() {
    setupTestActivity({
      configureMQTTConnectionToLocalWithGeneratedPassword(saveConfigurationIdlingResource)
    })
    PreferenceManager.getDefaultSharedPreferences(baristaRule.activityTestRule.activity).edit {
      putBoolean("remoteConfiguration", true)
    }
    baristaRule.activityTestRule.activity.importConfigurationIdlingResource.setIdleState(false)
    val testPreference = "TEST_PREFERENCE_VALUE"

    listOf(
            MessageCmd().apply {
              action = CommandAction.SET_CONFIGURATION
              configuration =
                  MessageConfiguration().apply {
                    set(Preferences::opencageApiKey.name, testPreference)
                  }
            },
        )
        .map {
          messageReceivedIdlingResource.add(it)
          it
        }
        .map(Parser(null)::toJsonBytes)
        .forEach {
          broker.publish(
              false,
              "owntracks/$mqttUsername/$deviceId/cmd",
              Qos.AT_LEAST_ONCE,
              MQTT5Properties(),
              it.toUByteArray(),
          )
        }
    baristaRule.activityTestRule.activity.importConfigurationIdlingResource.use {
      clickOn(R.id.fabMyLocation)
    }
    val configuredPreference = getPreferences().getString(Preferences::opencageApiKey.name, "")
    assertEquals(testPreference, configuredPreference)
  }

  @Test
  fun given_an_mqtt_configured_client_to_not_accept_remote_configs_when_the_broker_sends_a_setconfiguration_response_message_then_the_configuration_is_not_set_in_the_preferences() {
    setupTestActivity({
      configureMQTTConnectionToLocalWithGeneratedPassword(saveConfigurationIdlingResource)
    })
    PreferenceManager.getDefaultSharedPreferences(baristaRule.activityTestRule.activity).edit {
      putBoolean("remoteConfiguration", false)
    }
    baristaRule.activityTestRule.activity.importConfigurationIdlingResource.setIdleState(false)
    val testPreference = "TEST_PREFERENCE_VALUE"

    listOf(
            MessageCmd().apply {
              action = CommandAction.SET_CONFIGURATION
              configuration =
                  MessageConfiguration().apply {
                    set(Preferences::opencageApiKey.name, testPreference)
                  }
            },
        )
        .map {
          messageReceivedIdlingResource.add(it)
          it
        }
        .map(Parser(null)::toJsonBytes)
        .forEach {
          broker.publish(
              false,
              "owntracks/$mqttUsername/$deviceId/cmd",
              Qos.AT_LEAST_ONCE,
              MQTT5Properties(),
              it.toUByteArray(),
          )
        }
    baristaRule.activityTestRule.activity.importConfigurationIdlingResource.use {
      clickOn(R.id.fabMyLocation)
    }
    val configuredPreference = getPreferences().getString(Preferences::opencageApiKey.name, "")

    assertEquals("", configuredPreference)
  }

  @Test
  fun given_an_mqtt_configured_client_when_the_broker_sends_a_status_response_message_then_a_status_message_is_sent_back_to_the_broker() {
    setupTestActivity({
      configureMQTTConnectionToLocalWithGeneratedPassword(saveConfigurationIdlingResource)
    })
    reportLocationFromMap(mockLocationIdlingResource) {
      mockLocationProviderClient.setLocation(52.0, 0.0)
    }
    clickOn(R.id.menu_monitoring)
    clickOn(R.id.fabMonitoringModeSignificantChanges)

    baristaRule.activityTestRule.activity.publishResponseMessageIdlingResource.setIdleState(false)
    listOf(MessageCmd().apply { action = CommandAction.STATUS })
        .map {
          messageReceivedIdlingResource.add(it)
          it
        }
        .map(Parser(null)::toJsonBytes)
        .forEach {
          broker.publish(
              false,
              "owntracks/$mqttUsername/$deviceId/cmd",
              Qos.AT_LEAST_ONCE,
              MQTT5Properties(),
              it.toUByteArray(),
          )
        }

    baristaRule.activityTestRule.activity.publishResponseMessageIdlingResource.use {
      clickOn(R.id.fabMyLocation)
    }
    baristaRule.activityTestRule.activity.outgoingQueueIdlingResource.use {
      assertTrue(
          mqttPacketsReceived
              .filterIsInstance<MQTTPublish>()
              .map { Parser(null).fromJson((it.payload)!!.toByteArray()) }
              .any { it is MessageStatus },
      )
    }
  }
}
