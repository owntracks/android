package org.owntracks.android.preferences.types

import com.fasterxml.jackson.annotation.JsonValue

enum class ConnectionMode(@JsonValue val value: Int) {
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
}
