package org.owntracks.android.net

import android.annotation.SuppressLint
import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.net.wifi.WifiInfo
import android.net.wifi.WifiManager
import android.os.Build
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.onStart
import org.owntracks.android.model.messages.MessageStatus
import timber.log.Timber

@Singleton
class WifiInfoProvider @Inject constructor(@ApplicationContext context: Context) {
  @SuppressLint("WifiManagerPotentialLeak")
  private val wifiManager: WifiManager =
      context.getSystemService(Context.WIFI_SERVICE) as WifiManager

  private var ssid: String? = null
  private var bssid: String? = null

  private val connectivityManager =
      context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager

  private val connectedSSIDRefreshes = MutableSharedFlow<Unit>(extraBufferCapacity = 1)

  /**
   * The SSID of the connected Wi-Fi network, or null if there isn't one. Unlike [getSSID], this
   * follows Wi-Fi itself rather than the default network, so it's still known when e.g. a VPN is
   * the default network. Wi-Fi is only watched while this is collected.
   */
  @OptIn(ExperimentalCoroutinesApi::class)
  val connectedSSID: Flow<String?> =
      connectedSSIDRefreshes
          .onStart { emit(Unit) }
          .flatMapLatest { watchConnectedSSID() }
          .distinctUntilChanged()

  /**
   * Looks at the connected Wi-Fi network again. The SSID is redacted unless we have location
   * permission at the time, so this needs calling once we've been granted it.
   */
  fun refreshConnectedSSID() {
    connectedSSIDRefreshes.tryEmit(Unit)
  }

  private fun watchConnectedSSID(): Flow<String?> = callbackFlow {
    val connectivityManager =
        connectivityManager
            ?: run {
              send(null)
              awaitClose()
              return@callbackFlow
            }
    val callback =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
          object : ConnectivityManager.NetworkCallback(FLAG_INCLUDE_LOCATION_INFO) {
            override fun onCapabilitiesChanged(
                network: Network,
                networkCapabilities: NetworkCapabilities,
            ) {
              (networkCapabilities.transportInfo as? WifiInfo)?.run {
                trySend(getUnquotedSSID().takeIfKnownSSID())
              }
            }

            override fun onLost(network: Network) {
              trySend(null)
            }
          }
        } else {
          // Before S, the SSID isn't available on the network capabilities, so ask the WifiManager
          object : ConnectivityManager.NetworkCallback() {
            override fun onCapabilitiesChanged(
                network: Network,
                networkCapabilities: NetworkCapabilities,
            ) {
              trySend(getSSID()?.takeIfKnownSSID())
            }

            override fun onLost(network: Network) {
              trySend(null)
            }
          }
        }
    // The callback only reports Wi-Fi networks, so say straight away if there aren't any
    @Suppress("DEPRECATION")
    if (
        connectivityManager.allNetworks.none {
          connectivityManager
              .getNetworkCapabilities(it)
              ?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) == true
        }
    ) {
      send(null)
    }
    connectivityManager.registerNetworkCallback(
        NetworkRequest.Builder().addTransportType(NetworkCapabilities.TRANSPORT_WIFI).build(),
        callback,
    )
    awaitClose { connectivityManager.unregisterNetworkCallback(callback) }
  }

  init {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
      connectivityManager?.registerDefaultNetworkCallback(
          object : ConnectivityManager.NetworkCallback(FLAG_INCLUDE_LOCATION_INFO) {
            override fun onCapabilitiesChanged(
                network: Network,
                networkCapabilities: NetworkCapabilities,
            ) {
              if (networkCapabilities.transportInfo is WifiInfo) {
                ssid = (networkCapabilities.transportInfo as WifiInfo).getUnquotedSSID()
                bssid = (networkCapabilities.transportInfo as WifiInfo).bssid
              } else {
                ssid = null
                bssid = null
              }
              super.onCapabilitiesChanged(network, networkCapabilities)
            }
          }
      )
    }
  }

  /**
   * The networks seen in the most recent Wi-Fi scan, strongest first. Asks for a new scan too, but
   * that's throttled and finishes later, so its results are only seen the next time this is called.
   */
  @SuppressLint("MissingPermission") // Callers need location permission to be shown anything
  fun nearbySSIDs(): List<String> {
    // A failed (or throttled) scan request still leaves the previous scan's results to use
    try {
      @Suppress("DEPRECATION")
      if (!wifiManager.startScan()) Timber.d("Wi-Fi scan request was refused, probably throttled")
    } catch (e: SecurityException) {
      Timber.w(e, "Not allowed to request a Wi-Fi scan")
    }
    return try {
      nearbySSIDsBySignal(
          wifiManager.scanResults.map { @Suppress("DEPRECATION") it.SSID.unquoteSSID() to it.level }
      )
    } catch (e: SecurityException) {
      Timber.w(e, "Unable to get Wi-Fi scan results")
      emptyList()
    }
  }

  fun getBSSID(): String? =
      if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
        @Suppress("DEPRECATION") wifiManager.connectionInfo.bssid
      } else {
        bssid
      }

  fun getSSID(): String? =
      if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
        @Suppress("DEPRECATION") wifiManager.connectionInfo.getUnquotedSSID()
      } else {
        ssid
      }

  fun isConnected(): Boolean = getBSSID() != null

  fun isWiFiEnabled(): Int =
      if (wifiManager.isWifiEnabled) {
        MessageStatus.STATUS_WIFI_ENABLED
      } else {
        MessageStatus.STATUS_WIFI_DISABLED
      }
}

fun WifiInfo.getUnquotedSSID(): String = this.ssid.unquoteSSID()

private val quotedSSID = Regex("^\"(.*)\"$")

internal fun String.unquoteSSID(): String = replace(quotedSSID, "$1")

/** The SSID, unless it's been redacted (or is otherwise unknown) */
internal fun String.takeIfKnownSSID(): String? = takeUnless { it.isBlank() || it == UNKNOWN_SSID }

// WifiManager.UNKNOWN_SSID, which is only available from R
private const val UNKNOWN_SSID = "<unknown ssid>"

/** Distinct, known SSIDs from (SSID, signal level) pairs, strongest first */
internal fun nearbySSIDsBySignal(networks: List<Pair<String, Int>>): List<String> =
    networks
        .filter { (ssid, _) -> ssid.takeIfKnownSSID() != null }
        .sortedByDescending { (_, level) -> level }
        .map { (ssid, _) -> ssid }
        .distinct()
