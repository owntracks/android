package org.owntracks.android.mqtt

import androidx.preference.PreferenceManager
import androidx.test.espresso.Espresso
import androidx.test.espresso.Espresso.openActionBarOverflowOrOptionsMenu
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import com.adevinta.android.barista.assertion.BaristaListAssertions.assertDisplayedAtPosition
import com.adevinta.android.barista.assertion.BaristaRecyclerViewAssertions.assertRecyclerViewItemCount
import com.adevinta.android.barista.assertion.BaristaVisibilityAssertions.assertContains
import com.adevinta.android.barista.assertion.BaristaVisibilityAssertions.assertDisplayed
import com.adevinta.android.barista.interaction.BaristaDrawerInteractions.openDrawer
import com.adevinta.android.barista.interaction.BaristaEditTextInteractions.writeTo
import com.adevinta.android.barista.interaction.BaristaSleepInteractions.sleep
import java.time.Instant
import mqtt.packets.mqtt.MQTTPublish
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.owntracks.android.R
import org.owntracks.android.model.BatteryStatus
import org.owntracks.android.model.messages.MessageCard
import org.owntracks.android.model.messages.MessageLocation
import org.owntracks.android.model.messages.MessageWaypoints
import org.owntracks.android.preferences.Preferences
import org.owntracks.android.preferences.types.MonitoringMode
import org.owntracks.android.support.Parser
import org.owntracks.android.testutils.GPSMockDeviceLocation
import org.owntracks.android.testutils.MockDeviceLocation
import org.owntracks.android.testutils.OWNTRACKS_ICON_BASE64
import org.owntracks.android.testutils.TestWithAnActivity
import org.owntracks.android.testutils.TestWithAnMQTTBroker
import org.owntracks.android.testutils.TestWithAnMQTTBrokerImpl
import org.owntracks.android.testutils.getCurrentActivity
import org.owntracks.android.testutils.grantMapActivityPermissions
import org.owntracks.android.testutils.reportLocationFromMap
import org.owntracks.android.testutils.setNotFirstStartPreferences
import org.owntracks.android.testutils.use
import org.owntracks.android.testutils.waitUntilActivityVisible
import org.owntracks.android.ui.clickOnAndWait
import org.owntracks.android.ui.contacts.ContactsActivity
import org.owntracks.android.ui.map.MapActivity
import timber.log.Timber

@ExperimentalUnsignedTypes
@LargeTest
@RunWith(AndroidJUnit4::class)
class MQTTMessagePublishTests :
    TestWithAnActivity<MapActivity>(MapActivity::class.java, false),
    TestWithAnMQTTBroker by TestWithAnMQTTBrokerImpl(),
    MockDeviceLocation by GPSMockDeviceLocation() {

    @Test
    fun given_an_MQTT_configured_client_when_the_report_button_is_pressed_then_the_broker_receives_a_packet_with_the_correct_location_message_in() {
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
    @org.owntracks.android.testutils.JustThisTestPlease
    fun given_an_MQTT_configured_client_when_the_broker_sends_a_location_for_a_cleared_contact_then_a_the_contact_returns_with_the_correct_details() {
        setup()

        openDrawer()
        clickOnAndWait(R.string.title_activity_contacts)
        val contactsCountingIdlingResource = (getCurrentActivity() as ContactsActivity).contactsCountingIdlingResource

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
        ).also {
            repeat(it.size) { contactsCountingIdlingResource.increment() }
        }.sendFromBroker(broker)

        contactsCountingIdlingResource.use {
            clickOnAndWait("TestName")
        }
        sleep(1000) // Apparently espresso won't wait for the MapActivity to finish rendering
        clickOnAndWait(R.id.contactPeek)
        assertDisplayed(R.id.contactClearButton)
        clickOnAndWait(R.id.contactClearButton)

        openDrawer()
        clickOnAndWait(R.string.title_activity_contacts)
        assertDisplayed(R.id.placeholder)

        contactsCountingIdlingResource.increment()
        listOf(
            MessageLocation().apply {
                latitude = 50.123
                longitude = 3.56789
                timestamp = Instant.parse("2006-01-02T15:04:05Z").epochSecond
            }
        ).sendFromBroker(broker)

        contactsCountingIdlingResource.use {
            assertRecyclerViewItemCount(R.id.contactsRecyclerView, 1)
            assertDisplayedAtPosition(R.id.contactsRecyclerView, 0, R.id.name, "TestName")
            assertDisplayedAtPosition(R.id.contactsRecyclerView, 0, R.id.location, "50.1230, 3.5679")
            assertDisplayedAtPosition(
                R.id.contactsRecyclerView,
                0,
                R.id.locationDate,
                "1/2/06" // Default locale for emulator is en_US
            )
        }
    }

    @Test
    fun given_an_MQTT_configured_client_when_the_wrong_credentials_are_used_then_the_status_screen_shows_that_the_broker_is_not_connected() {
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
    fun given_an_MQTT_configured_client_when_the_user_publishes_waypoints_then_the_broker_receives_a_waypoint_message() {
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
        PreferenceManager.getDefaultSharedPreferences(app)
            .edit()
            .putInt(Preferences::monitoring.name, MonitoringMode.QUIET.value)
            .putString(Preferences::reverseGeocodeProvider.name, "None")
            .apply()
        setNotFirstStartPreferences()
        launchActivity()
        grantMapActivityPermissions()

        initializeMockLocationProvider(app)
        configureMQTTConnectionToLocalWithGeneratedPassword()
        waitUntilActivityVisible<MapActivity>()
        clickOnAndWait(R.id.fabMyLocation)
    }
}
