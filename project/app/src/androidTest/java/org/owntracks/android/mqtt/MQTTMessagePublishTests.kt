package org.owntracks.android.mqtt

import android.Manifest
import android.view.View
import androidx.annotation.IdRes
import androidx.preference.PreferenceManager
import androidx.test.espresso.Espresso
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.Espresso.openActionBarOverflowOrOptionsMenu
import androidx.test.espresso.UiController
import androidx.test.espresso.ViewAction
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isClickable
import androidx.test.espresso.matcher.ViewMatchers.isEnabled
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import com.adevinta.android.barista.assertion.BaristaListAssertions.assertDisplayedAtPosition
import com.adevinta.android.barista.assertion.BaristaRecyclerViewAssertions.assertRecyclerViewItemCount
import com.adevinta.android.barista.assertion.BaristaVisibilityAssertions.assertContains
import com.adevinta.android.barista.assertion.BaristaVisibilityAssertions.assertDisplayed
import com.adevinta.android.barista.interaction.BaristaDrawerInteractions.openDrawer
import com.adevinta.android.barista.interaction.BaristaEditTextInteractions.writeTo
import com.adevinta.android.barista.interaction.BaristaSleepInteractions.sleep
import com.adevinta.android.barista.interaction.PermissionGranter
import java.time.Instant
import kotlin.time.Duration.Companion.seconds
import mqtt.packets.Qos
import mqtt.packets.mqtt.MQTTPublish
import mqtt.packets.mqttv5.MQTT5Properties
import org.hamcrest.CoreMatchers
import org.hamcrest.Matcher
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.owntracks.android.R
import org.owntracks.android.model.BatteryStatus
import org.owntracks.android.model.messages.MessageCard
import org.owntracks.android.model.messages.MessageLocation
import org.owntracks.android.model.messages.MessageWaypoints
import org.owntracks.android.preferences.Preferences
import org.owntracks.android.support.Parser
import org.owntracks.android.testutils.GPSMockDeviceLocation
import org.owntracks.android.testutils.MockDeviceLocation
import org.owntracks.android.testutils.OWNTRACKS_ICON_BASE64
import org.owntracks.android.testutils.TestWithAnActivity
import org.owntracks.android.testutils.TestWithAnMQTTBroker
import org.owntracks.android.testutils.TestWithAnMQTTBrokerImpl
import org.owntracks.android.testutils.grantMapActivityPermissions
import org.owntracks.android.testutils.reportLocationFromMap
import org.owntracks.android.testutils.setNotFirstStartPreferences
import org.owntracks.android.testutils.use
import org.owntracks.android.testutils.waitUntilActivityVisible
import org.owntracks.android.ui.clickOnAndWait
import org.owntracks.android.ui.map.MapActivity

@ExperimentalUnsignedTypes
@LargeTest
@RunWith(AndroidJUnit4::class)
class MQTTMessagePublishTests :
    TestWithAnActivity<MapActivity>(MapActivity::class.java, false),
    TestWithAnMQTTBroker by TestWithAnMQTTBrokerImpl(),
    MockDeviceLocation by GPSMockDeviceLocation() {

    @Test
    fun given_an_MQTT_configured_client_when_the_report_button_is_pressed_then_the_broker_receives_a_packet_with_the_correct_location_message_in() { // ktlint-disable max-line-length
        setup()
        val mockLatitude = 51.0
        val mockLongitude = 0.0

        reportLocationFromMap(app.mockLocationIdlingResource) {
            setMockLocation(mockLatitude, mockLongitude)
        }

        baristaRule.activityTestRule.activity.outgoingQueueIdlingResource.use {
            openDrawer()
            clickOnAndWait(R.string.title_activity_contacts)
        }

        assertTrue(
            "Packet has been received that is a location message with the correct latlng in it",
            mqttPacketsReceived.filterIsInstance<MQTTPublish>()
                .map {
                    Parser(null).fromJson((it.payload)!!.toByteArray())
                }
                .any {
                    it is MessageLocation && it.latitude == mockLatitude && it.longitude == mockLongitude
                }
        )
    }

    @Test
    fun given_an_MQTT_configured_client_when_the_broker_sends_a_message_card_without_a_location_then_a_new_contact_appears() { // ktlint-disable max-line-length
        setup()

        reportLocationFromMap(app.mockLocationIdlingResource) {
            setMockLocation(51.0, 0.0)
        }

        baristaRule.activityTestRule.activity.outgoingQueueIdlingResource.use {
            openDrawer()
            clickOnAndWait(R.string.title_activity_contacts)
        }

        val messageCard = MessageCard().apply {
            name = "TestName"
            face = OWNTRACKS_ICON_BASE64
        }
        val bytes = Parser(null).toJsonBytes(messageCard)

        broker.publish(
            false,
            "owntracks/someuser/somedevice/info",
            Qos.AT_LEAST_ONCE,
            MQTT5Properties(),
            bytes.toUByteArray()
        )
        sleep(1000) // Need to wait for the message to percolate through the app.
        assertRecyclerViewItemCount(R.id.contactsRecyclerView, 2)
        assertDisplayedAtPosition(R.id.contactsRecyclerView, 1, R.id.name, "TestName")
        assertDisplayedAtPosition(R.id.contactsRecyclerView, 1, R.id.location, R.string.na)
    }

    @Test
    fun given_an_MQTT_configured_client_when_the_broker_sends_a_message_card_with_a_location_then_a_new_contact_appears() { // ktlint-disable max-line-length
        PreferenceManager.getDefaultSharedPreferences(app)
            .edit()
            .putString(Preferences::reverseGeocodeProvider.name, "None")
            .apply()
        setup()

        reportLocationFromMap(app.mockLocationIdlingResource) {
            setMockLocation(51.0, 0.0)
        }

        app.mqttConnectionIdlingResource.use {
            openDrawer()
            clickOnAndWait(R.string.title_activity_contacts)
        }

        listOf(
            MessageLocation().apply {
                latitude = 52.123
                longitude = 0.56789
                timestamp = Instant.parse("2006-01-02T15:04:05Z").epochSecond
            },
            MessageCard().apply {
                name = "TestName"
                face = OWNTRACKS_ICON_BASE64
            }
        ).map {
            Pair(
                Parser(null).toJsonBytes(it),
                when (it) {
                    is MessageCard -> "owntracks/someuser/somedevice/info"
                    else -> "owntracks/someuser/somedevice"
                }
            )
        }
            .forEach {
                broker.publish(
                    false,
                    it.second,
                    Qos.AT_LEAST_ONCE,
                    MQTT5Properties(),
                    it.first.toUByteArray()
                )
            }
        sleep(1000) // Need to wait for the message to percolate through the app.
        assertRecyclerViewItemCount(R.id.contactsRecyclerView, 2)
        assertDisplayedAtPosition(R.id.contactsRecyclerView, 1, R.id.name, "TestName")
        assertDisplayedAtPosition(R.id.contactsRecyclerView, 1, R.id.location, "52.1230, 0.5679")
        assertDisplayedAtPosition(
            R.id.contactsRecyclerView,
            1,
            R.id.locationDate,
            "1/2/06" // Default locale for emulator is en_US
        )
    }

    @Test
    fun given_an_MQTT_configured_client_when_the_broker_sends_a_location_for_a_cleared_contact_then_a_the_contact_returns_with_the_correct_details() { // ktlint-disable max-line-length
        PreferenceManager.getDefaultSharedPreferences(app)
            .edit()
            .putString(Preferences::reverseGeocodeProvider.name, "None")
            .apply()

        setup()
        reportLocationFromMap(app.mockLocationIdlingResource) {
            setMockLocation(51.0, 0.0)
        }

        baristaRule.activityTestRule.activity.outgoingQueueIdlingResource.use {
            openDrawer()
            clickOnAndWait(R.string.title_activity_contacts)
        }

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
                name = "TestName"
                face = OWNTRACKS_ICON_BASE64
            }
        ).sendFromBroker(broker)

        sleep(1000) // Need to wait for the message to percolate through the app.
        clickOnAndWait("TestName")
        sleep(1000) // Apparently espresso won't wait for the MapActivity to finish rendering
        clickOnAndWait(R.id.contactPeek)
        assertDisplayed(R.id.contactClearButton)
        clickOnRegardlessOfVisibility(R.id.contactClearButton)

        openDrawer()
        clickOnAndWait(R.string.title_activity_contacts)
        assertRecyclerViewItemCount(R.id.contactsRecyclerView, 1)
        assertDisplayedAtPosition(R.id.contactsRecyclerView, 0, R.id.name, deviceId)

        listOf(
            MessageLocation().apply {
                latitude = 52.123
                longitude = 0.56789
                timestamp = Instant.parse("2006-01-02T15:04:05Z").epochSecond
            }
        ).sendFromBroker(broker)

        sleep(1.seconds.inWholeMilliseconds)

        assertRecyclerViewItemCount(R.id.contactsRecyclerView, 2)
        assertDisplayedAtPosition(R.id.contactsRecyclerView, 1, R.id.name, "TestName")
        assertDisplayedAtPosition(R.id.contactsRecyclerView, 1, R.id.location, "52.1230, 0.5679")
        assertDisplayedAtPosition(
            R.id.contactsRecyclerView,
            1,
            R.id.locationDate,
            "1/2/06" // Default locale for emulator is en_US
        )
    }

    @Test
    fun given_an_MQTT_configured_client_when_the_wrong_credentials_are_used_then_the_status_screen_shows_that_the_broker_is_not_connected() { // ktlint-disable max-line-length
        setNotFirstStartPreferences()
        launchActivity()
        grantMapActivityPermissions()
        configureMQTTConnectionToLocal("not the right password")
        waitUntilActivityVisible<MapActivity>()
        app.mqttConnectionIdlingResource.use {
            openDrawer()
            clickOnAndWait(R.string.title_activity_status)
        }
        assertContains(R.id.connectedStatus, R.string.ERROR)
    }

    @OptIn(ExperimentalUnsignedTypes::class)
    @Test
    fun given_an_MQTT_configured_client_when_the_user_publishes_waypoints_then_the_broker_receives_a_waypoint_message() { // ktlint-disable max-line-length
        setup()

        openDrawer()
        clickOnAndWait(R.string.title_activity_waypoints)

        clickOnAndWait(R.id.add)
        writeTo(R.id.description, "test waypoint")
        writeTo(R.id.latitude, "51.0")
        writeTo(R.id.longitude, "1.0")
        writeTo(R.id.radius, "20")

        clickOnAndWait(R.id.save)

        clickOnAndWait(R.id.add)
        writeTo(R.id.description, "test waypoint 2")
        writeTo(R.id.latitude, "20.123456789")
        writeTo(R.id.longitude, "1.234567")
        writeTo(R.id.radius, "20.987")

        clickOnAndWait(R.id.save)

        baristaRule.activityTestRule.activity.publishResponseMessageIdlingResource.setIdleState(false)

        openActionBarOverflowOrOptionsMenu(baristaRule.activityTestRule.activity)
        clickOnAndWait(R.string.exportWaypointsToEndpoint)

        baristaRule.activityTestRule.activity.publishResponseMessageIdlingResource.use {
            Espresso.onIdle()
        }

        baristaRule.activityTestRule.activity.outgoingQueueIdlingResource.use {
            val packets = mqttPacketsReceived.filterIsInstance<MQTTPublish>().map {
                Parser(null).fromJson((it.payload)!!.toByteArray())
            }
            assertTrue(
                packets.any {
                    it is MessageWaypoints
                }
            )
        }
    }

    private fun setup() {
        setNotFirstStartPreferences()
        launchActivity()
        grantMapActivityPermissions()
        initializeMockLocationProvider(app)
        configureMQTTConnectionToLocalWithGeneratedPassword()
        waitUntilActivityVisible<MapActivity>()
        clickOnAndWait(R.id.fabMyLocation)
        PermissionGranter.allowPermissionsIfNeeded(Manifest.permission.ACCESS_FINE_LOCATION)
    }

    private fun clickOnRegardlessOfVisibility(@IdRes id: Int) {
        onView(withId(id)).check(
            matches(
                CoreMatchers.allOf(
                    isEnabled(),
                    isClickable()
                )
            )
        )
            .perform(object : ViewAction {
                override fun getConstraints(): Matcher<View> {
                    return isEnabled() // no constraints, they are checked above
                }

                override fun getDescription(): String {
                    return "click plus button"
                }

                override fun perform(uiController: UiController?, view: View) {
                    view.performClick()
                }
            })
    }
}
