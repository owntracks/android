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
import java.time.temporal.ChronoUnit

class OpenCageGeocoder @JvmOverloads internal constructor(private val apiKey: String, private val httpClient: OkHttpClient = OkHttpClient()) : CachingGeocoder() {
    private val jsonMapper: ObjectMapper = ObjectMapper().registerKotlinModule().registerModule(JavaTimeModule())
    private var quotaResetTimestamp: Instant = Instant.MIN
    override fun doLookup(latitude: BigDecimal, longitude: BigDecimal): GeocodeResult {
        if (quotaResetTimestamp > Instant.now()) {
            return GeocodeResult.RateLimited(quotaResetTimestamp)
        }
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
        return try {
            httpClient.newCall(request).execute().use { response ->
                val responseBody = response.body?.string()
                when (response.code) {
                    200 -> {
                        val deserializedOpenCageResponse = jsonMapper.readValue(responseBody, OpenCageResponse::class.java)
                        Timber.d("Opencage HTTP response: %s", responseBody)
                        return deserializedOpenCageResponse.formatted?.let { GeocodeResult.Formatted(it) }
                                ?: GeocodeResult.Empty
                    }
                    401 -> {
                        val deserializedOpenCageResponse = jsonMapper.readValue(responseBody, OpenCageResponse::class.java)
                        quotaResetTimestamp = Instant.now().plus(1, ChronoUnit.MINUTES)
                        GeocodeResult.Error(deserializedOpenCageResponse.status?.message
                                ?: "No error message provided")
                    }
                    402 -> {
                        val deserializedOpenCageResponse = jsonMapper.readValue(responseBody, OpenCageResponse::class.java)
                        Timber.d("Opencage HTTP response: %s", responseBody)
                        Timber.w("Opencage quota exceeded")
                        deserializedOpenCageResponse.rate?.let { rate ->
                            Timber.w("Not retrying Opencage requests until ${rate.reset}")
                            quotaResetTimestamp = rate.reset
                        }
                        GeocodeResult.RateLimited(quotaResetTimestamp)
                    }
                    403 -> {
                        val deserializedOpenCageResponse = jsonMapper.readValue(responseBody, OpenCageResponse::class.java)
                        Timber.e(responseBody)
                        quotaResetTimestamp = Instant.now().plus(1, ChronoUnit.MINUTES)
                        if (deserializedOpenCageResponse.status?.message == "IP address rejected") {
                            GeocodeResult.IPAddressRejected
                        } else {
                            GeocodeResult.Disabled
                        }

                    }
                    429 -> {
                        quotaResetTimestamp = Instant.now().plus(1, ChronoUnit.MINUTES)
                        GeocodeResult.RateLimited(quotaResetTimestamp)
                    }
                    else -> {
                        Timber.e("Unexpected response from Opencage: %s", response)
                        GeocodeResult.Error("status: ${response.code} $responseBody")
                    }
                }
            }

        } catch (e: Exception) {
            Timber.e(e, "Error reverse geocoding from opencage")
            GeocodeResult.Error(e.message ?: "No error provided")
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
        val status: Status? = null
        val formatted: String?
            get() = if (results != null && results.isNotEmpty()) results[0].formatted else null


    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    internal data class Rate(val limit: Int, val remaining: Int, val reset: Instant)

    @JsonInclude(JsonInclude.Include.NON_NULL)
    internal data class Status(val code: Int, val message: String)
}