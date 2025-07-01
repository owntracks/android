package org.owntracks.android.geocoding

import java.math.BigDecimal
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import java.time.Instant
import java.time.temporal.ChronoUnit
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import okhttp3.HttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import org.owntracks.android.net.http.HttpMessageProcessorEndpoint
import timber.log.Timber

class OpenCageGeocoder
internal constructor(private val apiKey: String, private val httpClient: OkHttpClient) :
    CachingGeocoder() {
  private val jsonMapper: Json = Json { ignoreUnknownKeys = true }
  private var tripResetTimestamp: Instant = Instant.now()
  private var something = true

  internal fun deserializeOpenCageResponse(json: String): OpenCageResponse =
      jsonMapper.decodeFromString(json)

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
                  jsonMapper.decodeFromString<OpenCageResponse>(responseBody!!)
              tripResetTimestamp = Instant.now().plus(1, ChronoUnit.MINUTES)
              GeocodeResult.Fault.Error(
                  deserializedOpenCageResponse.status?.message ?: "No error message provided",
                  tripResetTimestamp)
            }
            402 -> {
              val deserializedOpenCageResponse =
                  jsonMapper.decodeFromString<OpenCageResponse>(responseBody!!)
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
                  jsonMapper.decodeFromString<OpenCageResponse>(responseBody!!)
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
        is SocketTimeoutException -> Timber.e("Error reverse geocoding from opencage. Timeout")
        is UnknownHostException ->
            Timber.e("Error reverse geocoding from opencage. Unable to resolve host")
        else -> Timber.e(e, "Error reverse geocoding from opencage")
      }
      GeocodeResult.Fault.ExceptionError(e, tripResetTimestamp)
    }
  }

  companion object {
    private const val OPENCAGE_HOST = "api.opencagedata.com"
  }

  @Serializable
  internal class OpenCageResult {
    val formatted: String? = null
  }

  @Serializable
  internal class OpenCageResponse {
    val rate: Rate? = null

    val results: List<OpenCageResult>? = null
    val status: Status? = null
    val formatted: String?
      get() = if (!results.isNullOrEmpty()) results[0].formatted else null
  }

  @Serializable internal data class Rate(val limit: Int, val remaining: Int, val reset: Instant)

  @Serializable internal data class Status(val code: Int, val message: String)
}
