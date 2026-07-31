package org.owntracks.android.services.worker

import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Tests for the MQTT reconnect backoff curve.
 *
 * The delay is computed by the app rather than handed to WorkManager's [androidx.work.BackoffPolicy]
 * precisely so that it can be capped: WorkManager clamps its own backoff to five hours, which a run
 * of failures reaches in well under a day, after which the app never recovers on its own (see issue
 * #2294). The cap is therefore the property worth pinning down.
 */
class SchedulerReconnectBackoffTest {

  @Test
  fun `first attempt waits the initial delay`() {
    assertEquals(Scheduler.RECONNECT_INITIAL_DELAY, Scheduler.reconnectDelayForAttempt(0))
  }

  @Test
  fun `successive attempts double the delay`() {
    assertEquals(10.seconds, Scheduler.reconnectDelayForAttempt(0))
    assertEquals(20.seconds, Scheduler.reconnectDelayForAttempt(1))
    assertEquals(40.seconds, Scheduler.reconnectDelayForAttempt(2))
    assertEquals(80.seconds, Scheduler.reconnectDelayForAttempt(3))
    assertEquals(160.seconds, Scheduler.reconnectDelayForAttempt(4))
    assertEquals(320.seconds, Scheduler.reconnectDelayForAttempt(5))
  }

  @Test
  fun `delay is capped at the maximum`() {
    assertEquals(Scheduler.RECONNECT_MAX_DELAY, Scheduler.reconnectDelayForAttempt(6))
    assertEquals(Scheduler.RECONNECT_MAX_DELAY, Scheduler.reconnectDelayForAttempt(50))
  }

  /**
   * The regression that matters: a device that has been failing to connect all night must still be
   * retrying on a timescale of minutes, not the hours WorkManager's own backoff would have reached.
   */
  @Test
  fun `no number of failures backs off beyond the maximum`() {
    // Well past the point where an unbounded exponential would overflow a Long of nanoseconds.
    listOf(0, 1, 7, 31, 32, 33, 63, 64, 65, 1_000, Int.MAX_VALUE).forEach { attempt ->
      val delay = Scheduler.reconnectDelayForAttempt(attempt)
      assertTrue(
          "attempt $attempt backed off to $delay, beyond the ${Scheduler.RECONNECT_MAX_DELAY} cap",
          delay <= Scheduler.RECONNECT_MAX_DELAY)
      assertTrue("attempt $attempt produced a non-positive delay of $delay", delay.isPositive())
    }
  }

  @Test
  fun `a negative attempt count is treated as the first attempt`() {
    assertEquals(Scheduler.RECONNECT_INITIAL_DELAY, Scheduler.reconnectDelayForAttempt(-1))
    assertEquals(Scheduler.RECONNECT_INITIAL_DELAY, Scheduler.reconnectDelayForAttempt(Int.MIN_VALUE))
  }

  @Test
  fun `the cap is short enough to recover promptly`() {
    // Guards against someone quietly raising the ceiling to something that reintroduces the bug.
    assertTrue(
        "reconnect ceiling of ${Scheduler.RECONNECT_MAX_DELAY} is too long to recover promptly",
        Scheduler.RECONNECT_MAX_DELAY <= 15.minutes)
  }
}
