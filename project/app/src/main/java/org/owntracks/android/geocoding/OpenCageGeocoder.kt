package org.owntracks.android.geocoding

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import okhttp3.HttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import org.owntracks.android.services.MessageProcessorEndpointHttp
import timber.log.Timber
import java.math.BigDecimal
import java.time.Instant

class OpenCageGeocoder @JvmOverloads internal constructor(private val apiKey: String, private val httpClient: OkHttpClient = OkHttpClient()) : CachingGeocoder() {
    private val jsonMapper: ObjectMapper = ObjectMapper().registerKotlinModule().registerModule(JavaTimeModule())
    private var quotaResetTimestamp: Instant = Instant.MIN
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
                return response.body?.let {
                    val deserializedOpenCageResponse = jsonMapper.readValue(it.string(), OpenCageResponse::class.java)
                    Timber.d("Opencage HTTP response: %s", it.toString())
                    when (response.code) {
                        200 -> {
                            return if (deserializedOpenCageResponse.formatted != null) {
                                Timber.d("Formatted location: %s", deserializedOpenCageResponse.formatted)
                                deserializedOpenCageResponse.formatted
                            } else {
                                Timber.e("No reverse geocode was received. Results in response: ${deserializedOpenCageResponse.results}")
                                null
                            }
                        }
                        402 -> {
                            Timber.w("Opencage quota exceeded")
                            deserializedOpenCageResponse.rate?.let { rate ->
                                Timber.w("Not retrying Opencage requests until ${rate.reset}")
                                quotaResetTimestamp = rate.reset
                            }
                            return null
                        }
                        else -> {
                            Timber.e("Unexpected response from Opencage: %s", response)
                            return null
                        }
                    }
                }
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
        val rate: Rate? = null
        val results: List<OpenCageResult>? = null
        val formatted: String?
            get() = if (results != null && results.isNotEmpty()) results[0].formatted else null


    }
    @JsonInclude(JsonInclude.Include.NON_NULL)
    internal data class Rate(val limit: Int, val remaining: Int, val reset: Instant)
}