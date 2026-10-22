package org.owntracks.android.testutils.di

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import org.owntracks.android.data.waypoints.WaypointsRepo
import org.owntracks.android.location.profiles.ChargingSource
import org.owntracks.android.location.profiles.DeviceContext
import org.owntracks.android.location.profiles.DeviceContextProvider

/**
 * A device whose Wi-Fi, Bluetooth and charging the test sets, as an emulator's can't be. Regions
 * come from the real waypoints, so that region conditions still follow transitions.
 */
class FakeDeviceContextProvider(waypointsRepo: WaypointsRepo) : DeviceContextProvider {
  private val device = MutableStateFlow(DeviceContext())

  override val deviceContext: Flow<DeviceContext> =
      combine(device, waypointsRepo.enteredWaypointTsts) { device, enteredWaypointTsts ->
        device.copy(enteredWaypointTsts = enteredWaypointTsts)
      }

  fun setWifi(ssid: String?) = device.update { it.copy(wifiSsid = ssid) }

  fun setBluetoothDevices(vararg addresses: String) = device.update {
    it.copy(bluetoothDeviceAddresses = addresses.toSet())
  }

  fun setCharging(source: ChargingSource?) = device.update { it.copy(chargingSource = source) }
}
