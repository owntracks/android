package org.owntracks.android.mqtt

import android.os.ParcelFileDescriptor
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.platform.app.InstrumentationRegistry
import dagger.hilt.android.testing.HiltAndroidTest
import io.github.davidepianca98.mqtt.packets.mqtt.MQTTPingreq
import org.junit.After
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.owntracks.android.testutils.TestWithAnActivity
import org.owntracks.android.testutils.TestWithAnMQTTBroker
import org.owntracks.android.testutils.TestWithAnMQTTBrokerImpl
import org.owntracks.android.ui.map.MapActivity

/**
 * Keepalive is what notices a dead connection, and when it stops nothing fails visibly: the broker
 * just drops the client. On API 31 and later, pings go through an exact alarm and a dynamically
 * registered receiver, but only for apps allowed to schedule exact alarms, so the app is put on the
 * battery optimisation allowlist to test that path rather than the coroutine fallback.
 */
@ExperimentalUnsignedTypes
@LargeTest
@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class MQTTKeepaliveTests :
    TestWithAnActivity<MapActivity>(false), TestWithAnMQTTBroker by TestWithAnMQTTBrokerImpl() {

  private val packageName = InstrumentationRegistry.getInstrumentation().targetContext.packageName

  private fun shell(command: String) =
      ParcelFileDescriptor.AutoCloseInputStream(
              InstrumentationRegistry.getInstrumentation().uiAutomation.executeShellCommand(command)
          )
          .use { it.readBytes() }

  @Before
  fun allowExactAlarms() {
    shell("dumpsys deviceidle whitelist +$packageName")
  }

  @After
  fun disallowExactAlarms() {
    shell("dumpsys deviceidle whitelist -$packageName")
  }

  @Test
  fun given_an_idle_mqtt_connection_when_the_keepalive_interval_passes_then_pings_reach_the_broker() {
    setupTestActivity {
      configureMQTTConnectionToLocalWithGeneratedPassword(
          saveConfigurationIdlingResource,
          KEEPALIVE,
      )
    }

    Thread.sleep(37_000)

    val pings = mqttPacketsReceived.filterIsInstance<MQTTPingreq>().size
    assertTrue("Expected repeated pings over 37 seconds, got $pings", pings >= 2)
  }

  private companion object {
    /**
     * Not the test default of 5. Android delays an alarm to at least 5 seconds from now, and the
     * alarm that fires a few milliseconds before a ping is due has to schedule one more, so the
     * first ping can be 5 seconds late. At 5 seconds, the broker gives up first (at 1.5x
     * keepalive).
     */
    const val KEEPALIVE = 15
  }
}
