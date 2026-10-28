package org.owntracks.android.net.mqtt

import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * A connect whose CONNACK never came used to hold `connectingLock` forever, because the wait for it
 * had no timeout; every later reconnect, the watchdog's included, queued behind it.
 *
 * The connect itself builds a real Paho client, so the bound is pinned through [completionWait],
 * the one value both the CONNACK and SUBACK waits use.
 */
class MQTTMessageProcessorEndpointConnectLivenessTest {

  @Test
  fun `a broker reply is waited for the connection timeout plus a margin, never unbounded`() {
    assertEquals(60.seconds, MQTTMessageProcessorEndpoint.completionWait(30.seconds))
    assertEquals(40.seconds, MQTTMessageProcessorEndpoint.completionWait(10.seconds))
  }

  @Test
  fun `a zero or sub-second timeout still yields a finite wait`() {
    // getConnectOptions coerces the connection timeout to at least 1 s; the wait does the same
    // rather than trusting it, so Paho's "0 = wait forever" can never reach waitForCompletion.
    assertEquals(31.seconds, MQTTMessageProcessorEndpoint.completionWait(Duration.ZERO))
    assertEquals(31.seconds, MQTTMessageProcessorEndpoint.completionWait(200.milliseconds))
  }
}
