package org.owntracks.android.preferences.types

import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable

@Serializable(with = MqttProtocolLevel.MqttProtocolLevelSerializer::class)
enum class MqttProtocolLevel(val value: Int) {
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

  object MqttProtocolLevelSerializer :
      KSerializer<MqttProtocolLevel> by intValueEnumSerializer(
          "MqttProtocolLevel", entries, { it.value }, MQTT_3_1)
}
