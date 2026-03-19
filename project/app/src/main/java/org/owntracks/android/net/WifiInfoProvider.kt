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
import org.owntracks.android.model.messages.MessageStatus

@Singleton
class WifiInfoProvider @Inject constructor(@ApplicationContext context: Context) {
  @SuppressLint("WifiManagerPotentialLeak")
  private val wifiManager: WifiManager =
      context.getSystemService(Context.WIFI_SERVICE) as WifiManager

  private var ssid: String? = null
  private var bssid: String? = null

  init {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
      val connectivityManager: ConnectivityManager =
          context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

      // Register callback specifically for WiFi networks, not just default network.
      // This ensures we track WiFi even when mobile data is the default network.
      val wifiNetworkRequest = NetworkRequest.Builder()
          .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
          .build()

      connectivityManager.registerNetworkCallback(
          wifiNetworkRequest,
          object : ConnectivityManager.NetworkCallback(FLAG_INCLUDE_LOCATION_INFO) {
            override fun onCapabilitiesChanged(
                network: Network,
                networkCapabilities: NetworkCapabilities
            ) {
              if (networkCapabilities.transportInfo is WifiInfo) {
                ssid = (networkCapabilities.transportInfo as WifiInfo).getUnquotedSSID()
                bssid = (networkCapabilities.transportInfo as WifiInfo).bssid
              }
              super.onCapabilitiesChanged(network, networkCapabilities)
            }

            override fun onLost(network: Network) {
              // Clear WiFi info when WiFi network is lost
              ssid = null
              bssid = null
              super.onLost(network)
            }
          })
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

  /**
   * Update SSID/BSSID from network capabilities if they contain WiFi info.
   * Only updates when WiFi info is present - doesn't clear when receiving
   * non-WiFi capabilities (e.g., mobile data) to handle dual-connectivity.
   */
  fun updateFromCapabilities(networkCapabilities: NetworkCapabilities) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
      if (networkCapabilities.transportInfo is WifiInfo) {
        ssid = (networkCapabilities.transportInfo as WifiInfo).getUnquotedSSID()
        bssid = (networkCapabilities.transportInfo as WifiInfo).bssid
      }
      // Don't clear SSID for non-WiFi capabilities - WiFi might still be connected
    }
  }

  /**
   * Clear WiFi info. Call this when WiFi network is lost.
   */
  fun clearWifiInfo() {
    ssid = null
    bssid = null
  }
}

fun WifiInfo.getUnquotedSSID(): String = this.ssid.replace(Regex("^\"(.*)\"$"), "$1")
