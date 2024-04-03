package org.owntracks.android.services

import android.content.Context
import android.net.wifi.WifiInfo
import android.net.wifi.WifiManager
import org.junit.Assert.assertEquals
import org.junit.Test
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.owntracks.android.net.WifiInfoProvider
import org.owntracks.android.net.getUnquotedSSID

class WifiInfoProviderTest {

  private val wifiInfo: WifiInfo = mock {
    on { ssid } doReturn "\"My SSID\""
    on { bssid } doReturn "12:34:56:78"
  }

  private val mockWifiManager: WifiManager = mock {
    @Suppress("DEPRECATION")
    on { connectionInfo } doReturn wifiInfo
  }

  private val mockContext: Context = mock {
    on { getSystemService(Context.WIFI_SERVICE) } doReturn mockWifiManager
  }

  @Test
  fun `given a WifiInfo object, when fetching the unquoted SSID, the SSID is returned without the surrounding quotes`() {
    assertEquals("My SSID", wifiInfo.getUnquotedSSID())
  }

  @Test
  fun `given a WifiInfoProvider, when getting the BSSID, the correct value is returned`() {
    val wifiInfoProvider = WifiInfoProvider(mockContext)
    assertEquals("12:34:56:78", wifiInfoProvider.getBSSID())
  }

  @Test
  fun `given a WifiInfoProvider, when getting the SSID, the correct unquoted value is returned`() {
    val wifiInfoProvider = WifiInfoProvider(mockContext)
    assertEquals("My SSID", wifiInfoProvider.getSSID())
  }
}
