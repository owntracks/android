package org.owntracks.android

import androidx.preference.PreferenceManager
import androidx.test.espresso.Espresso
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.platform.app.InstrumentationRegistry
import dagger.hilt.android.testing.HiltAndroidTest
import kotlin.random.Random
import mqtt.packets.mqtt.MQTTPublish
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.owntracks.android.model.Parser
import org.owntracks.android.model.messages.MessageLocation
import org.owntracks.android.preferences.Preferences
import org.owntracks.android.testutils.TestWithAnActivity
import org.owntracks.android.testutils.TestWithAnMQTTBroker
import org.owntracks.android.testutils.TestWithAnMQTTBrokerImpl
import org.owntracks.android.testutils.di.setLocation
import org.owntracks.android.testutils.use
import org.owntracks.android.ui.map.MapActivity

@ExperimentalUnsignedTypes
@LargeTest
@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class LocationBehaviourTests :
    TestWithAnActivity<MapActivity>(false), TestWithAnMQTTBroker by TestWithAnMQTTBrokerImpl() {

  @Test
  fun given_an_inaccurate_and_accurate_location_when_publishing_then_only_the_accurate_location_is_published() {
    val inaccurateMockLatitude = Random.nextDouble(-30.0, 30.0)
    val inaccurateMockLongitude = 4.0
    val accurateMockLatitude = Random.nextDouble(-30.0, 30.0)
    val accurateMockLongitude = 0.001

    PreferenceManager.getDefaultSharedPreferences(
            InstrumentationRegistry.getInstrumentation().targetContext,
        )
        .edit()
        .putInt(Preferences::ignoreInaccurateLocations.name, 50)
        .apply()
    setupTestActivity({
      configureMQTTConnectionToLocalWithGeneratedPassword(saveConfigurationIdlingResource)
    })
    reportLocationFromMap(mockLocationIdlingResource) {
      mockLocationProviderClient.setLocation(
          inaccurateMockLongitude,
          inaccurateMockLatitude,
          50.0,
          3000f,
      )
    }

    reportLocationFromMap(mockLocationIdlingResource) {
      mockLocationProviderClient.setLocation(accurateMockLatitude, accurateMockLongitude)
    }

    outgoingQueueIdlingResource.use { Espresso.onIdle() }

    mqttPacketsReceived
        .filterIsInstance<MQTTPublish>()
        .map { Pair(it.topicName, Parser(null).fromJson((it.payload)!!.toByteArray())) }
        .run {
          assertTrue(
              "received packets contains accurate location",
              any {
                it.second.run {
                  this is MessageLocation &&
                      latitude == accurateMockLatitude &&
                      longitude == accurateMockLongitude
                }
              },
          )
          assertFalse(
              "received packets doesn't contain inaccurate location",
              any {
                it.second.run {
                  this is MessageLocation &&
                      latitude == inaccurateMockLatitude &&
                      longitude == inaccurateMockLongitude
                }
              },
          )
        }
  }
}
