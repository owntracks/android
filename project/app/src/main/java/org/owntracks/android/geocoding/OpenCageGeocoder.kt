package org.owntracks.android.geocoding

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import okhttp3.HttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import org.owntracks.android.services.MessageProcessorEndpointHttp
import timber.log.Timber
import java.math.BigDecimal

class OpenCageGeocoder @JvmOverloads internal constructor(private val apiKey: String, private val httpClient: OkHttpClient = OkHttpClient()) : CachingGeocoder() {
    private val jsonMapper: ObjectMapper = ObjectMapper()
    override fun doLookup(latitude: BigDecimal, longitude: BigDecimal): GeocodeResult {
        val url = HttpUrl.Builder()
                .scheme("http")
                .host(OPENCAGE_HOST)
                .addPathSegment("geocode")
                .addPathSegment("v1")
                .addPathSegment("json")
                .addEncodedQueryParameter("q", String.format("%s,%s", latitude, longitude))
                .addQueryParameter("no_annotations", "1")
                .addQueryParameter("abbrv", "1")
                .addQueryParameter("limit", "1")
                .addQueryParameter("no_dedupe", "1")
                .addQueryParameter("no_record", "1")
                .addQueryParameter("key", apiKey)
                .build()
        val request = Request.Builder()
                .url(url)
                .header("User-Agent", MessageProcessorEndpointHttp.USERAGENT)
                .get()
                .build()
        try {
            httpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful || response.body == null) {
                    Timber.e("Unexpected response from Opencage: %s", response)
                }
                val rs = response.body!!.string()
                Timber.d("Opencage HTTP response: %s", rs)
                val deserializedOpenCageResponse = jsonMapper.readValue(rs, OpenCageResponse::class.java)
                if (deserializedOpenCageResponse.formatted == null) {
                    Timber.e("No reverse geocode was received. Results in response: ${deserializedOpenCageResponse.results}")
                    return GeocodeResult.Empty
                }
                val formattedLocation = deserializedOpenCageResponse.formatted!!
                Timber.d("Formatted location: %s", formattedLocation)
                return GeocodeResult.Formatted(formattedLocation)
            }
        } catch (e: Exception) {
            Timber.e(e, "Error reverse geocoding from opencage")
            return GeocodeResult.Error("Error reverse geocoding from opencage")
        }
    }

    companion object {
        private const val OPENCAGE_HOST = "api.opencagedata.com"
    }

    init {
        jsonMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
    }

    internal class OpenCageResult {
        val formatted: String? = null
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    internal class OpenCageResponse {
        val results: List<OpenCageResult>? = null
        val formatted: String?
            get() = if (results != null && results.isNotEmpty()) results[0].formatted else null
    }
}