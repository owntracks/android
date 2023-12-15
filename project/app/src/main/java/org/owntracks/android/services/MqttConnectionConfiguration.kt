package org.owntracks.android.services

import android.content.Context
import java.net.URI
import java.net.URISyntaxException
import java.security.KeyStore
import org.eclipse.paho.client.mqttv3.MqttConnectOptions
import org.json.JSONObject
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
    val keepAlive: Int,
    val timeout: Int,
    val cleanSession: Boolean,
    val mqttProtocolLevel: MqttProtocolLevel,
    val tlsClentCertAlias: String,
    val willTopic: String,
    val topicsToSubscribeTo: Set<String>,
    val subQos: MqttQos,
    val maxInFlight: Int = 500
) : ConnectionConfiguration {
    private val scheme = if (ws) {
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
            connectionString.run {
                Timber.v("MQTT Connection String validated as $this")
            }
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
            keepAliveInterval = keepAlive
            connectionTimeout = timeout
            isCleanSession = cleanSession
            setWill(
                willTopic,
                JSONObject().apply { put("_type", "lwt") }
                    .toString()
                    .toByteArray(),
                0,
                false
            )
            maxInflight = maxInFlight
            if (tls) {
                socketFactory = getSocketFactory(
                    timeout,
                    true,
                    tlsClentCertAlias,
                    context,
                    caKeyStore
                )

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
        keepalive,
        connectionTimeoutSeconds,
        cleanSession,
        mqttProtocolLevel,
        tlsClientCrt,
        pubTopicBaseWithUserDetails,
        if (subTopic.contains(" ")) {
            subTopic.split(" ")
                .toSortedSet()
        } else if (subTopic == DefaultsProvider.DEFAULT_SUB_TOPIC) {
            if (info) {
                sortedSetOf(
                    subTopic,
                    subTopic + infoTopicSuffix,
                    subTopic + eventTopicSuffix,
                    subTopic + waypointsTopicSuffix,
                    subTopic + commandTopicSuffix
                )
            } else {
                sortedSetOf(
                    subTopic,
                    subTopic + eventTopicSuffix,
                    subTopic + waypointsTopicSuffix,
                    subTopic + commandTopicSuffix
                )
            }
        } else {
            sortedSetOf(subTopic)
        },
        subQos
    )
