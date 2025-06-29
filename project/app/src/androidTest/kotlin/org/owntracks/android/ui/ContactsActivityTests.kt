package org.owntracks.android.ui

import androidx.test.filters.LargeTest
import com.adevinta.android.barista.assertion.BaristaListAssertions.assertDisplayedAtPosition
import com.adevinta.android.barista.assertion.BaristaRecyclerViewAssertions.assertRecyclerViewItemCount
import com.adevinta.android.barista.assertion.BaristaVisibilityAssertions.assertDisplayed
import com.adevinta.android.barista.assertion.BaristaVisibilityAssertions.assertNotDisplayed
import dagger.hilt.android.testing.HiltAndroidTest
import java.time.Instant
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi
import kotlin.random.Random
import mqtt.packets.Qos
import mqtt.packets.mqttv5.MQTT5Properties
import org.junit.Test
import org.owntracks.android.R
import org.owntracks.android.model.Parser
import org.owntracks.android.model.messages.MessageCard
import org.owntracks.android.model.messages.MessageClear
import org.owntracks.android.model.messages.MessageLocation
import org.owntracks.android.testutils.OWNTRACKS_ICON_BASE64
import org.owntracks.android.testutils.TestWithAnActivity
import org.owntracks.android.testutils.TestWithAnMQTTBroker
import org.owntracks.android.testutils.TestWithAnMQTTBrokerImpl
import org.owntracks.android.testutils.string
import org.owntracks.android.testutils.use
import org.owntracks.android.ui.contacts.ContactsActivity
import timber.log.Timber

@OptIn(ExperimentalUnsignedTypes::class)
@LargeTest
@HiltAndroidTest
class ContactsActivityTests :
    TestWithAnActivity<ContactsActivity>(false),
    TestWithAnMQTTBroker by TestWithAnMQTTBrokerImpl() {

  @Test
  fun initial_regions_activity_is_empty() {
    setupTestActivity({
      configureMQTTConnectionToLocalWithGeneratedPassword(saveConfigurationIdlingResource)
    })
    assertDisplayed(R.string.contactsListPlaceholder)
  }

  @Test
  fun contact_appears_when_broker_adds_a_card_message() {
    setupTestActivity({
      configureMQTTConnectionToLocalWithGeneratedPassword(saveConfigurationIdlingResource)
    })
    val contactName = "TestName"
    (baristaRule.activityTestRule.activity as ContactsActivity)
        .contactsCountingIdlingResource
        .increment()
    MessageCard()
        .apply {
          name = contactName
          face = OWNTRACKS_ICON_BASE64
        }
        .sendFromBroker(broker)

    (baristaRule.activityTestRule.activity as ContactsActivity).contactsCountingIdlingResource.use {
      assertNotDisplayed(R.id.placeholder)
      assertRecyclerViewItemCount(R.id.contactsRecyclerView, 1)
      assertDisplayedAtPosition(R.id.contactsRecyclerView, 0, R.id.name, contactName)
      assertDisplayedAtPosition(R.id.contactsRecyclerView, 0, R.id.location, R.string.na)
    }
  }

  @Test
  fun contact_appears_when_broker_adds_a_card_message_with_an_invalid_face_base64() {
    setupTestActivity({
      configureMQTTConnectionToLocalWithGeneratedPassword(saveConfigurationIdlingResource)
    })
    val contactName = "TestName"
    (baristaRule.activityTestRule.activity as ContactsActivity)
        .contactsCountingIdlingResource
        .increment()
    MessageCard()
        .apply {
          name = contactName
          face = "------------------------------"
        }
        .sendFromBroker(broker)

    (baristaRule.activityTestRule.activity as ContactsActivity).contactsCountingIdlingResource.use {
      assertNotDisplayed(R.id.placeholder)
      assertRecyclerViewItemCount(R.id.contactsRecyclerView, 1)
      assertDisplayedAtPosition(R.id.contactsRecyclerView, 0, R.id.name, contactName)
      assertDisplayedAtPosition(R.id.contactsRecyclerView, 0, R.id.location, R.string.na)
    }
  }

  @OptIn(ExperimentalEncodingApi::class)
  @Test
  fun contact_appears_when_broker_adds_a_card_message_with_an_invalid_face_image() {
    setupTestActivity({
      configureMQTTConnectionToLocalWithGeneratedPassword(saveConfigurationIdlingResource)
    })
    val contactName = "TestName"
    (baristaRule.activityTestRule.activity as ContactsActivity)
        .contactsCountingIdlingResource
        .increment()
    MessageCard()
        .apply {
          name = contactName
          face = Base64.encode("not an image".toByteArray())
        }
        .sendFromBroker(broker)

    (baristaRule.activityTestRule.activity as ContactsActivity).contactsCountingIdlingResource.use {
      assertNotDisplayed(R.id.placeholder)
      assertRecyclerViewItemCount(R.id.contactsRecyclerView, 1)
      assertDisplayedAtPosition(R.id.contactsRecyclerView, 0, R.id.name, contactName)
      assertDisplayedAtPosition(R.id.contactsRecyclerView, 0, R.id.location, R.string.na)
    }
  }

  @Test
  fun contact_appears_when_broker_adds_a_card_message_and_a_location_message() {
    setupTestActivity({
      configureMQTTConnectionToLocalWithGeneratedPassword(saveConfigurationIdlingResource)
    })

    (baristaRule.activityTestRule.activity as ContactsActivity)
        .contactsCountingIdlingResource
        .increment()
    val contactName = "TestName"
    listOf(
            MessageCard().apply {
              name = contactName
              face = OWNTRACKS_ICON_BASE64
            },
            MessageLocation().apply {
              latitude = 52.123
              longitude = 0.56789
              timestamp = Instant.parse("2006-01-02T15:04:05Z").epochSecond
            },
        )
        .sendFromBroker(broker)

    (baristaRule.activityTestRule.activity as ContactsActivity).contactsCountingIdlingResource.use {
      assertNotDisplayed(R.id.placeholder)
      assertRecyclerViewItemCount(R.id.contactsRecyclerView, 1)
      assertDisplayedAtPosition(R.id.contactsRecyclerView, 0, R.id.name, contactName)
      assertDisplayedAtPosition(R.id.contactsRecyclerView, 0, R.id.location, "52.1230, 0.5679")
    }
  }

  @Test
  fun contact_appears_when_broker_adds_a_location_message() {
    setupTestActivity({
      configureMQTTConnectionToLocalWithGeneratedPassword(saveConfigurationIdlingResource)
    })
    (baristaRule.activityTestRule.activity as ContactsActivity)
        .contactsCountingIdlingResource
        .increment()

    val contactName = "aa"
    MessageLocation()
        .apply {
          latitude = 34.0
          longitude = 0.0
          trackerId = contactName
        }
        .sendFromBroker(broker)

    (baristaRule.activityTestRule.activity as ContactsActivity).contactsCountingIdlingResource.use {
      assertNotDisplayed(R.id.placeholder)
      assertRecyclerViewItemCount(R.id.contactsRecyclerView, 1)
      assertDisplayedAtPosition(R.id.contactsRecyclerView, 0, R.id.name, contactName)
    }
  }

  @Test
  fun multiple_contacts_displayed_in_time_stamp_order() {
    setupTestActivity({
      configureMQTTConnectionToLocalWithGeneratedPassword(saveConfigurationIdlingResource)
    })

    val baseTimeStamp = Instant.ofEpochSecond(1695137000)

    data class TimeAndName(val instant: Instant, val trackerId: String, val topic: String)

    val random = Random(1)
    val timesAndNames =
        IntRange(0, random.nextInt(20))
            .map {
              TimeAndName(
                  baseTimeStamp.plusSeconds(random.nextLong(-3600, 3600)),
                  random.string(2),
                  random.string(5),
              )
            }
            .toList()

    val parser = Parser(null)
    timesAndNames
        .map {
          it to
              MessageLocation().apply {
                latitude = random.nextDouble(-90.0, 90.0)
                longitude = random.nextDouble(-180.0, 180.0)
                timestamp = it.instant.epochSecond
                trackerId = it.trackerId
              }
        }
        .map { it.first to it.second.toJsonBytes(parser) }
        .forEach {
          (baristaRule.activityTestRule.activity as ContactsActivity)
              .contactsCountingIdlingResource
              .increment()
          Timber.v("publishing location message onto ${it.first.topic}")
          broker.publish(
              false,
              "owntracks/${it.first.topic}/somedevice",
              Qos.AT_LEAST_ONCE,
              MQTT5Properties(),
              it.second.toUByteArray(),
          )
        }

    (baristaRule.activityTestRule.activity as ContactsActivity).contactsCountingIdlingResource.use {
      assertNotDisplayed(R.id.placeholder)
    }
    assertRecyclerViewItemCount(R.id.contactsRecyclerView, timesAndNames.size)
    timesAndNames
        .sortedBy { it.instant }
        .reversed()
        .forEachIndexed { index, s ->
          assertDisplayedAtPosition(R.id.contactsRecyclerView, index, R.id.name, s.trackerId)
        }
  }

  @Test
  fun contact_removed_when_clear_message_is_sent() {
    setupTestActivity({
      configureMQTTConnectionToLocalWithGeneratedPassword(saveConfigurationIdlingResource)
    })
    (baristaRule.activityTestRule.activity as ContactsActivity)
        .contactsCountingIdlingResource
        .increment()
    MessageLocation()
        .apply {
          latitude = 51.0
          longitude = 0.0
          timestamp = 1695137000
          trackerId = "aa"
        }
        .sendFromBroker(broker)

    (baristaRule.activityTestRule.activity as ContactsActivity).contactsCountingIdlingResource.use {
      assertNotDisplayed(R.id.placeholder)
    }

    (baristaRule.activityTestRule.activity as ContactsActivity)
        .contactsCountingIdlingResource
        .increment()
    MessageClear().sendFromBroker(broker)
    (baristaRule.activityTestRule.activity as ContactsActivity).contactsCountingIdlingResource.use {
      assertDisplayed(R.id.placeholder)
    }
  }
}
