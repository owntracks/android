package org.owntracks.android.mqtt

import androidx.test.espresso.Espresso
import androidx.test.espresso.Espresso.openActionBarOverflowOrOptionsMenu
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import com.adevinta.android.barista.assertion.BaristaListAssertions.assertDisplayedAtPosition
import com.adevinta.android.barista.assertion.BaristaRecyclerViewAssertions.assertRecyclerViewItemCount
import com.adevinta.android.barista.assertion.BaristaVisibilityAssertions.assertContains
import com.adevinta.android.barista.assertion.BaristaVisibilityAssertions.assertDisplayed
import com.adevinta.android.barista.interaction.BaristaClickInteractions.clickOn
import com.adevinta.android.barista.interaction.BaristaDrawerInteractions.openDrawer
import com.google.android.material.bottomsheet.BottomSheetBehavior
import dagger.hilt.android.testing.HiltAndroidTest
import java.time.Instant
import javax.inject.Inject
import javax.inject.Named
import mqtt.packets.mqtt.MQTTPublish
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.owntracks.android.R
import org.owntracks.android.model.BatteryStatus
import org.owntracks.android.model.Parser
import org.owntracks.android.model.messages.MessageCard
import org.owntracks.android.model.messages.MessageLocation
import org.owntracks.android.model.messages.MessageWaypoints
import org.owntracks.android.test.ThresholdIdlingResourceInterface
import org.owntracks.android.testutils.BottomSheetSetStateAction
import org.owntracks.android.testutils.OWNTRACKS_ICON_BASE64
import org.owntracks.android.testutils.TestWithAnActivity
import org.owntracks.android.testutils.TestWithAnMQTTBroker
import org.owntracks.android.testutils.TestWithAnMQTTBrokerImpl
import org.owntracks.android.testutils.addWaypoint
import org.owntracks.android.testutils.di.setLocation
import org.owntracks.android.testutils.getCurrentActivity
import org.owntracks.android.testutils.grantMapActivityPermissions
import org.owntracks.android.testutils.idlingresources.BottomSheetResource
import org.owntracks.android.testutils.setNotFirstStartPreferences
import org.owntracks.android.testutils.use
import org.owntracks.android.ui.map.MapActivity

@ExperimentalUnsignedTypes
@LargeTest
@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class MQTTMessagePublishTests :
    TestWithAnActivity<MapActivity>(false), TestWithAnMQTTBroker by TestWithAnMQTTBrokerImpl() {

  @Inject
  @Named("contactsActivityIdlingResource")
  lateinit var contactsActivityIdlingResource: ThresholdIdlingResourceInterface

  @Test
  fun given_an_MQTT_configured_client_when_the_report_button_is_pressed_then_the_broker_receives_a_packet_with_the_correct_location_message_in() {
    setupTestActivity({
      configureMQTTConnectionToLocalWithGeneratedPassword(saveConfigurationIdlingResource)
    })

    val mockLatitude = 51.0
    val mockLongitude = 0.0

    reportLocationFromMap(mockLocationIdlingResource) {
      mockLocationProviderClient.setLocation(mockLatitude, mockLongitude)
    }
    baristaRule.activityTestRule.activity.outgoingQueueIdlingResource.use {
      openDrawer()
      clickOn(R.string.title_activity_contacts)
    }

    assertTrue(
        "Packet has been received that is a location message with the correct latlng in it",
        mqttPacketsReceived
            .filterIsInstance<MQTTPublish>()
            .map { Parser(null).fromJson((it.payload)!!.toByteArray()) }
            .any {
              it is MessageLocation && it.latitude == mockLatitude && it.longitude == mockLongitude
            },
    )
  }

  @Test
  fun given_an_MQTT_configured_client_when_the_wrong_credentials_are_used_then_the_status_screen_shows_that_the_broker_is_not_connected() {
    setNotFirstStartPreferences()
    launchActivity()
    grantMapActivityPermissions()
    configureMQTTConnectionToLocal(saveConfigurationIdlingResource, "not the right password")
    waitUntilActivityVisible()
    mqttConnectionIdlingResource.use {
      openDrawer()
      clickOn(R.string.title_activity_status)
    }
    assertContains(R.id.connectedStatus, R.string.ERROR)
  }

  @Test
  fun given_an_MQTT_configured_client_when_the_user_publishes_waypoints_then_the_broker_receives_a_waypoint_message() {
    setupTestActivity({
      configureMQTTConnectionToLocalWithGeneratedPassword(saveConfigurationIdlingResource)
    })

    openDrawer()
    clickOn(R.string.title_activity_waypoints)

    addWaypoint("test waypoint", "51.0", "1.0", "20")
    addWaypoint("test waypoint 2", "20.123456789", "1.234567", "20.987")

    baristaRule.activityTestRule.activity.publishResponseMessageIdlingResource.setIdleState(false)

    openActionBarOverflowOrOptionsMenu(baristaRule.activityTestRule.activity)
    clickOn(R.string.exportWaypointsToEndpoint)

    baristaRule.activityTestRule.activity.publishResponseMessageIdlingResource.use {
      Espresso.onIdle()
    }

    baristaRule.activityTestRule.activity.outgoingQueueIdlingResource.use {
      val packets =
          mqttPacketsReceived.filterIsInstance<MQTTPublish>().map {
            Parser(null).fromJson((it.payload)!!.toByteArray())
          }
      assertTrue(packets.any { it is MessageWaypoints })
    }
  }

  @Test
  fun given_an_MQTT_configured_client_when_the_broker_sends_a_location_for_a_cleared_contact_then_a_the_contact_returns_with_the_correct_details() {
    setupTestActivity({
      configureMQTTConnectionToLocalWithGeneratedPassword(saveConfigurationIdlingResource)
    })

    openDrawer()
    clickOn(R.string.title_activity_contacts)

    val contactName = "TestName"

    listOf(
            MessageLocation().apply {
              latitude = 52.123
              longitude = 0.56789
              altitude = 123
              accuracy = 456
              battery = 78
              batteryStatus = BatteryStatus.CHARGING
              velocity = 99
              timestamp = Instant.parse("2006-01-02T15:04:05Z").epochSecond
            },
            MessageCard().apply {
              name = contactName
              face = OWNTRACKS_ICON_BASE64
            },
        )
        .also { repeat(it.size) { contactsActivityIdlingResource.increment() } }
        .sendFromBroker(broker)

    contactsActivityIdlingResource.use { clickOn(contactName) }

    Espresso.onView(withId(R.id.bottomSheetLayout))
        .perform(BottomSheetSetStateAction(BottomSheetBehavior.STATE_EXPANDED))
    BottomSheetResource.BottomSheetStateResource(
            BottomSheetBehavior.from(getCurrentActivity()!!.findViewById(R.id.bottomSheetLayout)),
            BottomSheetBehavior.STATE_EXPANDED,
        )
        .use {
          assertDisplayed(R.id.contactClearButton)
          clickOn(R.id.contactClearButton)
        }

    openDrawer()
    clickOn(R.string.title_activity_contacts)
    assertDisplayed(R.id.placeholder)

    contactsActivityIdlingResource.increment()
    listOf(
            MessageLocation().apply {
              latitude = 50.123
              longitude = 3.56789
              timestamp = Instant.parse("2006-01-02T15:04:05Z").epochSecond
            },
        )
        .sendFromBroker(broker)

    contactsActivityIdlingResource.use {
      assertRecyclerViewItemCount(R.id.contactsRecyclerView, 1)
      assertDisplayedAtPosition(R.id.contactsRecyclerView, 0, R.id.name, "TestName")
      assertDisplayedAtPosition(R.id.contactsRecyclerView, 0, R.id.location, "50.1230, 3.5679")
      assertDisplayedAtPosition(
          R.id.contactsRecyclerView,
          0,
          R.id.locationDate,
          "1/2/06", // Default locale for emulator is en_US
      )
    }
  }
}
