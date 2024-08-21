package org.owntracks.android.preferences.types

import com.fasterxml.jackson.annotation.JsonValue

enum class AppTheme(@JsonValue val value: Int) {
  LIGHT(0),
  DARK(1),
  AUTO(2);

  companion object {
    @JvmStatic
    @FromConfiguration
    fun getByValue(value: Int): AppTheme = entries.firstOrNull { it.value == value } ?: LIGHT

    @JvmStatic
    @FromConfiguration
    fun getByValue(value: String): AppTheme =
        (value.toIntOrNull() ?: Int.MIN_VALUE).run(::getByValue)
  }
}
