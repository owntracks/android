package org.owntracks.android.net.mqtt

import android.annotation.SuppressLint
import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import androidx.core.content.getSystemService
import kotlin.time.Duration.Companion.milliseconds
import org.eclipse.paho.client.mqttv3.MqttPingSender
import org.eclipse.paho.client.mqttv3.internal.ClientComms
import timber.log.Timber

class AlarmPingSender(private val applicationContext: Context) : MqttPingSender {
  private lateinit var alarmReceiver: AlarmReceiver
  private var comms: ClientComms? = null
  private val alarmManager: AlarmManager = applicationContext.getSystemService()!!
  private var pendingIntent: PendingIntent? = null

  override fun init(comms: ClientComms) {
    Timber.d("Initializing MQTT keepalive AlarmPingSender")
    this.comms = comms
    this.alarmReceiver = AlarmReceiver(comms)
    Timber.d("AlarmPingSender initialized. alarmReceiver=$alarmReceiver")
  }

  override fun start() {
    Timber.v("MQTT keepalive start")
    val action = this.javaClass.simpleName + comms!!.client.clientId
    comms?.run {
      applicationContext.registerReceiver(alarmReceiver, IntentFilter(action))
      pendingIntent =
          PendingIntent.getBroadcast(
              applicationContext,
              0,
              Intent(action),
              PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
      schedule(keepAlive)
    }
  }

  override fun stop() {
    Timber.v("MQTT keepalive stop")
    applicationContext.unregisterReceiver(alarmReceiver)
  }

  // We're not going to be even instantiated unless we can schedule exact alarms, ie the user has
  // explicitly white-listed us.
  @SuppressLint("MissingPermission")
  override fun schedule(delayInMilliseconds: Long) {
    Timber.v("MQTT keepalive scheduled in ${delayInMilliseconds.milliseconds}")
    alarmManager.setExactAndAllowWhileIdle(
        AlarmManager.RTC_WAKEUP, System.currentTimeMillis() + delayInMilliseconds, pendingIntent)
  }

  class AlarmReceiver(private val clientComms: ClientComms) : BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
      clientComms.checkForActivity()?.waitForCompletion()
          ?: Timber.w("MQTT keepalive token was null")
    }
  }
}
