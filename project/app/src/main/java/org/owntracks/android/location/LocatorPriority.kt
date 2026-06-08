package org.owntracks.android.location

import kotlinx.serialization.Serializable
import org.owntracks.android.preferences.types.FromConfiguration

@Serializable
enum class LocatorPriority(private val value: Int) {
  HighAccuracy(3),
  BalancedPowerAccuracy(2),
  LowPower(1),
  NoPower(0);

  companion object {
    @JvmStatic
    @FromConfiguration
    fun getByValue(value: Int): LocatorPriority = entries.getOrElse(value) { BalancedPowerAccuracy }

    @JvmStatic
    @FromConfiguration
    fun getByValue(value: String): LocatorPriority =
        value.toIntOrNull()?.run(::getByValue)
            ?: entries.firstOrNull { it.name.equals(value, true) }
            ?: BalancedPowerAccuracy
  }
}
