package org.owntracks.android.preferences.types

import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable

@Serializable(with = MqttQos.MqttQosSerializer::class)
enum class MqttQos(val value: Int) {
  Zero(0),
  One(1),
  Two(2);

  companion object {
    @JvmStatic
    @FromConfiguration
    fun getByValue(value: Int): MqttQos = entries.firstOrNull { it.value == value } ?: One

    @JvmStatic
    @FromConfiguration
    fun getByValue(value: String): MqttQos =
        value.toIntOrNull()?.run(::getByValue)
            ?: entries.firstOrNull { it.name.equals(value, true) }
            ?: One
  }

  object MqttQosSerializer :
      KSerializer<MqttQos> by intValueEnumSerializer("MqttQos", entries, { it.value }, One)
}
