package org.owntracks.android.net.mqtt

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import java.security.KeyStore
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import org.eclipse.paho.client.mqttv3.IMqttToken
import org.eclipse.paho.client.mqttv3.MqttAsyncClient
import org.eclipse.paho.client.mqttv3.MqttException
import org.eclipse.paho.client.mqttv3.internal.ClientComms
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.mockito.Mockito.mockConstruction
import org.mockito.Mockito.mockStatic
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.doReturnConsecutively
import org.mockito.kotlin.doThrow
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.same
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.owntracks.android.data.repos.EndpointStateRepo
import org.owntracks.android.model.Parser
import org.owntracks.android.preferences.Preferences
import org.owntracks.android.preferences.types.ConnectionMode
import org.owntracks.android.preferences.types.MqttProtocolLevel
import org.owntracks.android.preferences.types.MqttQos
import org.owntracks.android.services.worker.Scheduler
import org.owntracks.android.test.SimpleIdlingResource

/**
 * The keepalive defects that let a connection wedge: a ping alarm per schedule that piled up to
 * Android's 500-per-app cap, whose failure then escaped into Paho's receiver thread; a connection
 * check that called a healthy link dead on nearly every watchdog run; and an endpoint replacement
 * that cancelled the location ping and the watchdog.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class MqttKeepaliveLivenessTest {
  private val alarmManager: AlarmManager = mock {}
  private val pingIntent: PendingIntent = mock {}
  private val context: Context = mock {}

  private fun sender() = AlarmPingSender(context, alarmManager) { pingIntent }

  @Test
  fun `every schedule targets the one ping intent, so it replaces rather than adds an alarm`() {
    val s = sender()
    s.schedule(60_000)
    s.schedule(60_000)
    verify(alarmManager, times(2))
        .setExactAndAllowWhileIdle(eq(AlarmManager.RTC_WAKEUP), any(), same(pingIntent))
  }

  @Test
  fun `stop cancels the ping alarm it scheduled`() {
    val s = sender()
    s.schedule(60_000)
    s.stop()
    verify(alarmManager).cancel(same(pingIntent))
  }

  @Test
  fun `a client torn down after its successor connected leaves the successor's alarm`() {
    val old = sender()
    val successor = sender()
    old.schedule(60_000)
    successor.schedule(60_000)
    old.stop()
    verify(alarmManager, never()).cancel(any<PendingIntent>())
    successor.stop()
    verify(alarmManager).cancel(same(pingIntent))
  }

  @Test
  fun `the ping intent is always the one fixed request code, addressed to this package`() {
    // The property the whole fix rests on: same request code + same intent = the same
    // PendingIntent, so AlarmManager replaces the alarm. A random code per schedule is what piled
    // them up.
    val ctx: Context = mock { on { packageName } doReturn "org.owntracks.android.debug" }
    mockConstruction(Intent::class.java).use { intents ->
      mockStatic(PendingIntent::class.java).use { pi ->
        pi.`when`<PendingIntent> { PendingIntent.getBroadcast(any(), any(), any(), any()) }
            .thenReturn(pingIntent)
        AlarmPingSender.pingPendingIntent(ctx)
        AlarmPingSender.pingPendingIntent(ctx)
        pi.verify(
            {
              PendingIntent.getBroadcast(
                  same(ctx),
                  eq(AlarmPingSender.PING_REQUEST_CODE),
                  any(),
                  any(),
              )
            },
            times(2),
        )
        intents.constructed().forEach { verify(it).setPackage("org.owntracks.android.debug") }
      }
    }
  }

  @Test
  fun `an alarm-cap failure does not escape into Paho's receiver thread`() {
    val failing: AlarmManager = mock {
      on { setExactAndAllowWhileIdle(any(), any(), anyOrNull<PendingIntent>()) } doThrow
          IllegalStateException("Maximum limit of concurrent alarms 500 reached")
    }
    AlarmPingSender(context, failing) { pingIntent }.schedule(60_000) // must not throw
  }

  @Test
  fun `start schedules at the keepalive`() {
    val comms: ClientComms = mock { on { keepAlive } doReturn 3_600_000L }
    sender().apply {
      init(comms)
      start()
    }
    verify(alarmManager)
        .setExactAndAllowWhileIdle(eq(AlarmManager.RTC_WAKEUP), any(), same(pingIntent))
  }

  // --- checkConnection ---

  private val configuration =
      MqttConnectionConfiguration(
          tls = false,
          ws = true,
          host = "example.com",
          port = 9001,
          clientId = "test",
          username = "",
          password = "",
          keepAlive = 3600.seconds,
          timeout = 30.seconds,
          cleanSession = false,
          mqttProtocolLevel = MqttProtocolLevel.MQTT_3_1_1,
          tlsClientCertAlias = "",
          willTopic = "owntracks/test/test",
          topicsToSubscribeTo = emptySet(),
          subQos = MqttQos.One,
          maxInFlight = 10,
      )

  private val scheduler: Scheduler = mock {}

  private fun endpointWith(
      client: MqttAsyncClient,
      connectionMode: ConnectionMode = ConnectionMode.MQTT,
  ): MQTTMessageProcessorEndpoint {
    val prefs: Preferences = mock { on { mode } doReturn connectionMode }
    val cm: ConnectivityManager = mock {}
    val am: AlarmManager = mock {}
    val ctx: Context = mock {
      on { getSystemService(ConnectivityManager::class.java) } doReturn cm
      on { getSystemService(AlarmManager::class.java) } doReturn am
    }
    return MQTTMessageProcessorEndpoint(
            messageProcessor = mock {},
            endpointStateRepo = EndpointStateRepo(),
            scheduler = scheduler,
            preferences = prefs,
            parser = mock<Parser> {},
            caKeyStore = KeyStore.getInstance(KeyStore.getDefaultType()).also { it.load(null) },
            scope = CoroutineScope(SupervisorJob() + UnconfinedTestDispatcher()),
            ioDispatcher = UnconfinedTestDispatcher(),
            applicationContext = ctx,
            mqttConnectionIdlingResource = SimpleIdlingResource("test", true),
        )
        .also {
          it.mqttClientAndConfiguration =
              MQTTMessageProcessorEndpoint.MqttClientAndConfiguration(client, configuration)
        }
  }

  @Test
  fun `no ping due on a connected, idle link is healthy`() {
    // This used to read as dead, so the watchdog reconnected a healthy link on nearly every run.
    val client: MqttAsyncClient = mock {
      on { checkPing(anyOrNull(), anyOrNull()) } doReturn null
      on { isConnected } doReturn true
      on { inFlightMessageCount } doReturn 0
    }
    assertTrue(endpointWith(client).checkConnection())
  }

  @Test
  fun `no ping due and sent messages never acknowledged reads as dead`() {
    val client: MqttAsyncClient = mock {
      on { checkPing(anyOrNull(), anyOrNull()) } doReturn null
      on { isConnected } doReturn true
      on { inFlightMessageCount } doReturn 2
    }
    val endpoint = endpointWith(client).apply { inFlightGrace = 300.milliseconds }
    assertFalse(endpoint.checkConnection())
  }

  @Test
  fun `a publish acknowledged within the grace does not read as dead`() {
    val client: MqttAsyncClient = mock {
      on { checkPing(anyOrNull(), anyOrNull()) } doReturn null
      on { isConnected } doReturn true
      on { inFlightMessageCount } doReturnConsecutively listOf(1, 1, 0)
    }
    assertTrue(endpointWith(client).checkConnection())
  }

  @Test
  fun `a negative in-flight count (resumed QoS-2 flow) counts as drained`() {
    val client: MqttAsyncClient = mock {
      on { checkPing(anyOrNull(), anyOrNull()) } doReturn null
      on { isConnected } doReturn true
      on { inFlightMessageCount } doReturn -1
    }
    assertTrue(endpointWith(client).checkConnection())
  }

  @Test
  fun `no ping due on a client that knows it is disconnected reads as dead`() {
    val client: MqttAsyncClient = mock {
      on { checkPing(anyOrNull(), anyOrNull()) } doReturn null
      on { isConnected } doReturn false
    }
    assertFalse(endpointWith(client).checkConnection())
  }

  @Test
  fun `a due ping that comes back is healthy, one that times out is not`() {
    val answered: IMqttToken = mock {}
    val ok: MqttAsyncClient = mock { on { checkPing(anyOrNull(), anyOrNull()) } doReturn answered }
    assertTrue(endpointWith(ok).checkConnection())

    val silent: IMqttToken = mock { on { waitForCompletion(any()) } doThrow MqttException(32000) }
    val dead: MqttAsyncClient = mock { on { checkPing(anyOrNull(), anyOrNull()) } doReturn silent }
    assertFalse(endpointWith(dead).checkConnection())
  }

  // --- endpoint replacement ---

  @Test
  fun `replacing the endpoint leaves the ping and the watchdog scheduled`() {
    val client: MqttAsyncClient = mock { on { isConnected } doReturn true }
    endpointWith(client).deactivate()
    verify(scheduler, never()).cancelAllTasks()
    verify(scheduler, never()).cancelMqttTasks()
  }

  @Test
  fun `leaving MQTT mode cancels the MQTT work but not the location ping`() {
    val client: MqttAsyncClient = mock { on { isConnected } doReturn true }
    endpointWith(client, ConnectionMode.HTTP).deactivate()
    verify(scheduler).cancelMqttTasks()
    verify(scheduler, never()).cancelAllTasks()
  }

  @Test
  fun `a replaced endpoint schedules no reconnect when its connection drops`() {
    val client: MqttAsyncClient = mock { on { isConnected } doReturn true }
    val endpoint = endpointWith(client)
    endpoint.deactivate()
    endpoint.mqttCallback.connectionLost(java.io.IOException("gone"))
    verify(scheduler, never()).scheduleMqttReconnect()
  }
}
