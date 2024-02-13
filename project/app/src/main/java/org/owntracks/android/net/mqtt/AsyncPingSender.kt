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
  override fun init(comms: ClientComms?) {
    Timber.d("Initializing MQTT keepalive AsyncPingSender")
    this.comms = comms
  }

  private var comms: ClientComms? = null

  override fun start() {
    Timber.v("MQTT keepalive start")
    comms?.run { schedule(keepAlive) }
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
          comms?.checkForActivity()?.waitForCompletion()
              ?: Timber.w("MQTT keepalive token was null")
        }
  }
}
