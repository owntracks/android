package org.owntracks.android.ui.preferences

import android.content.Context
import org.owntracks.android.R
import org.owntracks.android.location.profiles.ChargingSource
import org.owntracks.android.location.profiles.Condition
import org.owntracks.android.location.profiles.ConditionMatch
import org.owntracks.android.location.profiles.ContextProfile

/**
 * Describes a profile [Condition]. Regions are named from [waypointNames], keyed by waypoint
 * timestamp in epoch seconds.
 */
internal fun Context.describeCondition(condition: Condition, waypointNames: Map<Long, String>) =
    when (condition) {
      is Condition.WifiSsid -> getString(R.string.contextProfileConditionWifi, condition.ssid)
      Condition.AnyWifi -> getString(R.string.contextProfileConditionAnyWifi)
      is Condition.BluetoothDevice ->
          getString(R.string.contextProfileConditionBluetooth, condition.name)
      Condition.Charging -> getString(R.string.contextProfileConditionCharging)
      is Condition.ChargingFrom ->
          getString(
              R.string.contextProfileConditionChargingFrom,
              getString(condition.source.labelResource),
          )
      is Condition.InRegion ->
          waypointNames[condition.tst]?.let {
            getString(R.string.contextProfileConditionRegion, it)
          } ?: getString(R.string.contextProfileConditionRegionMissing)
    }

internal fun Context.describeContextProfile(
    profile: ContextProfile,
    waypointNames: Map<Long, String>,
): String {
  val descriptions = profile.conditions.map { describeCondition(it, waypointNames) }
  val joining =
      if (profile.match == ConditionMatch.All) {
        R.string.contextProfileSummaryAll
      } else {
        R.string.contextProfileSummaryAny
      }
  val conditions =
      if (descriptions.isEmpty()) {
        getString(R.string.contextProfileSummaryNoConditions)
      } else {
        descriptions
            .reduce { joined, description -> getString(joining, joined, description) }
            .let {
              if (profile.match == ConditionMatch.None) {
                getString(R.string.contextProfileSummaryNone, it)
              } else {
                it
              }
            }
      }
  return if (profile.enabled) {
    conditions
  } else {
    getString(R.string.contextProfileSummaryDisabled, conditions)
  }
}

internal val ChargingSource.labelResource: Int
  get() =
      when (this) {
        ChargingSource.Cable -> R.string.chargingSourceCable
        ChargingSource.Wireless -> R.string.chargingSourceWireless
        ChargingSource.Dock -> R.string.chargingSourceDock
      }
