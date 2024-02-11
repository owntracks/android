package org.owntracks.android.e2e

import androidx.preference.PreferenceManager
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.platform.app.InstrumentationRegistry
import com.adevinta.android.barista.interaction.BaristaSleepInteractions
import java.util.concurrent.TimeUnit
import kotlin.random.Random
import mqtt.packets.mqtt.MQTTPublish
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.owntracks.android.model.messages.MessageLocation
import org.owntracks.android.preferences.Preferences
import org.owntracks.android.support.Parser
import org.owntracks.android.testutils.GPSMockDeviceLocation
import org.owntracks.android.testutils.MockDeviceLocation
import org.owntracks.android.testutils.TestWithAnActivity
import org.owntracks.android.testutils.TestWithAnMQTTBroker
import org.owntracks.android.testutils.TestWithAnMQTTBrokerImpl
import org.owntracks.android.testutils.grantMapActivityPermissions
import org.owntracks.android.testutils.reportLocationFromMap
import org.owntracks.android.testutils.setNotFirstStartPreferences
import org.owntracks.android.testutils.waitUntilActivityVisible
import org.owntracks.android.ui.map.MapActivity

@OptIn(ExperimentalUnsignedTypes::class)
@LargeTest
@RunWith(AndroidJUnit4::class)
class LocationAccuracyTest :
    TestWithAnActivity<MapActivity>(MapActivity::class.java, false),
    TestWithAnMQTTBroker by TestWithAnMQTTBrokerImpl(),
    MockDeviceLocation by GPSMockDeviceLocation() {

    @Test
    fun given_an_inaccurate_and_accurate_location_when_publishing_then_only_the_location_only_the_accurate_location_is_published() {
        val inaccurateMockLatitude = Random.nextDouble(-30.0, 30.0)
        val inaccurateMockLongitude = 4.0
        val accurateMockLatitude = Random.nextDouble(-30.0, 30.0)
        val accurateMockLongitude = 0.001

        PreferenceManager.getDefaultSharedPreferences(InstrumentationRegistry.getInstrumentation().targetContext)
            .edit()
            .putInt(Preferences::ignoreInaccurateLocations.name, 50)
            .apply()

        setup()

        reportLocationFromMap(app.mockLocationIdlingResource) {
            setMockLocation(inaccurateMockLatitude, inaccurateMockLongitude, 3000f)
        }

        app.mockLocationIdlingResource.setIdleState(false)
        reportLocationFromMap(app.mockLocationIdlingResource) {
            setMockLocation(accurateMockLatitude, accurateMockLongitude)
        }

        mqttPacketsReceived.filterIsInstance<MQTTPublish>().map {
            Pair(it.topicName, Parser(null).fromJson((it.payload)!!.toByteArray()))
        }.run {
            assertTrue(
                "received packets contains accurate location",
                any {
                    it.second.run {
                        this is MessageLocation &&
                            latitude == accurateMockLatitude &&
                            longitude == accurateMockLongitude
                    }
                }
            )
            assertFalse(
                "received packets doesn't contain inaccurate location",
                any {
                    it.second.run {
                        this is MessageLocation &&
                            latitude == inaccurateMockLatitude &&
                            longitude == inaccurateMockLongitude
                    }
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
    }
}
