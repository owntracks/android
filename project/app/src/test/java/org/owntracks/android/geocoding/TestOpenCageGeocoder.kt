package org.owntracks.android.geocoding

import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.doReturn
import com.nhaarman.mockitokotlin2.mock
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class TestOpenCageGeocoder {
    @Test
    fun testResultIsDeserializedCorrectly() {
        val openCageJSON = this.javaClass.getResource("/opencageResult.json")!!.readText()
        assertNotNull(openCageJSON)

        val httpResponse = Response.Builder()
                .body(openCageJSON.toResponseBody("application/json".toMediaTypeOrNull()))
                .request(Request.Builder().url("https://example.com").build())
                .protocol(Protocol.HTTP_1_1)
                .code(200)
                .message("Ok")
                .build()

        val httpCall: Call = mock { on { execute() } doReturn httpResponse }
        val httpClient: OkHttpClient = mock { on { newCall(any()) } doReturn httpCall }

        val geocoder = OpenCageGeocoder("", httpClient)

        val response = geocoder.reverse(0.0, 0.0)
        assertEquals("Friedrich-Ebert-Straße 7, 48153 Münster, Germany", response)
    }
}