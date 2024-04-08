package org.owntracks.android.net.mqtt

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import org.eclipse.paho.client.mqttv3.MqttAsyncClient
import timber.log.Timber

class MQTTPingAlarmReceiver(private val mqttClient: MqttAsyncClient) : BroadcastReceiver() {
  override fun onReceive(context: Context, intent: Intent) {
    Timber.v("alarm received with requestCode ${intent.getIntExtra("requestCode", -1)}")
    mqttClient.checkPing(null, null)
  }
}
