package org.owntracks.android.preferences.types

import com.fasterxml.jackson.annotation.JsonValue

enum class MonitoringMode(@JsonValue val value: Int) {
  QUIET(-1),
  MANUAL(0),
  SIGNIFICANT(1),
  MOVE(2);

  fun next(): MonitoringMode =
      when (this) {
        QUIET -> MANUAL
        MANUAL -> SIGNIFICANT
        SIGNIFICANT -> MOVE
        MOVE -> QUIET
      }

  companion object {
    @JvmStatic
    @FromConfiguration
    fun getByValue(value: Int): MonitoringMode =
        entries.firstOrNull { it.value == value } ?: SIGNIFICANT

    @JvmStatic
    @FromConfiguration
    fun getByValue(value: String): MonitoringMode =
        (value.toIntOrNull() ?: Int.MIN_VALUE).run(::getByValue)
  }
}
