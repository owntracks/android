package org.owntracks.android.geocoding

import android.content.Context
import androidx.core.app.NotificationManagerCompat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import okhttp3.Call
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Test
import org.mockito.Mockito.spy
import org.mockito.Mockito.times
import org.mockito.Mockito.verify
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.spy
import org.owntracks.android.location.LatLng
import org.owntracks.android.preferences.InMemoryPreferencesStore
import org.owntracks.android.preferences.Preferences
import org.owntracks.android.preferences.types.ReverseGeocodeProvider
import org.owntracks.android.services.BackgroundService
import org.owntracks.android.test.SimpleIdlingResource

@OptIn(ExperimentalCoroutinesApi::class)
class TestGeocoderProvider {
  private val mockIdlingResource = SimpleIdlingResource("mock", true)

  @Test
  fun `Given a preference for the None Geocoder, when resolving a location via the GeocoderProvider, then the LatLng is returned`() =
      runTest {
        val mockContext: Context = mock {}
        val notificationManager: NotificationManagerCompat = mock {}
        val preferences = Preferences(InMemoryPreferencesStore(), mockIdlingResource)
        preferences.reverseGeocodeProvider = ReverseGeocodeProvider.NONE
        val provider =
            GeocoderProvider(
                mockContext,
                preferences,
                notificationManager,
                this,
                UnconfinedTestDispatcher(),
                OkHttpClient())
        val result = provider.resolve(LatLng(50.0, 0.0))
        advanceUntilIdle()
        assertEquals("50.0000, 0.0000", result)
      }

  @Test
  fun `Given a preference for the None Geocoder, when resolving a location with a BackgroundService via the GeocoderProvider, then the background service is notified`() =
      runTest {
        val mockContext: Context = mock {}
        val notificationManager: NotificationManagerCompat = mock {}
        val preferences = Preferences(InMemoryPreferencesStore(), mockIdlingResource)
        preferences.reverseGeocodeProvider = ReverseGeocodeProvider.NONE
        val provider =
            GeocoderProvider(
                mockContext,
                preferences,
                notificationManager,
                this,
                UnconfinedTestDispatcher(),
                OkHttpClient())
        val backgroundService: BackgroundService = spy {}
        val latLng = LatLng(50.0, 0.0)
        provider.resolve(latLng, backgroundService)
        advanceUntilIdle()
        verify(backgroundService, times(1)).onGeocodingProviderResult(latLng, "50.0000, 0.0000")
      }

  @Test
  fun `Given a preference for the OpenCage Geocoder, when resolving a location via the GeocoderProvider, then the LatLng is returned`() =
      runTest {
        val openCageJSON = this.javaClass.getResource("/openCage/opencageResult.json")!!.readText()
        val httpResponse =
            Response.Builder()
                .body(openCageJSON.toResponseBody("application/json".toMediaTypeOrNull()))
                .request(Request.Builder().url("https://example.com").build())
                .protocol(Protocol.HTTP_1_1)
                .code(200)
                .message("Ok")
                .build()

        val httpCall: Call = mock { on { execute() } doReturn httpResponse }
        val mockHttpClient: OkHttpClient = mock { on { newCall(any()) } doReturn httpCall }

        val mockContext: Context = mock {}
        val notificationManager: NotificationManagerCompat = mock {}
        val preferences = Preferences(InMemoryPreferencesStore(), mockIdlingResource)
        preferences.reverseGeocodeProvider = ReverseGeocodeProvider.OPENCAGE
        val provider =
            GeocoderProvider(
                mockContext,
                preferences,
                notificationManager,
                this,
                UnconfinedTestDispatcher(),
                mockHttpClient)
        val result = provider.resolve(LatLng(50.0, 0.0))
        advanceUntilIdle()
        assertEquals("Friedrich-Ebert-Straße 7, 48153 Münster, Germany", result)
      }
}
