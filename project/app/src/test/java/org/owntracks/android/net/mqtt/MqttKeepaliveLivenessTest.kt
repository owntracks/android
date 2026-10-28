package org.owntracks.android.net.mqtt

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import org.eclipse.paho.client.mqttv3.internal.ClientComms
import org.junit.Test
import org.mockito.Mockito.mockConstruction
import org.mockito.Mockito.mockStatic
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.doThrow
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.same
import org.mockito.kotlin.times
import org.mockito.kotlin.verify

/**
 * The keepalive defects that let a connection wedge: a ping alarm per schedule that piled up to
 * Android's 500-per-app cap, whose failure then escaped into Paho's receiver thread.
 */
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
}
