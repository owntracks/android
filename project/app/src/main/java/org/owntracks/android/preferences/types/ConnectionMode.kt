package org.owntracks.android.preferences.types

import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable

@Serializable(with = ConnectionMode.ConnectionModeSerializer::class)
enum class ConnectionMode(val value: Int) {
  MQTT(0),
  HTTP(3);

  companion object {
    @JvmStatic
    @FromConfiguration
    fun getByValue(value: Int): ConnectionMode = entries.firstOrNull { it.value == value } ?: MQTT

    @JvmStatic
    @FromConfiguration
    fun getByValue(value: String): ConnectionMode =
        value.toIntOrNull()?.run(::getByValue)
            ?: entries.firstOrNull { it.name.equals(value, true) }
            ?: MQTT
  }

  object ConnectionModeSerializer :
      KSerializer<ConnectionMode> by intValueEnumSerializer(
          "ConnectionMode", entries, { it.value }, MQTT)
}
