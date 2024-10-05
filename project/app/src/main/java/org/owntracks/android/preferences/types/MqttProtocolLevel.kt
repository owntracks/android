package org.owntracks.android.preferences.types

import com.fasterxml.jackson.annotation.JsonValue

enum class MqttProtocolLevel(@JsonValue val value: Int) {
  MQTT_3_1(3),
  MQTT_3_1_1(4);

  companion object {
    @JvmStatic
    @FromConfiguration
    fun getByValue(value: Int): MqttProtocolLevel =
        entries.firstOrNull { it.value == value } ?: MQTT_3_1

    @JvmStatic
    @FromConfiguration
    fun getByValue(value: String): MqttProtocolLevel =
        value.toIntOrNull()?.run(::getByValue)
            ?: entries.firstOrNull { it.name.equals(value, true) }
            ?: MQTT_3_1
  }
}
