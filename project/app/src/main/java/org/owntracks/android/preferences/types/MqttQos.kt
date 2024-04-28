package org.owntracks.android.preferences.types

import com.fasterxml.jackson.annotation.JsonValue

enum class MqttQos(@JsonValue val value: Int) {
  ZERO(0),
  ONE(1),
  TWO(2);

  companion object {
    @JvmStatic
    @FromConfiguration
    fun getByValue(value: Int): MqttQos = entries.firstOrNull { it.value == value } ?: ONE
  }
}
