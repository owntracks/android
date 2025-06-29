package org.owntracks.android.mqtt

import android.app.Notification
import android.app.NotificationManager
import android.content.Context.NOTIFICATION_SERVICE
import androidx.test.espresso.Espresso
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import com.adevinta.android.barista.interaction.BaristaClickInteractions.clickOn
import com.adevinta.android.barista.interaction.BaristaDrawerInteractions.openDrawer
import dagger.hilt.android.testing.HiltAndroidTest
import java.time.Instant
import mqtt.packets.Qos
import mqtt.packets.mqtt.MQTTPublish
import mqtt.packets.mqttv5.MQTT5Properties
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.owntracks.android.R
import org.owntracks.android.model.Parser
import org.owntracks.android.model.messages.MessageCard
import org.owntracks.android.model.messages.MessageLocation
import org.owntracks.android.model.messages.MessageTransition
import org.owntracks.android.testutils.TestWithAnActivity
import org.owntracks.android.testutils.TestWithAnMQTTBroker
import org.owntracks.android.testutils.TestWithAnMQTTBrokerImpl
import org.owntracks.android.testutils.addWaypoint
import org.owntracks.android.testutils.di.setLocation
import org.owntracks.android.testutils.disableHeadsupNotifications
import org.owntracks.android.testutils.stopAndroidSetupProcess
import org.owntracks.android.testutils.use
import org.owntracks.android.ui.map.MapActivity
import timber.log.Timber

@ExperimentalUnsignedTypes
@LargeTest
@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class MQTTTransitionEventTests :
    TestWithAnActivity<MapActivity>(false), TestWithAnMQTTBroker by TestWithAnMQTTBrokerImpl() {

  @Before
  fun clearLocalData() {
    app.filesDir.listFiles()?.forEach { it.delete() }
  }

  @Before
  fun stopAndroidSetup() {
    stopAndroidSetupProcess()
    disableHeadsupNotifications()
  }

  @Test
  fun given_an_mqtt_configured_client_when_the_broker_sends_a_transition_message_then_a_notification_appears() {
    setupTestActivity(
        {
          configureMQTTConnectionToLocalWithGeneratedPassword(
              saveConfigurationIdlingResource,
          )
        },
    )

    listOf(
            MessageLocation().apply {
              latitude = 52.123
              longitude = 0.56789
              trackerId = "tt"
              timestamp = Instant.parse("2006-01-02T15:04:05Z").epochSecond
            },
            MessageTransition().apply {
              accuracy = 48
              description = "Transition!"
              event = "enter"
              latitude = 52.12
              longitude = 0.56
              trigger = "l"
              trackerId = "aa" // This is the trackerId of the *waypoint*
              timestamp = Instant.parse("2006-01-02T15:04:05Z").epochSecond
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
              "owntracks/someuser/somedevice",
              Qos.AT_LEAST_ONCE,
              MQTT5Properties(),
              it.toUByteArray(),
          )
        }
    messageReceivedIdlingResource.use { Espresso.onIdle() }

    val notificationManager = app.getSystemService(NOTIFICATION_SERVICE) as NotificationManager

    notificationManager.activeNotifications.forEach {
      Timber.d(
          "Notification Title: ${it.notification.extras.getString(Notification.EXTRA_TITLE)} Lines: ${
                  it.notification.extras.getCharSequenceArray(
                          Notification.EXTRA_TEXT_LINES,
                  )?.joinToString(separator = "|")
              }",
      )
    }

    assertTrue(
        "Event notification is displayed",
        notificationManager.activeNotifications.any {
          it.notification.extras.getString(Notification.EXTRA_TITLE) == "Events" &&
              it.notification.extras.getCharSequenceArray(Notification.EXTRA_TEXT_LINES)?.any { line
                ->
                line.toString() == "2006-01-02 15:04 tt enters Transition!"
              } ?: false
        },
    )
  }

  @Test
  fun given_an_mqtt_configured_client_when_the_broker_sends_a_transition_message_for_a_contact_with_a_card_then_a_notification_appears() {
    setupTestActivity({
      configureMQTTConnectionToLocalWithGeneratedPassword(
          saveConfigurationIdlingResource,
      )
    })

    listOf(
            MessageCard().apply { name = "Test Contact" },
            MessageLocation().apply {
              latitude = 52.123
              longitude = 0.56789
              trackerId = "tt"
              timestamp = Instant.parse("2006-01-02T15:04:05Z").epochSecond
            },
            MessageTransition().apply {
              accuracy = 48
              description = "Transition!"
              event = "enter"
              latitude = 52.12
              longitude = 0.56
              trigger = "l"
              trackerId = "aa"
              timestamp = Instant.parse("2006-01-02T15:04:05Z").epochSecond
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
              "owntracks/someuser/somedevice",
              Qos.AT_LEAST_ONCE,
              MQTT5Properties(),
              it.toUByteArray(),
          )
        }
    messageReceivedIdlingResource.use { Espresso.onIdle() }

    val notificationManager = app.getSystemService(NOTIFICATION_SERVICE) as NotificationManager

    notificationManager.activeNotifications.forEach {
      Timber.d(
          "Notification Title: ${it.notification.extras.getString(Notification.EXTRA_TITLE)} Lines: ${
                  it.notification.extras.getCharSequenceArray(
                          Notification.EXTRA_TEXT_LINES,
                  )?.joinToString(separator = "|")
              }",
      )
    }
    assertTrue(
        "Event notification is displayed",
        notificationManager.activeNotifications.any {
          it.notification.extras.getString(Notification.EXTRA_TITLE) == "Events" &&
              it.notification.extras.getCharSequenceArray(Notification.EXTRA_TEXT_LINES)?.any { line
                ->
                line.toString() == "2006-01-02 15:04 Test Contact enters Transition!"
              } ?: false
        },
    )
  }

  @Test
  fun given_an_mqtt_configured_client_when_the_location_enters_a_geofence_a_transition_message_is_sent() {
    val waypointLatitude = 48.0
    val waypointLongitude = -1.0
    val waypointDescription = "Test Region"

    setupTestActivity({
      configureMQTTConnectionToLocalWithGeneratedPassword(
          saveConfigurationIdlingResource,
      )
    })

    reportLocationFromMap(mockLocationIdlingResource) {
      mockLocationProviderClient.setLocation(
          51.0,
          0.0,
      )
    }

    openDrawer()
    clickOn(R.string.title_activity_waypoints)
    addWaypoint(
        waypointDescription, waypointLatitude.toString(), waypointLongitude.toString(), "100")

    reportLocationFromMap(mockLocationIdlingResource) {
      mockLocationProviderClient.setLocation(waypointLatitude, waypointLongitude)
    }

    assertTrue(
        "Packet has been received that is a transition message with the correct details",
        mqttPacketsReceived
            .filterIsInstance<MQTTPublish>()
            .map { Pair(it.topicName, Parser(null).fromJson((it.payload)!!.toByteArray())) }
            .any {
              it.second.let { message ->
                message is MessageTransition &&
                    message.description == waypointDescription &&
                    message.latitude == waypointLatitude &&
                    message.longitude == waypointLongitude &&
                    message.event == "enter"
              } && it.first == "owntracks/$mqttUsername/$deviceId/event"
            },
    )
  }
}
