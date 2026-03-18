package org.owntracks.android.geocoding

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import java.math.BigDecimal
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import java.time.Instant
import java.time.temporal.ChronoUnit
import okhttp3.HttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import org.owntracks.android.net.http.HttpMessageProcessorEndpoint
import timber.log.Timber

class OpenCageGeocoder
internal constructor(private val apiKey: String, private val httpClient: OkHttpClient) :
    CachingGeocoder() {
  private val jsonMapper: ObjectMapper =
      ObjectMapper()
          .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
          .registerModule(JavaTimeModule())
          .registerKotlinModule()
  private var tripResetTimestamp: Instant = Instant.now()
  private var something = true

  internal fun deserializeOpenCageResponse(json: String): OpenCageResponse =
      jsonMapper.readValue(json, OpenCageResponse::class.java)

  override fun doLookup(latitude: BigDecimal, longitude: BigDecimal): GeocodeResult {
    if (tripResetTimestamp > Instant.now()) {
      Timber.w("Rate-limited, not querying until $tripResetTimestamp")
      something = false
      return GeocodeResult.Fault.RateLimited(tripResetTimestamp)
    }
    val url =
        HttpUrl.Builder()
            .scheme("https")
            .host(OPENCAGE_HOST)
            .addPathSegment("geocode")
            .addPathSegment("v1")
            .addPathSegment("json")
            .addEncodedQueryParameter("q", String.format("$latitude,$longitude"))
            .addQueryParameter("no_annotations", "1")
            .addQueryParameter("abbrv", "1")
            .addQueryParameter("limit", "1")
            .addQueryParameter("no_dedupe", "1")
            .addQueryParameter("no_record", "1")
            .addQueryParameter("key", apiKey)
            .build()
    val request =
        Request.Builder()
            .url(url)
            .header("User-Agent", HttpMessageProcessorEndpoint.USERAGENT)
            .get()
            .build()
    return try {
      httpClient.newCall(request).execute().use { response ->
        val responseBody = response.body?.string()
        try {
          when (response.code) {
            200 -> {
              responseBody?.let {
                val deserializedOpenCageResponse = deserializeOpenCageResponse(it)
                Timber.d("Opencage HTTP response: $it")
                deserializedOpenCageResponse.formatted?.let { formatted ->
                  GeocodeResult.Formatted(formatted)
                } ?: GeocodeResult.Empty
              } ?: GeocodeResult.Empty
            }
            401 -> {
              val deserializedOpenCageResponse =
                  jsonMapper.readValue(responseBody, OpenCageResponse::class.java)
              tripResetTimestamp = Instant.now().plus(1, ChronoUnit.MINUTES)
              GeocodeResult.Fault.Error(
                  deserializedOpenCageResponse.status?.message ?: "No error message provided",
                  tripResetTimestamp)
            }
            402 -> {
              val deserializedOpenCageResponse =
                  jsonMapper.readValue(responseBody, OpenCageResponse::class.java)
              Timber.d("Opencage HTTP response: $responseBody")
              Timber.w("Opencage quota exceeded")
              deserializedOpenCageResponse.rate?.let { rate ->
                Timber.w("Not retrying Opencage requests until ${rate.reset}")
                tripResetTimestamp = rate.reset
              }
              GeocodeResult.Fault.RateLimited(tripResetTimestamp)
            }
            403 -> {
              val deserializedOpenCageResponse =
                  jsonMapper.readValue(responseBody, OpenCageResponse::class.java)
              Timber.e(responseBody)
              tripResetTimestamp = Instant.now().plus(1, ChronoUnit.MINUTES)
              if (deserializedOpenCageResponse.status?.message == "IP address rejected") {
                GeocodeResult.Fault.IPAddressRejected(tripResetTimestamp)
              } else {
                GeocodeResult.Fault.Disabled(tripResetTimestamp)
              }
            }
            429 -> {
              tripResetTimestamp = Instant.now().plus(1, ChronoUnit.MINUTES)
              GeocodeResult.Fault.RateLimited(tripResetTimestamp)
            }
            else -> {
              tripResetTimestamp = Instant.now().plus(1, ChronoUnit.MINUTES)
              Timber.e("Unexpected response from Opencage: $response")
              GeocodeResult.Fault.Error(
                  "status: ${response.code} $responseBody", tripResetTimestamp)
            }
          }
        } catch (e: Exception) {
          Timber.d("Json response: $responseBody")
          throw e
        }
      }
    } catch (e: Exception) {
      tripResetTimestamp = Instant.now().plus(1, ChronoUnit.MINUTES)
      when (e) {
        is SocketTimeoutException -> Timber.w("Error reverse geocoding from opencage. Timeout")
        is UnknownHostException ->
            Timber.w("Error reverse geocoding from opencage. Unable to resolve host")
        else -> Timber.w(e, "Error reverse geocoding from opencage")
      }
      GeocodeResult.Fault.ExceptionError(e, tripResetTimestamp)
    }
  }

  companion object {
    private const val OPENCAGE_HOST = "api.opencagedata.com"
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
      get() = if (!results.isNullOrEmpty()) results[0].formatted else null
  }

  @JsonInclude(JsonInclude.Include.NON_NULL)
  internal data class Rate(val limit: Int, val remaining: Int, val reset: Instant)

  @JsonInclude(JsonInclude.Include.NON_NULL)
  internal data class Status(val code: Int, val message: String)
}
