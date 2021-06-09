package org.owntracks.android.services

import android.content.Context
import android.net.wifi.WifiInfo
import android.net.wifi.WifiManager
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.doReturn
import com.nhaarman.mockitokotlin2.mock
import org.junit.Test

import org.junit.Assert.*

class WifiInfoProviderTest {

    private val wifiInfo: WifiInfo = mock {
        on { ssid } doReturn "\"My SSID\""
        on { bssid } doReturn "12:34:56:78"
    }

    private val mockWifiManager: WifiManager = mock {
        on { connectionInfo } doReturn wifiInfo
    }

    private val mockContext: Context = mock {
        on { getSystemService(Context.WIFI_SERVICE) } doReturn mockWifiManager
    }

    @Test
    fun `when given a WifiInfoProvider, when getting the BSSID, the correct value is returned`() {
        val wifiInfoProvider = WifiInfoProvider(mockContext)
        assertEquals("12:34:56:78", wifiInfoProvider.getBSSID())
    }

    @Test
    fun `when given a WifiInfoProvider, when getting the SSID, the correct unquoted value is returned`() {
        val wifiInfoProvider = WifiInfoProvider(mockContext)
        assertEquals("My SSID", wifiInfoProvider.getSSID())
    }
}