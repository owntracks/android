package org.owntracks.android.net

import android.annotation.SuppressLint
import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
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

      connectivityManager.registerDefaultNetworkCallback(
          object : ConnectivityManager.NetworkCallback(FLAG_INCLUDE_LOCATION_INFO) {
            override fun onCapabilitiesChanged(
                network: Network,
                networkCapabilities: NetworkCapabilities
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
}

fun WifiInfo.getUnquotedSSID(): String = this.ssid.replace(Regex("^\"(.*)\"$"), "$1")
