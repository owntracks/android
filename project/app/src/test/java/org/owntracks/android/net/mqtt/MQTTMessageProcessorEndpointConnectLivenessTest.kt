package org.owntracks.android.net.mqtt

import android.app.AlarmManager
import android.content.Context
import android.net.ConnectivityManager
import java.security.KeyStore
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import org.eclipse.paho.client.mqttv3.MqttAsyncClient
import org.junit.Assert.assertEquals
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.doThrow
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.owntracks.android.data.repos.EndpointStateRepo
import org.owntracks.android.model.Parser
import org.owntracks.android.preferences.types.MqttProtocolLevel
import org.owntracks.android.preferences.types.MqttQos
import org.owntracks.android.services.worker.Scheduler
import org.owntracks.android.test.SimpleIdlingResource

/**
 * A connect whose CONNACK never came used to hold `connectingLock` forever, because the wait for it
 * had no timeout; every later reconnect, the watchdog's included, queued behind it. A client left
 * behind by such a connect has to be torn down forcibly, and only once.
 *
 * The connect itself builds a real Paho client, so the bound is pinned through [completionWait],
 * the one value both the CONNACK and SUBACK waits use.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class MQTTMessageProcessorEndpointConnectLivenessTest {

  private val mockConnectivityManager: ConnectivityManager = mock {}
  private val mockAlarmManager: AlarmManager = mock {}
  private val mockContext: Context = mock {
    on { getSystemService(ConnectivityManager::class.java) } doReturn mockConnectivityManager
    on { getSystemService(AlarmManager::class.java) } doReturn mockAlarmManager
  }

  private val configuration =
      MqttConnectionConfiguration(
          tls = false,
          ws = true,
          host = "example.com",
          port = 9001,
          clientId = "test",
          username = "",
          password = "",
          keepAlive = 60.seconds,
          timeout = 30.seconds,
          cleanSession = false,
          mqttProtocolLevel = MqttProtocolLevel.MQTT_3_1_1,
          tlsClientCertAlias = "",
          willTopic = "owntracks/test/test",
          topicsToSubscribeTo = emptySet(),
          subQos = MqttQos.One,
          maxInFlight = 1,
      )

  private fun endpoint() =
      MQTTMessageProcessorEndpoint(
          messageProcessor = mock {},
          endpointStateRepo = EndpointStateRepo(),
          scheduler = mock<Scheduler> {},
          preferences = mock {},
          parser = mock<Parser> {},
          caKeyStore = KeyStore.getInstance(KeyStore.getDefaultType()).also { it.load(null) },
          scope = CoroutineScope(SupervisorJob() + UnconfinedTestDispatcher()),
          ioDispatcher = UnconfinedTestDispatcher(),
          applicationContext = mockContext,
          mqttConnectionIdlingResource = SimpleIdlingResource("test", true),
      )

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

  @Test
  fun `a connected client is disconnected politely`() {
    val endpoint = endpoint()
    val client: MqttAsyncClient = mock { on { isConnected } doReturn true }
    endpoint.mqttClientAndConfiguration =
        MQTTMessageProcessorEndpoint.MqttClientAndConfiguration(client, configuration)

    endpoint.deactivate()

    verify(client).disconnect()
    verify(client, never()).disconnectForcibly(any(), any(), any())
    verify(client).close(true)
  }

  @Test
  fun `a client that never finished connecting is torn down forcibly, without a DISCONNECT`() {
    // A polite disconnect leaves the teardown to Paho's DisconnectBG, and close() right after it
    // only marks the client close-pending, so Paho never shuts it down.
    val endpoint = endpoint()
    val client: MqttAsyncClient = mock { on { isConnected } doReturn false }
    endpoint.mqttClientAndConfiguration =
        MQTTMessageProcessorEndpoint.MqttClientAndConfiguration(client, configuration)

    endpoint.deactivate()

    verify(client).disconnectForcibly(0L, 2_000L, false)
    verify(client, never()).disconnect()
    verify(client).close(true)
  }

  @Test
  fun `a closed client is not torn down again on the next disconnect`() {
    // "network lost" disconnects, then the reconnect disconnects first: the second pass used to
    // call disconnectForcibly on the closed client, whose nulled internals made Paho NPE.
    val endpoint = endpoint()
    val client: MqttAsyncClient = mock { on { isConnected } doReturn false }
    endpoint.mqttClientAndConfiguration =
        MQTTMessageProcessorEndpoint.MqttClientAndConfiguration(client, configuration)

    endpoint.deactivate()
    endpoint.deactivate()

    verify(client, times(1)).disconnectForcibly(0L, 2_000L, false)
    verify(client, times(1)).close(true)
  }

  @Test
  fun `a Paho runtime exception during teardown is contained and the client still closed`() {
    val endpoint = endpoint()
    val client: MqttAsyncClient = mock {
      on { isConnected } doReturn false
      on { disconnectForcibly(any(), any(), any()) } doThrow NullPointerException("tokenStore")
    }
    endpoint.mqttClientAndConfiguration =
        MQTTMessageProcessorEndpoint.MqttClientAndConfiguration(client, configuration)

    endpoint.deactivate()

    verify(client).close(true)
  }
}
