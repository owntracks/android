package org.owntracks.android.preferences.types

import kotlinx.serialization.Serializable

@Serializable
enum class ReverseGeocodeProvider {
  None,
  Device,
  OpenCage;

  companion object {
    @JvmStatic
    @FromConfiguration
    fun getByValue(value: String): ReverseGeocodeProvider =
        entries.firstOrNull { it.name.equals(value, true) } ?: None
  }
}
