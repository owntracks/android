package org.owntracks.android.location

import org.owntracks.android.preferences.types.FromConfiguration

enum class LocatorPriority {
  HighAccuracy,
  BalancedPowerAccuracy,
  LowPower,
  NoPower;

  companion object {
    @JvmStatic
    @FromConfiguration
    fun getByValue(value: String?): LocatorPriority? =
        LocatorPriority.entries.firstOrNull { it.name == value }
  }
}
