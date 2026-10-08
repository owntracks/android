package org.owntracks.android.location.profiles

import org.owntracks.android.location.LocatorPriority
import org.owntracks.android.preferences.Preferences
import org.owntracks.android.preferences.types.MonitoringMode

/**
 * The locator settings actually in effect: the user's preferences, with the overrides of the
 * effective [ContextProfile] (if any) applied on top.
 */
data class LocatorSettings(
    val monitoring: MonitoringMode,
    val locatorInterval: Int,
    val moveModeLocatorInterval: Int,
    val locatorDisplacement: Int,
    val locatorPriority: LocatorPriority?,
    val ping: Int,
    val staticLocation: StaticLocation?,
) {
  companion object {
    // WorkManager won't run periodic work more often than this anyway
    private const val MINIMUM_PING_MINUTES = 15

    fun from(preferences: Preferences, overrides: LocatorOverrides?): LocatorSettings =
        LocatorSettings(
            monitoring = overrides?.monitoring ?: preferences.monitoring,
            locatorInterval =
                overrides?.locatorInterval?.coerceAtLeast(1) ?: preferences.locatorInterval,
            moveModeLocatorInterval =
                overrides?.moveModeLocatorInterval?.coerceAtLeast(1)
                    ?: preferences.moveModeLocatorInterval,
            locatorDisplacement =
                overrides?.locatorDisplacement?.coerceAtLeast(0) ?: preferences.locatorDisplacement,
            locatorPriority = overrides?.locatorPriority ?: preferences.locatorPriority,
            ping = overrides?.ping?.coerceAtLeast(MINIMUM_PING_MINUTES) ?: preferences.ping,
            staticLocation = overrides?.staticLocation,
        )
  }
}
