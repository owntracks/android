package org.owntracks.android.mqtt

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import com.adevinta.android.barista.interaction.BaristaSleepInteractions
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.DelicateCoroutinesApi
import mqtt.packets.Qos
import mqtt.packets.mqttv5.MQTT5Properties
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.owntracks.android.testutils.GPSMockDeviceLocation
import org.owntracks.android.testutils.MockDeviceLocation
import org.owntracks.android.testutils.TestWithAnActivity
import org.owntracks.android.testutils.TestWithAnMQTTBroker
import org.owntracks.android.testutils.TestWithAnMQTTBrokerImpl
import org.owntracks.android.testutils.grantMapActivityPermissions
import org.owntracks.android.testutils.setNotFirstStartPreferences
import org.owntracks.android.ui.map.MapActivity

@OptIn(ExperimentalUnsignedTypes::class)
@LargeTest
@RunWith(AndroidJUnit4::class)
class MQTTRemoteCommandTests :
    TestWithAnActivity<MapActivity>(MapActivity::class.java, false),
    TestWithAnMQTTBroker by TestWithAnMQTTBrokerImpl(),
    MockDeviceLocation by GPSMockDeviceLocation() {
    @Before
    @OptIn(DelicateCoroutinesApi::class)
    fun mqttBefore() {
        startBroker()
    }

    @After
    fun mqttAfter() {
        stopBroker()
    }

    @After
    fun uninitMockLocation() {
        unInitializeMockLocationProvider()
    }

    @Test
    fun given_an_MQTT_configured_client_when_the_broker_sends_an_invalid_command_message_then_nothing_happens() {
        setNotFirstStartPreferences()
        launchActivity()

        grantMapActivityPermissions()

        configureMQTTConnectionToLocalWithGeneratedPassword()
        listOf(
            """
            {"_type":"cmd", "action":"invalid"}
            """.trimIndent()
        ).forEach {
            broker.publish(
                false,
                "owntracks/someuser/somedevice",
                Qos.AT_LEAST_ONCE,
                MQTT5Properties(),
                it.toByteArray().toUByteArray()
            )
        }
        // Wait for it to not crash
        BaristaSleepInteractions.sleep(1, TimeUnit.SECONDS)
    }
}
