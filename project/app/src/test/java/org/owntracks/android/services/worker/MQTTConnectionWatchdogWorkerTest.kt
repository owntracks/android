package org.owntracks.android.services.worker

import kotlin.time.Duration.Companion.minutes
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.owntracks.android.data.EndpointState

/**
 * Tests for the connection watchdog's reconnect policy.
 *
 * The watchdog exists because every other reconnect trigger in the app is reactive, so anything
 * they collectively miss is missed permanently (issue #2294). The case that motivates it is a
 * connection that is dead but still believed to be up, which produces no event of any kind — so
 * that is the case most worth pinning down here.
 */
class MQTTConnectionWatchdogWorkerTest {

  @Test
  fun `a connected endpoint that passes its connection check is left alone`() {
    assertFalse(
        MQTTConnectionWatchdogWorker.shouldReconnect(
            EndpointState.CONNECTED, connectionCheckPassed = true))
  }

  @Test
  fun `a connected endpoint that fails its connection check is reconnected`() {
    // The half-open connection: the state says CONNECTED and nothing has raised an error, but
    // nothing can actually be published. Without this check it is never noticed at all.
    assertTrue(
        MQTTConnectionWatchdogWorker.shouldReconnect(
            EndpointState.CONNECTED, connectionCheckPassed = false))
  }

  @Test
  fun `every non-connected state is reconnected`() {
    EndpointState.entries
        .filter { it != EndpointState.CONNECTED }
        .forEach {
          assertTrue(
              "state $it should have triggered a reconnect",
              // connectionCheckPassed cannot be true unless the state is CONNECTED, but assert
              // against both so the state alone is demonstrably sufficient.
              MQTTConnectionWatchdogWorker.shouldReconnect(it, connectionCheckPassed = false) &&
                  MQTTConnectionWatchdogWorker.shouldReconnect(it, connectionCheckPassed = true))
        }
  }

  @Test
  fun `a stuck connect attempt is reconnected`() {
    // Unlike the connectivity callback, which leaves an in-flight attempt alone, the watchdog runs
    // only every fifteen minutes: a connect still outstanding on that timescale is stuck.
    assertTrue(
        MQTTConnectionWatchdogWorker.shouldReconnect(
            EndpointState.CONNECTING, connectionCheckPassed = false))
    assertTrue(Scheduler.CONNECTION_WATCHDOG_INTERVAL >= 15.minutes)
  }

  @Test
  fun `the watchdog runs often enough to be a useful backstop`() {
    // Guards the ceiling on how long a silently-dead connection can persist unnoticed.
    assertTrue(
        "watchdog interval of ${Scheduler.CONNECTION_WATCHDOG_INTERVAL} is too infrequent",
        Scheduler.CONNECTION_WATCHDOG_INTERVAL <= 30.minutes)
  }
}
