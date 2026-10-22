package org.owntracks.android.location.profiles

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import androidx.core.content.ContextCompat
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.flatMapLatest
import org.owntracks.android.data.waypoints.WaypointsRepo
import org.owntracks.android.net.WifiInfoProvider

@Singleton
class AndroidDeviceContextProvider
@Inject
constructor(
    @param:ApplicationContext private val context: Context,
    private val wifiInfoProvider: WifiInfoProvider,
    private val connectedBluetoothDevices: ConnectedBluetoothDevices,
    private val waypointsRepo: WaypointsRepo,
) : DeviceContextProvider {
  /**
   * How the device is charging. The battery status is broadcast whenever it changes, including how
   * it's plugged in, so this also follows e.g. a wireless charger that takes a while to start.
   */
  private val chargingSource: Flow<ChargingSource?> = callbackFlow {
    val receiver =
        object : BroadcastReceiver() {
          override fun onReceive(context: Context, intent: Intent) {
            trySend(chargingSourceFromPlugged(intent.getIntExtra(BatteryManager.EXTRA_PLUGGED, 0)))
          }
        }
    // The battery status is sticky, so this also tells the receiver the current one
    ContextCompat.registerReceiver(
        context,
        receiver,
        IntentFilter(Intent.ACTION_BATTERY_CHANGED),
        ContextCompat.RECEIVER_NOT_EXPORTED,
    )
    awaitClose { context.unregisterReceiver(receiver) }
  }
      .distinctUntilChanged()

  /** Regions aren't known until the waypoints have been loaded */
  @OptIn(ExperimentalCoroutinesApi::class)
  private val enteredWaypointTsts: Flow<Set<Long>> =
      waypointsRepo.migrationCompleteFlow
          .filter { it }
          .flatMapLatest { waypointsRepo.enteredWaypointTsts }

  override val deviceContext: Flow<DeviceContext> =
      combine(
          wifiInfoProvider.connectedSSID.orFallbackAfter(SOURCE_TIMEOUT, null),
          connectedBluetoothDevices.addresses.orFallbackAfter(SOURCE_TIMEOUT, emptySet()),
          chargingSource.orFallbackAfter(SOURCE_TIMEOUT, null),
          enteredWaypointTsts.orFallbackAfter(SOURCE_TIMEOUT, emptySet()),
      ) { ssid, bluetoothDeviceAddresses, chargingSource, enteredWaypointTsts ->
        DeviceContext(ssid, bluetoothDeviceAddresses, chargingSource, enteredWaypointTsts)
      }

  companion object {
    /** How long to wait for any one part of the context, before assuming there's nothing to know */
    private val SOURCE_TIMEOUT = 3.seconds
  }
}
