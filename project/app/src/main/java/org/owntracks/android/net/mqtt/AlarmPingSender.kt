package org.owntracks.android.net.mqtt

import android.annotation.SuppressLint
import android.app.AlarmManager
import android.app.PendingIntent
import android.app.PendingIntent.FLAG_IMMUTABLE
import android.app.PendingIntent.FLAG_UPDATE_CURRENT
import android.content.Context
import android.content.Intent
import androidx.core.content.getSystemService
import kotlin.random.Random
import kotlin.time.Duration.Companion.milliseconds
import kotlinx.datetime.Instant
import org.eclipse.paho.client.mqttv3.MqttPingSender
import org.eclipse.paho.client.mqttv3.internal.ClientComms
import timber.log.Timber

class AlarmPingSender(private val applicationContext: Context) : MqttPingSender {
  private val alarmManager: AlarmManager = applicationContext.getSystemService()!!
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
  }

  // We're not going to be even instantiated unless we can schedule exact alarms, ie the user has
  // explicitly white-listed us.
  @SuppressLint("MissingPermission")
  override fun schedule(delayInMilliseconds: Long) {
    Timber.v("MQTT keepalive scheduled in ${delayInMilliseconds.milliseconds}")
    try {
      Random.nextInt(0, Int.MAX_VALUE).run {
        alarmManager.setExactAndAllowWhileIdle(
            AlarmManager.RTC_WAKEUP,
            (System.currentTimeMillis() + delayInMilliseconds).also {
              Timber.v("Alarm time is ${Instant.fromEpochMilliseconds(it)}")
            },
            PendingIntent.getBroadcast(
                applicationContext,
                this,
                Intent().setAction(PING_INTENT_ACTION).putExtra("requestCode", this),
                FLAG_UPDATE_CURRENT or FLAG_IMMUTABLE,
            ))
        Timber.v("MQTT ping alarm intent requestcode=$this")
      }
    } catch (_: SecurityException) {
      Timber.w(
          "Unable to schedule MQTT ping alarm, looks like we don't have the necessary permissions")
    }
  }

  companion object {
    const val PING_INTENT_ACTION = "org.owntracks.android.debug.MQTT_PING"
  }
}
