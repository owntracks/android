package org.owntracks.android.support

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkCapabilities.NET_CAPABILITY_INTERNET
import android.net.NetworkCapabilities.TRANSPORT_CELLULAR
import android.net.NetworkCapabilities.TRANSPORT_WIFI
import org.junit.Assert.assertEquals
import org.junit.Test
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.owntracks.android.model.messages.MessageLocation.Companion.CONN_TYPE_MOBILE
import org.owntracks.android.model.messages.MessageLocation.Companion.CONN_TYPE_OFFLINE
import org.owntracks.android.model.messages.MessageLocation.Companion.CONN_TYPE_WIFI

internal class DeviceMetricsProviderTest {
  @Test
  fun `given a connectivity Service with no active network, when calling connectionType getter then a Connection Type of Offline is returned`() {
    val connectivityManager: ConnectivityManager = mock {
      on { getNetworkCapabilities(null) } doReturn (null)
    }
    val mockContext: Context = mock {
      on { getSystemService(Context.CONNECTIVITY_SERVICE) } doReturn connectivityManager
    }
    assertEquals(CONN_TYPE_OFFLINE, DeviceMetricsProvider(mockContext).connectionType)
  }

  @Test
  fun `given a connectivity Service with an active network but no capabilities, when calling connectionType getter then a Connection Type of Offline is returned`() {
    val network: Network = mock {}
    val connectivityManager: ConnectivityManager = mock {
      on { getNetworkCapabilities(network) } doReturn (null)
    }
    val mockContext: Context = mock {
      on { getSystemService(Context.CONNECTIVITY_SERVICE) } doReturn connectivityManager
    }
    assertEquals(CONN_TYPE_OFFLINE, DeviceMetricsProvider(mockContext).connectionType)
  }

  @Test
  fun `given a connectivity Service with an active network, internet capability and wifi transport, when calling connectionType getter then a Connection Type of Wifi is returned`() {
    val network: Network = mock {}
    val networkCapabilities: NetworkCapabilities = mock {
      on { hasCapability(NET_CAPABILITY_INTERNET) } doReturn (true)
      on { hasTransport(TRANSPORT_WIFI) } doReturn (true)
    }
    val connectivityManager: ConnectivityManager = mock {
      on { activeNetwork } doReturn (network)
      on { getNetworkCapabilities(network) } doReturn (networkCapabilities)
    }
    val mockContext: Context = mock {
      on { getSystemService(Context.CONNECTIVITY_SERVICE) } doReturn connectivityManager
    }
    assertEquals(CONN_TYPE_WIFI, DeviceMetricsProvider(mockContext).connectionType)
  }

  @Test
  fun `given a connectivity Service with an active network, internet capability and cellular transport, when calling connectionType getter then a Connection Type of Wifi is returned`() {
    val network: Network = mock {}
    val networkCapabilities: NetworkCapabilities = mock {
      on { hasCapability(NET_CAPABILITY_INTERNET) } doReturn (true)
      on { hasTransport(TRANSPORT_CELLULAR) } doReturn (true)
    }
    val connectivityManager: ConnectivityManager = mock {
      on { activeNetwork } doReturn (network)
      on { getNetworkCapabilities(network) } doReturn (networkCapabilities)
    }
    val mockContext: Context = mock {
      on { getSystemService(Context.CONNECTIVITY_SERVICE) } doReturn connectivityManager
    }
    assertEquals(CONN_TYPE_MOBILE, DeviceMetricsProvider(mockContext).connectionType)
  }

  @Test
  fun `given a connectivity Service with an active network but without internet capability, when calling connectionType getter then a Connection Type of Wifi is returned`() {
    val network: Network = mock {}
    val networkCapabilities: NetworkCapabilities = mock {
      on { hasCapability(NET_CAPABILITY_INTERNET) } doReturn (false)
    }
    val connectivityManager: ConnectivityManager = mock {
      on { activeNetwork } doReturn (network)
      on { getNetworkCapabilities(network) } doReturn (networkCapabilities)
    }
    val mockContext: Context = mock {
      on { getSystemService(Context.CONNECTIVITY_SERVICE) } doReturn connectivityManager
    }
    assertEquals(CONN_TYPE_OFFLINE, DeviceMetricsProvider(mockContext).connectionType)
  }
}
