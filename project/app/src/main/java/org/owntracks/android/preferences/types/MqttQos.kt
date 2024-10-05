package org.owntracks.android.preferences.types

import com.fasterxml.jackson.annotation.JsonValue

enum class MqttQos(@JsonValue val value: Int) {
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
}
