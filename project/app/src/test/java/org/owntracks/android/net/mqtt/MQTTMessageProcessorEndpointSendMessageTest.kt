package org.owntracks.android.net.mqtt

import android.app.AlarmManager
import android.content.Context
import android.net.ConnectivityManager
import java.security.KeyStore
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.currentTime
import kotlinx.coroutines.test.runTest
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken
import org.eclipse.paho.client.mqttv3.MqttAsyncClient
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.doReturnConsecutively
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.owntracks.android.data.EndpointState
import org.owntracks.android.data.EndpointStatus
import org.owntracks.android.data.repos.EndpointStateRepo
import org.owntracks.android.model.Parser
import org.owntracks.android.model.messages.MessageBase
import org.owntracks.android.preferences.types.MqttProtocolLevel
import org.owntracks.android.preferences.types.MqttQos
import org.owntracks.android.services.worker.Scheduler
import org.owntracks.android.test.SimpleIdlingResource

/**
 * Tests for how [MQTTMessageProcessorEndpoint.sendMessage] waits for in-flight capacity.
 *
 * The wait used to be unbounded and pinned to whichever client was current when the send started.
 * If a reconnect replaced that client mid-wait, the old client's in-flight count never dropped, so
 * the outbound message loop wedged forever while the new, healthy client sat idle and the queue
 * grew (issue #2323). The wait must now give up (so the message gets re-queued) rather than hang.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class MQTTMessageProcessorEndpointSendMessageTest {

  private val endpointStateRepo = EndpointStateRepo()

  private val mockConnectivityManager: ConnectivityManager = mock {}
  private val mockAlarmManager: AlarmManager = mock {}
  private val mockContext: Context = mock {
    on { getSystemService(ConnectivityManager::class.java) } doReturn mockConnectivityManager
    on { getSystemService(AlarmManager::class.java) } doReturn mockAlarmManager
  }

  private val configuration =
      MqttConnectionConfiguration(
          tls = false,
          ws = false,
          host = "example.com",
          port = 1883,
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

  private val message: MessageBase = mock { on { toJsonBytes(any()) } doReturn byteArrayOf() }

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
            scope = CoroutineScope(SupervisorJob() + UnconfinedTestDispatcher()),
            ioDispatcher = UnconfinedTestDispatcher(),
            applicationContext = mockContext,
            mqttConnectionIdlingResource = SimpleIdlingResource("test", true),
        )
    endpointStateRepo.endpointState.value = EndpointStatus(EndpointState.CONNECTED)
  }

  private fun clientWithInFlight(vararg counts: Int): MqttAsyncClient = mock {
    on { inFlightMessageCount } doReturnConsecutively counts.toList()
    on { publish(anyOrNull(), any<ByteArray>(), any(), any()) } doReturn mock<IMqttDeliveryToken> {}
  }

  private fun connectWith(client: MqttAsyncClient) {
    endpoint.mqttClientAndConfiguration =
        MQTTMessageProcessorEndpoint.MqttClientAndConfiguration(client, configuration)
  }

  @Test
  fun `publishes once the in-flight count drops below the max`() = runTest {
    val client = clientWithInFlight(1, 1, 1, 0)
    connectWith(client)

    val result = endpoint.sendMessage(message)

    assertTrue("send should succeed: $result", result.isSuccess)
    verify(client).publish(anyOrNull(), any<ByteArray>(), any(), any())
  }

  @Test
  fun `gives up waiting when a reconnect replaces the client`() = runTest {
    // The old client's in-flight messages will never be acked: it's been replaced.
    val oldClient = clientWithInFlight(1)
    connectWith(oldClient)
    launch {
      delay(1.seconds)
      connectWith(clientWithInFlight(0))
    }

    val result = endpoint.sendMessage(message)

    assertTrue(
        "should fail as not connected so the message is re-queued: $result",
        result.exceptionOrNull() is MQTTMessageProcessorEndpoint.NotConnectedException,
    )
    assertTrue("should give up promptly, not wait out the timeout", currentTime < 2_000)
    verify(oldClient, never()).publish(anyOrNull(), any<ByteArray>(), any(), any())
  }

  @Test
  fun `gives up waiting when the endpoint disconnects`() = runTest {
    val client = clientWithInFlight(1)
    connectWith(client)
    launch {
      delay(1.seconds)
      endpointStateRepo.endpointState.value = EndpointStatus(EndpointState.DISCONNECTED)
    }

    val result = endpoint.sendMessage(message)

    assertTrue(
        "should fail as not connected so the message is re-queued: $result",
        result.exceptionOrNull() is MQTTMessageProcessorEndpoint.NotConnectedException,
    )
    assertTrue("should give up promptly, not wait out the timeout", currentTime < 2_000)
    verify(client, never()).publish(anyOrNull(), any<ByteArray>(), any(), any())
  }

  @Test
  fun `gives up waiting after a timeout if the in-flight count never drops`() = runTest {
    val client = clientWithInFlight(1)
    connectWith(client)

    val result = endpoint.sendMessage(message)

    assertTrue(
        "should fail as not connected so the message is re-queued: $result",
        result.exceptionOrNull() is MQTTMessageProcessorEndpoint.NotConnectedException,
    )
    assertTrue("should wait out the timeout first", currentTime >= 30_000)
    verify(client, never()).publish(anyOrNull(), any<ByteArray>(), any(), any())
  }
}
