package org.owntracks.android.net.mqtt

import kotlin.time.Duration.Companion.milliseconds
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.eclipse.paho.client.mqttv3.MqttPingSender
import org.eclipse.paho.client.mqttv3.internal.ClientComms
import timber.log.Timber

class AsyncPingSender(private val scope: CoroutineScope) : MqttPingSender {
  private lateinit var comms: ClientComms

  override fun init(comms: ClientComms) {
    Timber.d("Initializing MQTT keepalive AsyncPingSender with comms $comms")
    // Stop is not reliably called. We need to cancel the job here to avoid multiple jobs running
    keepaliveJob?.cancel()
    this.comms = comms
  }

  override fun start() {
    Timber.v("MQTT keepalive start")
    comms.run { schedule(keepAlive) }
  }

  override fun stop() {
    Timber.v("MQTT keepalive stop")
    keepaliveJob?.cancel()?.run { Timber.v("MQTT keepalive cancelled") }
  }

  private var keepaliveJob: Job? = null

  override fun schedule(delayInMilliseconds: Long) {
    Timber.v("MQTT keepalive scheduled in ${delayInMilliseconds.milliseconds}")
    keepaliveJob =
        scope.launch {
          delay(delayInMilliseconds)
          Timber.v("Sending keepalive")
          try {
            comms.checkForActivity()?.waitForCompletion()
                ?: Timber.w("MQTT keepalive token was null")
          } catch (e: Exception) {
            Timber.w(e, "Unable to send MQTT ping")
          }
        }
  }
}
