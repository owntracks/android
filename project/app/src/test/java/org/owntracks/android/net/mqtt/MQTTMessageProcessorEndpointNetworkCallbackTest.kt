package org.owntracks.android.net.mqtt

import android.app.AlarmManager
import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import java.security.KeyStore
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.owntracks.android.data.EndpointState
import org.owntracks.android.data.repos.EndpointStateRepo
import org.owntracks.android.model.Parser
import org.owntracks.android.services.worker.Scheduler
import org.owntracks.android.test.SimpleIdlingResource

/**
 * Tests for the [MQTTMessageProcessorEndpoint.networkChangeCallback] state machine.
 *
 * Android's [ConnectivityManager.registerDefaultNetworkCallback] delivers [onAvailable] for the
 * *new* default network before [onLost] for the *old* one during a network switch (e.g. WiFi →
 * mobile data). The [currentNetwork] field in the callback exists specifically to handle this
 * ordering: see the comment on that field for the full explanation of the bug it fixes.
 *
 * These tests verify the [currentNetwork] tracking logic and the guarding behaviour of [onLost]
 * without exercising the full MQTT reconnect/disconnect path.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class MQTTMessageProcessorEndpointNetworkCallbackTest {

  private val testDispatcher = UnconfinedTestDispatcher()

  // Use SupervisorJob + CoroutineExceptionHandler so coroutine failures inside reconnect()
  // (e.g. ConfigurationIncompleteException because preferences are not configured) do not
  // propagate out and fail the test.
  private val testScope =
      CoroutineScope(
          SupervisorJob() +
              testDispatcher +
              CoroutineExceptionHandler { _, _ -> /* swallow expected reconnect failures */ }
      )

  private val endpointStateRepo = EndpointStateRepo()

  private val mockConnectivityManager: ConnectivityManager = mock {}
  private val mockAlarmManager: AlarmManager = mock {}
  private val mockContext: Context = mock {
    on { getSystemService(ConnectivityManager::class.java) } doReturn mockConnectivityManager
    on { getSystemService(AlarmManager::class.java) } doReturn mockAlarmManager
  }

  private lateinit var endpoint: MQTTMessageProcessorEndpoint

  @Before
  fun setUp() {
    endpoint =
        MQTTMessageProcessorEndpoint(
            messageProcessor = mock {},
            endpointStateRepo = endpointStateRepo,
            scheduler = mock<Scheduler> {},
            preferences = mock {},
            parser = mock<Parser> {},
            caKeyStore = KeyStore.getInstance(KeyStore.getDefaultType()).also { it.load(null) },
            scope = testScope,
            ioDispatcher = testDispatcher,
            applicationContext = mockContext,
            mqttConnectionIdlingResource = SimpleIdlingResource("test", true),
        )
  }

  // ── justRegistered bootstrap ───────────────────────────────────────────────

  @Test
  fun `onAvailable when justRegistered records network and clears the flag`() {
    val network: Network = mock {}
    val cb = endpoint.networkChangeCallback
    cb.reset()

    cb.onAvailable(network)

    assertEquals(network, cb.currentNetwork)
    assertFalse(cb.justRegistered)
  }

  @Test
  fun `onAvailable when justRegistered does not trigger reconnect even while DISCONNECTED`() {
    // Reconnect is handled by activate() for the initial callback; onAvailable must not
    // double-trigger it on first registration.
    val network: Network = mock {}
    endpointStateRepo.endpointState.value = EndpointState.DISCONNECTED
    val cb = endpoint.networkChangeCallback
    cb.reset()

    cb.onAvailable(network)

    // State must not have changed (no reconnect was launched)
    assertEquals(EndpointState.DISCONNECTED, endpointStateRepo.endpointState.value)
  }

  // ── network switch (WiFi → mobile): the primary bug scenario ──────────────

  @Test
  fun `onAvailable for a different network while CONNECTED updates currentNetwork`() {
    // This is the exact bug scenario: mobile onAvailable fires before wifi onLost.
    // Without the fix the CONNECTED guard would block any reconnect attempt.
    val wifiNetwork: Network = mock {}
    val mobileNetwork: Network = mock {}
    val cb = endpoint.networkChangeCallback
    cb.reset()
    cb.onAvailable(wifiNetwork) // bootstrap: records wifi, clears justRegistered
    endpointStateRepo.endpointState.value = EndpointState.CONNECTED

    cb.onAvailable(mobileNetwork)

    // currentNetwork must be updated to mobile so that the subsequent onLost(wifi) is ignored.
    assertEquals(mobileNetwork, cb.currentNetwork)
  }

  @Test
  fun `onAvailable for the same network while CONNECTED does not change currentNetwork`() {
    val network: Network = mock {}
    val cb = endpoint.networkChangeCallback
    cb.reset()
    cb.onAvailable(network) // bootstrap: records network, clears justRegistered
    endpointStateRepo.endpointState.value = EndpointState.CONNECTED

    cb.onAvailable(network)

    assertEquals(network, cb.currentNetwork)
  }

  // ── recovery from total disconnection ─────────────────────────────────────

  @Test
  fun `onAvailable for a different network while DISCONNECTED updates currentNetwork`() {
    val oldNetwork: Network = mock {}
    val newNetwork: Network = mock {}
    val cb = endpoint.networkChangeCallback
    cb.reset()
    cb.onAvailable(oldNetwork) // bootstrap: records oldNetwork, clears justRegistered
    endpointStateRepo.endpointState.value = EndpointState.DISCONNECTED

    cb.onAvailable(newNetwork)

    assertEquals(newNetwork, cb.currentNetwork)
  }

  @Test
  fun `onAvailable for the same network while DISCONNECTED does not change currentNetwork`() {
    val network: Network = mock {}
    val cb = endpoint.networkChangeCallback
    cb.reset()
    cb.onAvailable(network) // bootstrap: records network, clears justRegistered
    endpointStateRepo.endpointState.value = EndpointState.DISCONNECTED

    cb.onAvailable(network)

    // currentNetwork is already correct; it must not be cleared or replaced.
    assertEquals(network, cb.currentNetwork)
  }

  // ── recovery from a failed connect attempt (issue #2294) ──────────────────

  /**
   * Drives a [NetworkTrackingCallback] directly, so that the reconnect can be observed rather than
   * inferred from endpoint state. Bootstraps [network] as the current network first, so the
   * subsequent onAvailable exercises the same-network branch.
   *
   * @return the number of reconnects triggered by the second onAvailable
   */
  private fun reconnectsOnRepeatOnAvailable(state: EndpointState): Int {
    val network: Network = mock {}
    var reconnects = 0
    val cb =
        NetworkTrackingCallback(
            endpointState = { state },
            reconnectFunction = { reconnects++ },
            disconnectFunction = {},
        )
    cb.onAvailable(network) // bootstrap: records network, clears justRegistered
    reconnects = 0

    cb.onAvailable(network)
    return reconnects
  }

  @Test
  fun `onAvailable for the same network while in ERROR triggers a reconnect`() {
    // A connect that fails (e.g. DNS not yet usable on a freshly-associated wifi) leaves the
    // endpoint in ERROR, not DISCONNECTED. The framework hands out no new Network once the link
    // settles, so this repeat onAvailable is the only signal we get and must not be ignored.
    assertEquals(1, reconnectsOnRepeatOnAvailable(EndpointState.ERROR))
  }

  @Test
  fun `onAvailable for the same network in any non-connected state triggers a reconnect`() {
    listOf(
            EndpointState.INITIAL,
            EndpointState.IDLE,
            EndpointState.DISCONNECTED,
            EndpointState.ERROR,
            EndpointState.ERROR_CONFIGURATION,
        )
        .forEach {
          assertEquals("state $it should reconnect", 1, reconnectsOnRepeatOnAvailable(it))
        }
  }

  @Test
  fun `onAvailable for the same network while CONNECTED does not trigger a reconnect`() {
    assertEquals(0, reconnectsOnRepeatOnAvailable(EndpointState.CONNECTED))
  }

  @Test
  fun `onAvailable for the same network while CONNECTING does not trigger a reconnect`() {
    // A connect attempt is genuinely in flight and will settle on CONNECTED or ERROR by itself;
    // reconnecting would tear down an attempt that may be about to succeed.
    assertEquals(0, reconnectsOnRepeatOnAvailable(EndpointState.CONNECTING))
  }

  // ── onLost guarding ───────────────────────────────────────────────────────

  @Test
  fun `onLost for the current network clears currentNetwork`() {
    val network: Network = mock {}
    val cb = endpoint.networkChangeCallback
    cb.reset()
    cb.onAvailable(network) // bootstrap: records network, clears justRegistered

    cb.onLost(network)

    assertNull(cb.currentNetwork)
  }

  @Test
  fun `onLost for a non-current network leaves currentNetwork unchanged`() {
    // After a WiFi → mobile switch, onLost(wifi) fires after onAvailable(mobile) has already
    // updated currentNetwork. The disconnect must be skipped to avoid racing with the reconnect
    // that onAvailable already triggered.
    val mobileNetwork: Network = mock {}
    val wifiNetwork: Network = mock {}
    val cb = endpoint.networkChangeCallback
    cb.reset()
    cb.onAvailable(mobileNetwork) // bootstrap: already on mobile

    cb.onLost(wifiNetwork) // stale loss notification for the old wifi

    assertEquals(mobileNetwork, cb.currentNetwork) // mobile network unaffected
  }
}
