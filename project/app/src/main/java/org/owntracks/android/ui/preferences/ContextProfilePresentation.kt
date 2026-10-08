package org.owntracks.android.ui.preferences

import org.owntracks.android.location.profiles.ContextProfile
import org.owntracks.android.location.profiles.LocatorOverrides
import org.owntracks.android.preferences.types.MonitoringMode

/**
 * The locator overrides that can be set on a profile, other than the monitoring mode, by the key of
 * their preference in the profile editor
 */
internal enum class LocatorOverride(val key: String) {
  LocatorPriority("locatorPriority"),
  LocatorInterval("locatorInterval"),
  MoveModeLocatorInterval("moveModeLocatorInterval"),
  LocatorDisplacement("locatorDisplacement"),
  Ping("ping"),
}

/**
 * The overrides that make a difference given the profile's monitoring mode and static location.
 * Without a monitoring mode, the user's own one applies, which could be any of them.
 */
internal fun relevantOverrides(overrides: LocatorOverrides): Set<LocatorOverride> {
  // Quiet and Manual still locate, they just don't publish (pings included)
  val pings = overrides.monitoring.let { it != MonitoringMode.Quiet && it != MonitoringMode.Manual }
  if (overrides.staticLocation != null) {
    // Nothing is located, so all that's left is how often the static location is sent
    return if (pings) setOf(LocatorOverride.Ping) else emptySet()
  }
  return when (overrides.monitoring) {
    null -> LocatorOverride.entries.toSet()
    MonitoringMode.Quiet,
    MonitoringMode.Manual ->
        setOf(
            LocatorOverride.LocatorPriority,
            LocatorOverride.LocatorInterval,
            LocatorOverride.LocatorDisplacement,
        )
    MonitoringMode.Significant ->
        setOf(
            LocatorOverride.LocatorPriority,
            LocatorOverride.LocatorInterval,
            LocatorOverride.LocatorDisplacement,
            LocatorOverride.Ping,
        )
    MonitoringMode.Move ->
        setOf(
            LocatorOverride.LocatorPriority,
            LocatorOverride.MoveModeLocatorInterval,
            LocatorOverride.Ping,
        )
  }
}

/** What the profiles are doing right now, for showing to the user */
internal sealed interface ProfileStatus {
  data object Off : ProfileStatus

  /** No profile applies, so the user's own settings are used */
  data object NoneApplies : ProfileStatus

  data class Active(val profile: ContextProfile) : ProfileStatus

  /** [profile] applies, but the user has manually changed the settings it would override */
  data class Paused(val profile: ContextProfile) : ProfileStatus
}

internal fun profileStatus(enabled: Boolean, matched: ContextProfile?, active: ContextProfile?) =
    when {
      !enabled -> ProfileStatus.Off
      active != null -> ProfileStatus.Active(active)
      matched != null -> ProfileStatus.Paused(matched)
      else -> ProfileStatus.NoneApplies
    }
