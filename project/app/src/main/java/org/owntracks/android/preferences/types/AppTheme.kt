package org.owntracks.android.preferences.types

enum class AppTheme(val value: Int) {
  Light(0),
  Dark(1),
  Auto(2);

  companion object {
    @JvmStatic
    @FromConfiguration
    fun getByValue(value: Int): AppTheme = entries.firstOrNull { it.value == value } ?: Auto

    @JvmStatic
    @FromConfiguration
    fun getByValue(value: String): AppTheme =
        value.toIntOrNull()?.run(::getByValue)
            ?: entries.firstOrNull { it.name.equals(value, true) }
            ?: Auto
  }
}
