package org.owntracks.android.services

import android.annotation.SuppressLint
import android.content.Context
import android.net.wifi.WifiManager
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WifiInfoProvider @Inject constructor(@ApplicationContext context: Context) {
    @SuppressLint("WifiManagerPotentialLeak")
    private val wifiManager: WifiManager =
        context.getSystemService(Context.WIFI_SERVICE) as WifiManager

    fun getBSSID(): String? = wifiManager.connectionInfo.bssid

    // WifiInfo::getSSID returns the SSID quoted for some reason
    fun getSSID(): String = wifiManager.connectionInfo.ssid.replace(Regex("^\"(.*)\"$"), "$1")

    fun isConnected(): Boolean = getBSSID() != null
}
