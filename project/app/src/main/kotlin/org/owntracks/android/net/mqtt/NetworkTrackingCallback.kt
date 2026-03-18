package org.owntracks.android.net.mqtt

import android.net.ConnectivityManager
import android.net.Network
import org.owntracks.android.data.EndpointState
import timber.log.Timber

/** A [ConnectivityManager.NetworkCallback] that tracks the network that's active. */
class NetworkTrackingCallback(
    private val endpointState: () -> EndpointState,
    private val reconnectFunction: () -> Unit,
    private val disconnectFunction: () -> Unit
) : ConnectivityManager.NetworkCallback() {
  var justRegistered = true
    private set

  // Tracks the network that was most recently reported as the default via onAvailable.
  // Android's registerDefaultNetworkCallback delivers onAvailable for the *new* default
  // network before onLost for the *old* one (documented behaviour). Without this, a
  // wifi→mobile switch leaves the client stuck in DISCONNECTED: onAvailable fires while
  // we're still CONNECTED on wifi (so the DISCONNECTED guard skips the reconnect), then
  // onLost tears down the wifi connection, and no further onAvailable ever fires because
  // mobile was already signalled as available. By recording which network we are currently
  // on, onAvailable can detect a switch and reconnect unconditionally, while onLost can
  // tell whether the lost network is the one we actually care about.
  var currentNetwork: Network? = null
    private set

  fun reset() {
    justRegistered = true
    currentNetwork = null
  }

  override fun onAvailable(network: Network) {
    Timber.v("Network becomes available: $network")
    if (justRegistered) {
      currentNetwork = network
      justRegistered = false
      return
    }
    if (network != currentNetwork) {
      // Default network has switched (e.g. wifi → mobile). Android delivers onAvailable
      // for the new network before onLost for the old one, so we may still appear
      // CONNECTED on the old network. Reconnect unconditionally so we bind to the new
      // network, and record it so the subsequent onLost for the old network is ignored.
      Timber.v("Default network changed from $currentNetwork to $network, reconnecting")
      currentNetwork = network
      reconnectFunction()
    } else if (endpointState() == EndpointState.DISCONNECTED) {
      Timber.v("Currently disconnected on same network, attempting reconnect")
      reconnectFunction()
    }
  }

  override fun onLost(network: Network) {
    if (network == currentNetwork) {
      // The network we were actually using is gone — disconnect.
      Timber.v("Current network $network lost, disconnecting")
      currentNetwork = null
      disconnectFunction()
    } else {
      // A non-current network was lost (e.g. the old wifi after a wifi→mobile switch).
      // onAvailable for the new network already triggered a reconnect; skip disconnect
      // to avoid racing with that in-flight reconnect.
      Timber.v("Non-current network $network lost, ignoring")
    }
  }
}
