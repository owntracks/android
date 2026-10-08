package org.owntracks.android.location.profiles

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager.PERMISSION_GRANTED
import android.os.Build
import androidx.core.content.ContextCompat
import androidx.core.content.IntentCompat
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber

/** Keeps track of which Bluetooth devices are currently connected */
@Singleton
class ConnectedBluetoothDevices
@Inject
constructor(@param:ApplicationContext private val context: Context) {
  private val bluetoothAdapter: BluetoothAdapter? =
      context.getSystemService(BluetoothManager::class.java)?.adapter

  private val refreshes = MutableSharedFlow<Unit>(extraBufferCapacity = 1)

  /**
   * The addresses of the connected devices. Bluetooth is only watched while this is collected, and
   * the first value isn't emitted until the devices that were already connected are known.
   */
  @OptIn(ExperimentalCoroutinesApi::class)
  val addresses: Flow<Set<String>> =
      refreshes.onStart { emit(Unit) }.flatMapLatest { watchAddresses() }.distinctUntilChanged()

  /** Looks at the connected devices again, e.g. once we've been given permission to see them */
  fun refresh() {
    refreshes.tryEmit(Unit)
  }

  private fun watchAddresses(): Flow<Set<String>> = callbackFlow {
    val addresses = MutableStateFlow<Set<String>?>(null)
    val receiver =
        object : BroadcastReceiver() {
          override fun onReceive(context: Context, intent: Intent) {
            when (intent.action) {
              BluetoothDevice.ACTION_ACL_CONNECTED ->
                  intent.bluetoothDevice()?.run { addresses.update { it.orEmpty() + address } }
              BluetoothDevice.ACTION_ACL_DISCONNECTED ->
                  intent.bluetoothDevice()?.run { addresses.update { it.orEmpty() - address } }
              BluetoothAdapter.ACTION_STATE_CHANGED ->
                  if (
                      intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR) !=
                          BluetoothAdapter.STATE_ON
                  ) {
                    addresses.value = emptySet()
                  }
            }
          }
        }
    ContextCompat.registerReceiver(
        context,
        receiver,
        IntentFilter().apply {
          addAction(BluetoothDevice.ACTION_ACL_CONNECTED)
          addAction(BluetoothDevice.ACTION_ACL_DISCONNECTED)
          addAction(BluetoothAdapter.ACTION_STATE_CHANGED)
        },
        ContextCompat.RECEIVER_NOT_EXPORTED,
    )
    findConnectedDevices { connected -> addresses.update { it.orEmpty() + connected } }
    val forwarding = launch { addresses.filterNotNull().collect { send(it) } }
    awaitClose {
      forwarding.cancel()
      context.unregisterReceiver(receiver)
    }
  }

  fun hasPermission(): Boolean =
      Build.VERSION.SDK_INT < Build.VERSION_CODES.S ||
          ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) ==
              PERMISSION_GRANTED

  /** The devices paired with this one, to choose from */
  @SuppressLint("MissingPermission")
  fun bondedDevices(): List<BluetoothDevice> =
      if (hasPermission()) {
        try {
          bluetoothAdapter?.bondedDevices?.toList() ?: emptyList()
        } catch (e: SecurityException) {
          Timber.w(e, "Unable to list bonded Bluetooth devices")
          emptyList()
        }
      } else {
        emptyList()
      }

  /**
   * Finds the devices that are already connected, as connection broadcasts only tell us about
   * changes. Asks the audio profiles, as that's what cars and headsets connect with. Calls
   * [onFound] once with everything that was found.
   */
  @SuppressLint("MissingPermission")
  private fun findConnectedDevices(onFound: (Set<String>) -> Unit) {
    val adapter = bluetoothAdapter
    if (!hasPermission() || adapter == null || !adapter.isEnabled) {
      onFound(emptySet())
      return
    }
    val profiles = listOf(BluetoothProfile.A2DP, BluetoothProfile.HEADSET)
    val found = mutableSetOf<String>()
    var answered = 0
    fun answer(connected: Collection<String>) =
        synchronized(found) {
          found += connected
          if (++answered == profiles.size) onFound(found.toSet())
        }
    profiles.forEach { profile ->
      val listening =
          try {
            adapter.getProfileProxy(
                context,
                object : BluetoothProfile.ServiceListener {
                  override fun onServiceConnected(profile: Int, proxy: BluetoothProfile) {
                    val connected =
                        try {
                          proxy.connectedDevices.map { it.address }
                        } catch (e: SecurityException) {
                          Timber.w(e, "Unable to get connected Bluetooth devices")
                          emptyList()
                        } finally {
                          adapter.closeProfileProxy(profile, proxy)
                        }
                    Timber.d("Bluetooth devices connected on profile $profile: $connected")
                    answer(connected)
                  }

                  override fun onServiceDisconnected(profile: Int) {}
                },
                profile,
            )
          } catch (e: SecurityException) {
            Timber.w(e, "Unable to get Bluetooth profile $profile")
            false
          }
      if (!listening) answer(emptyList())
    }
  }

  private fun Intent.bluetoothDevice(): BluetoothDevice? =
      IntentCompat.getParcelableExtra(
          this,
          BluetoothDevice.EXTRA_DEVICE,
          BluetoothDevice::class.java,
      )
}
