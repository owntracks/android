package org.owntracks.android.net.http

import android.content.Context
import com.fasterxml.jackson.core.JsonProcessingException
import java.io.ByteArrayInputStream
import java.io.IOException
import java.security.KeyStore
import java.util.Locale
import java.util.concurrent.TimeUnit
import javax.net.ssl.HostnameVerifier
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import okhttp3.Credentials.basic
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.conscrypt.Conscrypt
import org.owntracks.android.BuildConfig
import org.owntracks.android.data.EndpointState
import org.owntracks.android.data.repos.EndpointStateRepo
import org.owntracks.android.di.ApplicationScope
import org.owntracks.android.di.CoroutineScopes
import org.owntracks.android.model.Parser
import org.owntracks.android.model.messages.MessageBase
import org.owntracks.android.model.messages.MessageCard
import org.owntracks.android.model.messages.MessageLocation
import org.owntracks.android.net.CALeafCertMatchingHostnameVerifier
import org.owntracks.android.net.MessageProcessorEndpoint
import org.owntracks.android.net.OutgoingMessageSendingException
import org.owntracks.android.preferences.Preferences
import org.owntracks.android.preferences.types.ConnectionMode
import org.owntracks.android.services.MessageProcessor
import org.owntracks.android.support.SocketFactory
import org.owntracks.android.support.interfaces.ConfigurationIncompleteException
import timber.log.Timber

class HttpMessageProcessorEndpoint(
    messageProcessor: MessageProcessor,
    private val parser: Parser,
    private val preferences: Preferences,
    private val applicationContext: Context,
    private val endpointStateRepo: EndpointStateRepo,
    private val caKeyStore: KeyStore,
    @ApplicationScope private val scope: CoroutineScope,
    @CoroutineScopes.IoDispatcher private val ioDispatcher: CoroutineDispatcher
) : MessageProcessorEndpoint(messageProcessor), Preferences.OnPreferenceChangeListener {
  override val modeId: ConnectionMode = ConnectionMode.HTTP
  private var httpClientAndConfiguration: HttpClientAndConfiguration? = null

  override fun activate() {
    Timber.v("HTTP Activate")
    preferences.registerOnPreferenceChangedListener(this)
    scope.launch {
      try {
        httpClientAndConfiguration = setClientAndConfiguration(applicationContext, preferences)
      } catch (e: ConfigurationIncompleteException) {
        Timber.w(e)
        endpointStateRepo.setState(EndpointState.ERROR_CONFIGURATION.withError(e))
      }
    }
  }

  private fun createHttpClient(
      socketFactory: SocketFactory,
      hostnameVerifier: HostnameVerifier?
  ): OkHttpClient =
      OkHttpClient.Builder()
          .followRedirects(true)
          .sslSocketFactory(socketFactory, Conscrypt.getDefaultX509TrustManager())
          .followSslRedirects(true)
          .connectTimeout(preferences.connectionTimeoutSeconds.toLong(), TimeUnit.SECONDS)
          .retryOnConnectionFailure(false)
          .protocols(listOf(Protocol.HTTP_1_1))
          .cache(null)
          .apply {
            if (hostnameVerifier != null) {
              this.hostnameVerifier(hostnameVerifier)
            }
          }
          .build()

  fun getRequest(configuration: HttpConfiguration, message: MessageBase): Request =
      Request.Builder()
          .url(configuration.url)
          .header(HEADER_USERAGENT, USERAGENT)
          .method(
              "POST",
              message
                  .toJson(parser)
                  .also { Timber.d("Publishing Message JSON $it") }
                  ?.toRequestBody("application/json; charset=utf-8".toMediaTypeOrNull()))
          .apply {
            val url = configuration.url.toHttpUrl()
            val username = url.username.ifEmpty { configuration.username }
            val password = url.password.ifEmpty { configuration.password }
            if (username.isNotEmpty() && password.isNotEmpty()) {
              header(HEADER_AUTHORIZATION, basic(username, password))
            }
            if (username.isNotEmpty()) {
              header(HEADER_USERNAME, username)
            }
            if (configuration.deviceId.isNotEmpty()) {
              header(HEADER_DEVICE, configuration.deviceId)
            }
          }
          .build()

  override suspend fun sendMessage(message: MessageBase) {
    message.annotateFromPreferences(preferences)
    // HTTP messages carry the topic field in the body of the message, rather than MQTT which
    // simply publishes the message to that topic.
    message.setTopicVisible()
    if (httpClientAndConfiguration == null) {
      throw OutgoingMessageSendingException(Exception("Http client not yet initialized"))
    }

    httpClientAndConfiguration?.run {
      endpointStateRepo.setState(EndpointState.CONNECTING)
      try {
        client.newCall(getRequest(configuration, message)).execute().use { response ->
          Timber.d("HTTP response received: $response")
          if (!response.isSuccessful) {
            val httpException = Exception("HTTP request failed. Status: ${response.code}")
            Timber.e("HTTP request failed. Status: ${response.code}")
            endpointStateRepo.setState(
                EndpointState.ERROR.withMessage(
                    String.format(Locale.ROOT, "HTTP code %d", response.code)))
            messageProcessor.onMessageDeliveryFailed(message)
            throw OutgoingMessageSendingException(httpException)
          } else {
            if (response.body != null) {
              try {
                val responseString = response.body!!.string()
                Timber.d("HTTP response body: ${responseString.take(1000)}")
                val responseStream = ByteArrayInputStream(responseString.toByteArray())
                val result = parser.fromJson(responseStream)
                // TODO apply i18n here
                scope.launch {
                  endpointStateRepo.setState(
                      EndpointState.IDLE.withMessage(
                          String.format(
                              Locale.ROOT,
                              "Response %d, (%d msgs received)",
                              response.code,
                              result.size)))
                  result.forEach { onMessageReceived(it) }
                }
              } catch (e: JsonProcessingException) {
                Timber.e("JsonParseException HTTP status: %s", response.code)
                endpointStateRepo.setState(
                    EndpointState.IDLE.withMessage(
                        String.format(
                            Locale.ROOT, "HTTP status %d, JsonParseException", response.code)))
              } catch (e: Parser.EncryptionException) {
                Timber.e("JsonParseException HTTP status: %s", response.code)
                endpointStateRepo.setState(
                    EndpointState.ERROR.withMessage(
                        String.format(
                            Locale.ROOT, "HTTP status: %d, EncryptionException", response.code)))
              } catch (e: IOException) {
                Timber.e(e, "HTTP Delivery failed")
                endpointStateRepo.setState(EndpointState.ERROR.withError(e))
                messageProcessor.onMessageDeliveryFailed(message)
                throw OutgoingMessageSendingException(e)
              }
            }
          }
          messageProcessor.onMessageDelivered()
        }
      } catch (e: Exception) {
        Timber.d(e, "Execute call failed")
        // Sometimes we get an exception just on the execute() call
        throw OutgoingMessageSendingException(e)
      }
    }
  }

  override fun deactivate() {
    preferences.unregisterOnPreferenceChangedListener(this)
  }

  override fun onPreferenceChanged(properties: Set<String>) {
    if (preferences.mode != ConnectionMode.HTTP) {
      return
    }
    Timber.v("HTTP preferences changed: [${properties.joinToString(",")}]")
    /* In HTTP mode, the *only* preference we care about wanting to trigger an immediate reprocessing
     * of the outgoing message queue is the password. The other properties that might change the
     * likelihood of message sends succeeding (e.g. URL, username etc.) will actually trigger a
     * queue wipe and full reset, and that's handled in the [MessageProcessor].
     */
    val propertiesWeCareAbout = setOf(Preferences::password.name)

    if (propertiesWeCareAbout.intersect(properties).isNotEmpty()) {
      scope.launch(ioDispatcher) {
        try {
          httpClientAndConfiguration = setClientAndConfiguration(applicationContext, preferences)
        } catch (e: ConfigurationIncompleteException) {
          Timber.d(
              e, "Configuration is incomplete, not doing anything with this preference change yet")
        }
        messageProcessor.notifyOutgoingMessageQueue()
      }
    }
  }

  private fun setClientAndConfiguration(
      context: Context,
      preferences: Preferences
  ): HttpClientAndConfiguration {
    Timber.v("Creating new HTTP client and configuration")
    val httpConfiguration = getEndpointConfiguration()

    val hostnameVerifier = CALeafCertMatchingHostnameVerifier()
    val socketFactory =
        httpConfiguration.getSocketFactory(
            preferences.connectionTimeoutSeconds,
            preferences.tls,
            preferences.tlsClientCrt,
            context,
            caKeyStore)

    return HttpClientAndConfiguration(
        createHttpClient(socketFactory, hostnameVerifier), httpConfiguration)
  }

  override fun getEndpointConfiguration(): HttpConfiguration {
    val configuration =
        HttpConfiguration(
            preferences.url, preferences.username, preferences.password, preferences.deviceId)
    configuration.validate()
    return configuration
  }

  override fun onFinalizeMessage(message: MessageBase): MessageBase {
    // Build pseudo topic based on tid
    when (message) {
      is MessageLocation -> {
        message.topic = HTTPTOPIC + message.trackerId
      }
      is MessageCard -> {
        message.topic = HTTPTOPIC + message.trackerId
      }
      else -> {
        message.topic = "NOKEY"
      }
    }
    return message
  }

  data class HttpClientAndConfiguration(
      val client: OkHttpClient,
      val configuration: HttpConfiguration
  )

  companion object {
    // Headers according to https://github.com/owntracks/recorder#http-mode
    const val HEADER_AUTHORIZATION = "Authorization"
    const val HEADER_USERNAME = "X-Limit-U"
    const val HEADER_DEVICE = "X-Limit-D"
    const val HEADER_USERAGENT = "User-Agent"
    const val USERAGENT = "Owntracks-Android/${BuildConfig.FLAVOR}/${BuildConfig.VERSION_CODE}"
    private const val HTTPTOPIC = "owntracks/http/"
  }
}
