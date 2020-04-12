package org.owntracks.android.support

import okhttp3.*
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito

class TestGeocoderOpencage {
    @Test
    fun testResultIsDeserializedCorrectly() {
        val openCageJSON = this.javaClass.getResource("/opencageResult.json")?.readText()
        assertNotNull(openCageJSON)

        val httpClient = Mockito.mock(OkHttpClient::class.java)
        val httpCall = Mockito.mock(Call::class.java)

        val httpResponse = Response.Builder()
                .body(ResponseBody.create(MediaType.parse("application/json"), openCageJSON!!))
                .request(Request.Builder().url("https://example.com").build())
                .protocol(Protocol.HTTP_1_1)
                .code(200)
                .message("Ok")
                .build()

        Mockito.`when`(httpClient.newCall(any())).thenReturn(httpCall)
        Mockito.`when`(httpCall.execute()).thenReturn(httpResponse)

        val geocoder = GeocoderOpencage("", httpClient)

        val response = geocoder.reverse(0.0, 0.0)
        assertEquals("Friedrich-Ebert-Straße 7, 48153 Münster, Germany", response)
    }
}