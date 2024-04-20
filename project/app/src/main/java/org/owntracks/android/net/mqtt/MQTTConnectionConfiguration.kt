package org.owntracks.android.net.mqtt

import android.content.Context
import java.net.URI
import java.net.URISyntaxException
import java.security.KeyStore
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds
import org.eclipse.paho.client.mqttv3.MqttConnectOptions
import org.json.JSONObject
import org.owntracks.android.net.CALeafCertMatchingHostnameVerifier
import org.owntracks.android.net.ConnectionConfiguration
import org.owntracks.android.preferences.DefaultsProvider
import org.owntracks.android.preferences.Preferences
import org.owntracks.android.preferences.types.MqttProtocolLevel
import org.owntracks.android.preferences.types.MqttQos
import org.owntracks.android.support.interfaces.ConfigurationIncompleteException
import timber.log.Timber

data class MqttConnectionConfiguration(
    val tls: Boolean,
    val ws: Boolean,
    val host: String,
    val port: Int,
    val clientId: String,
    val username: String,
    val password: String,
    val keepAlive: Duration,
    val timeout: Duration,
    val cleanSession: Boolean,
    val mqttProtocolLevel: MqttProtocolLevel,
    val tlsClientCertAlias: String,
    val willTopic: String,
    val topicsToSubscribeTo: Set<String>,
    val subQos: MqttQos,
    val maxInFlight: Int = 500
) : ConnectionConfiguration {
  private val scheme =
      if (ws) {
        if (tls) "wss" else "ws"
      } else {
        if (tls) "ssl" else "tcp"
      }

  @kotlin.jvm.Throws(ConfigurationIncompleteException::class)
  override fun validate() {
    try {
      if (host.isBlank()) {
        throw ConfigurationIncompleteException(MissingHostException())
      }
      connectionString.run { Timber.v("MQTT Connection String validated as $this") }
    } catch (e: URISyntaxException) {
      throw ConfigurationIncompleteException(e)
    }
  }

  val connectionString: String
    @Throws(URISyntaxException::class)
    get() {
      return URI(scheme, null, host, port, "", "", "").toString()
    }

  fun getConnectOptions(context: Context, caKeyStore: KeyStore): MqttConnectOptions =
      MqttConnectOptions().apply {
        userName = username
        password = this@MqttConnectionConfiguration.password.toCharArray()
        mqttVersion = mqttProtocolLevel.value
        isAutomaticReconnect = false
        keepAliveInterval = keepAlive.inWholeSeconds.coerceAtLeast(0).toInt()
        connectionTimeout = timeout.inWholeSeconds.coerceAtLeast(1).toInt()
        isCleanSession = cleanSession
        setWill(
            willTopic,
            JSONObject().apply { put("_type", "lwt") }.toString().toByteArray(),
            0,
            false)
        maxInflight = maxInFlight
        if (tls) {
          socketFactory =
              getSocketFactory(
                  timeout.inWholeSeconds.toInt(), true, tlsClientCertAlias, context, caKeyStore)

          /* The default for paho is to validate hostnames as per the HTTPS spec. However, this causes
          a bit of a breakage for some users using self-signed certificates, where the verification of
          the hostname is unnecessary under certain circumstances. Specifically when the fingerprint of
          the server leaf certificate is the same as the certificate supplied as the CA (as would be the
          case using self-signed certs.

          So we turn off HTTPS behaviour and supply our own hostnameverifier that knows about the self-signed
          case.
           */
          isHttpsHostnameVerificationEnabled = false
          sslHostnameVerifier = CALeafCertMatchingHostnameVerifier()
        }
      }

  class MissingHostException : Exception()
}

fun Preferences.toMqttConnectionConfiguration(): MqttConnectionConfiguration =
    MqttConnectionConfiguration(
        tls,
        ws,
        host,
        port,
        clientId,
        username,
        password,
        keepalive.seconds,
        connectionTimeoutSeconds.seconds,
        cleanSession,
        mqttProtocolLevel,
        tlsClientCrt,
        pubTopicBaseWithUserDetails,
        if (subTopic.contains(" ")) {
          subTopic.split(" ").toSortedSet()
        } else if (subTopic == DefaultsProvider.DEFAULT_SUB_TOPIC) {
          if (info) {
            sortedSetOf(
                subTopic,
                subTopic + infoTopicSuffix,
                subTopic + eventTopicSuffix,
                subTopic + statusTopicSuffix,
                receivedCommandsTopic)
          } else {
            sortedSetOf(subTopic, subTopic + eventTopicSuffix, receivedCommandsTopic)
          }
        } else {
          sortedSetOf(subTopic)
        },
        subQos)
