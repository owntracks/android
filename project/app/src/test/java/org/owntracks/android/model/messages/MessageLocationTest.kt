package org.owntracks.android.model.messages

import android.content.Context
import android.location.Location
import android.net.wifi.WifiInfo
import android.net.wifi.WifiManager
import android.net.wifi.WifiManager.UNKNOWN_SSID
import android.os.Build
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.owntracks.android.net.WifiInfoProvider

class MessageLocationTest {

  @Test
  fun `given a location, when creating a MessageLocation on Android O, the correct fields are set`() {
    val location: Location = mock {
      on { latitude } doReturn 51.0
      on { longitude } doReturn 0.3
      on { accuracy } doReturn 5.0f
      on { verticalAccuracyMeters } doReturn 10.0f
      on { hasVerticalAccuracy() } doReturn true
    }

    val messageLocation = MessageLocation.fromLocation(location, Build.VERSION_CODES.O)
    assertEquals(51.0, messageLocation.latitude, 0.0)
    assertEquals(0.3, messageLocation.longitude, 0.0)
    assertEquals(5, messageLocation.accuracy)
    assertEquals(10, messageLocation.verticalAccuracy)
    assertNull(messageLocation.bssid)
    assertNull(messageLocation.ssid)
  }

  @Test
  fun `given a location, when creating a MessageLocation on Android before O, the correct fields are set`() {
    val location: Location = mock {
      on { latitude } doReturn 51.0
      on { longitude } doReturn 0.3
      on { accuracy } doReturn 5.0f
      on { verticalAccuracyMeters } doReturn 10.0f
    }

    val messageLocation = MessageLocation.fromLocation(location, Build.VERSION_CODES.N)
    assertEquals(51.0, messageLocation.latitude, 0.0)
    assertEquals(0.3, messageLocation.longitude, 0.0)
    assertEquals(5, messageLocation.accuracy)
    assertEquals(0, messageLocation.verticalAccuracy)
    assertNull(messageLocation.bssid)
    assertNull(messageLocation.ssid)
  }

  @Test
  fun `given a location and a WifiInfo, when creating a MessageLocation, the correct SSID fields are set`() {
    val location: Location = mock {
      on { latitude } doReturn 51.0
      on { longitude } doReturn 0.3
      on { accuracy } doReturn 5.0f
      on { verticalAccuracyMeters } doReturn 10.0f
    }
    val wifiInfo: WifiInfo = mock {
      on { ssid } doReturn "\"My SSID\""
      on { bssid } doReturn "12:34:56:78"
    }

    val mockWifiManager: WifiManager = mock {
      on { @Suppress("DEPRECATION") connectionInfo } doReturn wifiInfo
    }

    val mockContext: Context = mock {
      on { getSystemService(Context.WIFI_SERVICE) } doReturn mockWifiManager
    }
    val wifiInfoProvider = WifiInfoProvider(mockContext)

    val messageLocation = MessageLocation.fromLocationAndWifiInfo(location, wifiInfoProvider)
    assertEquals("My SSID", messageLocation.ssid)
    assertEquals("12:34:56:78", messageLocation.bssid)
  }

  @Test
  fun `given a location and a WifiInfo representing a disconnected network, when creating a MessageLocation, the correct SSID fields are set`() {
    val location: Location = mock {
      on { latitude } doReturn 51.0
      on { longitude } doReturn 0.3
      on { accuracy } doReturn 5.0f
      on { verticalAccuracyMeters } doReturn 10.0f
    }
    val wifiInfo: WifiInfo = mock {
      on { ssid } doReturn UNKNOWN_SSID
      on { bssid } doReturn null
    }

    val mockWifiManager: WifiManager = mock {
      on { @Suppress("DEPRECATION") connectionInfo } doReturn wifiInfo
    }

    val mockContext: Context = mock {
      on { getSystemService(Context.WIFI_SERVICE) } doReturn mockWifiManager
    }
    val wifiInfoProvider = WifiInfoProvider(mockContext)

    val messageLocation = MessageLocation.fromLocationAndWifiInfo(location, wifiInfoProvider)
    assertNull(messageLocation.ssid)
    assertNull(messageLocation.bssid)
  }
}
