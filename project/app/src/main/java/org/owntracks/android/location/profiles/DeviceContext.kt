package org.owntracks.android.location.profiles

/** A snapshot of the device state that [Condition]s are matched against. */
data class DeviceContext(
    val wifiSsid: String? = null,
    val bluetoothDeviceAddresses: Set<String> = emptySet(),
    /** How the device is charging, or null if it isn't */
    val chargingSource: ChargingSource? = null,
    /** The [org.owntracks.android.data.waypoints.WaypointModel.tst]s of the regions we're in */
    val enteredWaypointTsts: Set<Long> = emptySet(),
)

/**
 * The [ChargingSource] for a [android.os.BatteryManager.EXTRA_PLUGGED] value, or null if it isn't
 * plugged in
 */
internal fun chargingSourceFromPlugged(plugged: Int): ChargingSource? =
    when {
      plugged and (BATTERY_PLUGGED_AC or BATTERY_PLUGGED_USB) != 0 -> ChargingSource.Cable
      plugged and BATTERY_PLUGGED_WIRELESS != 0 -> ChargingSource.Wireless
      plugged and BATTERY_PLUGGED_DOCK != 0 -> ChargingSource.Dock
      else -> null
    }

// The BatteryManager constants, which aren't available to unit tests (and DOCK only from T)
private const val BATTERY_PLUGGED_AC = 1
private const val BATTERY_PLUGGED_USB = 2
private const val BATTERY_PLUGGED_WIRELESS = 4
private const val BATTERY_PLUGGED_DOCK = 8
