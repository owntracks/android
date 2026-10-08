package org.owntracks.android.net

import android.content.Context
import android.net.wifi.WifiInfo
import android.net.wifi.WifiManager
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock

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

  @Test
  fun `a redacted SSID is not a known SSID`() {
    assertNull("<unknown ssid>".takeIfKnownSSID())
  }

  @Test
  fun `an empty SSID is not a known SSID`() {
    assertNull("".takeIfKnownSSID())
  }

  @Test
  fun `a real SSID is a known SSID`() {
    assertEquals("My SSID", "My SSID".takeIfKnownSSID())
  }

  @Test
  fun `nearby networks are listed once each, strongest first`() {
    assertEquals(
        listOf("Home-5G", "Home", "Neighbour"),
        nearbySSIDsBySignal(
            listOf("Home" to -70, "Neighbour" to -80, "Home-5G" to -40, "Home" to -50),
        ),
    )
  }

  @Test
  fun `hidden and redacted networks aren't listed`() {
    assertEquals(
        listOf("Home"),
        nearbySSIDsBySignal(listOf("" to -40, "<unknown ssid>" to -45, "Home" to -50)),
    )
  }
}
