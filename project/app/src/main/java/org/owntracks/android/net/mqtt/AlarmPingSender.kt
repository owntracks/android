package org.owntracks.android.net.mqtt

import android.annotation.SuppressLint
import android.app.AlarmManager
import android.app.PendingIntent
import android.app.PendingIntent.FLAG_IMMUTABLE
import android.app.PendingIntent.FLAG_UPDATE_CURRENT
import android.content.Context
import android.content.Intent
import androidx.core.content.getSystemService
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.ExperimentalTime
import kotlin.time.Instant
import org.eclipse.paho.client.mqttv3.MqttPingSender
import org.eclipse.paho.client.mqttv3.internal.ClientComms
import timber.log.Timber

/**
 * There is exactly one ping alarm. Each [schedule] replaces it (a fixed request code, so the same
 * PendingIntent) and [stop] cancels it. With a random request code every schedule added an alarm
 * that kept rescheduling itself, so they accumulated with every reconnect and watchdog check until
 * Android's 500-per-app cap made `setExactAndAllowWhileIdle` throw — inside Paho's CONNACK
 * handling, which wedged the connect.
 */
@OptIn(ExperimentalTime::class)
class AlarmPingSender(
    private val applicationContext: Context,
    private val alarmManager: AlarmManager = applicationContext.getSystemService()!!,
    private val pingIntent: () -> PendingIntent = { pingPendingIntent(applicationContext) },
) : MqttPingSender {
  private lateinit var comms: ClientComms

  override fun init(comms: ClientComms) {
    Timber.d("Initializing MQTT keepalive AlarmPingSender")
    this.comms = comms
  }

  override fun start() {
    Timber.v("MQTT keepalive start. Keepalive is ${comms.keepAlive}")
    schedule(comms.keepAlive)
  }

  override fun stop() {
    Timber.v("MQTT keepalive stop.")
    // The alarm is shared by every client (one PendingIntent), so only the sender that scheduled it
    // last may cancel it — a client being torn down after its successor connected must not cancel
    // the successor's keepalive.
    synchronized(Companion) {
      if (owner !== this) return
      owner = null
    }
    try {
      alarmManager.cancel(pingIntent())
    } catch (e: RuntimeException) {
      Timber.w(e, "Unable to cancel the MQTT ping alarm")
    }
  }

  // We're not going to be even instantiated unless we can schedule exact alarms, ie the user has
  // explicitly white-listed us.
  @SuppressLint("MissingPermission")
  override fun schedule(delayInMilliseconds: Long) {
    Timber.v("MQTT keepalive scheduled in ${delayInMilliseconds.milliseconds}")
    synchronized(Companion) { owner = this }
    try {
      alarmManager.setExactAndAllowWhileIdle(
          AlarmManager.RTC_WAKEUP,
          (System.currentTimeMillis() + delayInMilliseconds).also {
            Timber.v("Alarm time is ${Instant.fromEpochMilliseconds(it)}")
          },
          pingIntent(),
      )
    } catch (_: SecurityException) {
      Timber.w(
          "Unable to schedule MQTT ping alarm, looks like we don't have the necessary permissions"
      )
    } catch (e: RuntimeException) {
      // E.g. IllegalStateException at the per-app alarm cap. This runs on Paho's receiver thread
      // during CONNACK handling; letting it escape kills the receiver and leaves the connect token
      // incomplete forever.
      Timber.w(e, "Unable to schedule MQTT ping alarm")
    }
  }

  companion object {
    const val PING_INTENT_ACTION = "org.owntracks.android.debug.MQTT_PING"

    /** One request code, so every schedule targets (and replaces) one alarm. */
    internal const val PING_REQUEST_CODE = 0x07_A1

    /** The sender whose schedule the one alarm currently carries. */
    private var owner: AlarmPingSender? = null

    /**
     * The one ping PendingIntent, addressed to this package only (the action string is shared by
     * the debug and release builds, which would otherwise feed each other's pings).
     */
    fun pingPendingIntent(context: Context): PendingIntent =
        PendingIntent.getBroadcast(
            context,
            PING_REQUEST_CODE,
            Intent(PING_INTENT_ACTION).apply { setPackage(context.packageName) },
            FLAG_UPDATE_CURRENT or FLAG_IMMUTABLE,
        )
  }
}
