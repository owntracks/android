package org.owntracks.android.support

import com.nhaarman.mockitokotlin2.doReturn
import com.nhaarman.mockitokotlin2.mock
import okhttp3.*
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.mockito.ArgumentMatchers.any

class TestGeocoderOpencage {
    @Test
    fun testResultIsDeserializedCorrectly() {
        val openCageJSON = this.javaClass.getResource("/opencageResult.json")?.readText()
        assertNotNull(openCageJSON)

        val httpResponse = Response.Builder()
                .body(ResponseBody.create(MediaType.parse("application/json"), openCageJSON!!))
                .request(Request.Builder().url("https://example.com").build())
                .protocol(Protocol.HTTP_1_1)
                .code(200)
                .message("Ok")
                .build()

        val httpCall: Call = mock { on { execute() } doReturn httpResponse }
        val httpClient: OkHttpClient = mock { on { newCall(any()) } doReturn httpCall }
        
        val geocoder = GeocoderOpencage("", httpClient)

        val response = geocoder.reverse(0.0, 0.0)
        assertEquals("Friedrich-Ebert-Straße 7, 48153 Münster, Germany", response)
    }
}