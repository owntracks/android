package org.owntracks.android.services

import android.content.Context
import java.io.FileNotFoundException
import java.net.URI
import java.net.URISyntaxException
import java.security.cert.CertificateFactory
import java.security.cert.X509Certificate
import org.eclipse.paho.client.mqttv3.MqttConnectOptions
import org.json.JSONObject
import org.owntracks.android.preferences.DefaultsProvider
import org.owntracks.android.preferences.Preferences
import org.owntracks.android.preferences.types.MqttProtocolLevel
import org.owntracks.android.preferences.types.MqttQos
import org.owntracks.android.support.SocketFactory
import org.owntracks.android.support.SocketFactory.SocketFactoryOptions
import org.owntracks.android.support.interfaces.ConfigurationIncompleteException
import timber.log.Timber

interface ConnectionConfiguration
data class MqttConnectionConfiguration constructor(
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
    val tlsCaCrt: String,
    val tlsClientCrt: String,
    val tlsClientCrtPassword: String,
    val willTopic: String,
    val topicsToSubscribeTo: Set<String>,
    val subQos: MqttQos
) : ConnectionConfiguration {
    private val scheme = if (ws) {
        if (tls) "wss" else "ws"
    } else {
        if (tls) "ssl" else "tcp"
    }

    @kotlin.jvm.Throws(ConfigurationIncompleteException::class)
    fun validate() {
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

    fun getConnectOptions(context: Context): MqttConnectOptions =
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
            setSocketFactory(context)
        }

    private fun MqttConnectOptions.setSocketFactory(context: Context) {
        if (tls) {
            val socketFactoryOptions = SocketFactoryOptions().withSocketTimeout(connectionTimeout)
            if (tlsCaCrt.isNotEmpty()) {
                try {
                    context.openFileInput(tlsCaCrt)
                        .use {
                            socketFactoryOptions.withCaCertificate(it.readBytes())
                        }

                    /* The default for paho is to validate hostnames as per the HTTPS spec. However, this causes
                    a bit of a breakage for some users using self-signed certificates, where the verification of
                    the hostname is unnecessary under certain circumstances. Specifically when the fingerprint of
                    the server leaf certificate is the same as the certificate supplied as the CA (as would be the
                    case using self-signed certs.

                    So we turn off HTTPS behaviour and supply our own hostnameverifier that knows about the self-signed
                    case.
                     */
                    isHttpsHostnameVerificationEnabled = false
                    context.openFileInput(tlsCaCrt)
                        .use { caFileInputStream ->
                            val ca = CertificateFactory.getInstance("X.509", "BC")
                                .generateCertificate(caFileInputStream) as X509Certificate
                            sslHostnameVerifier = MqttHostnameVerifier(ca)
                        }
                } catch (e: FileNotFoundException) {
                    Timber.e(e)
                }
            }
            if (tlsClientCrt.isNotEmpty()) {
                context.openFileInput(tlsClientCrt)
                    .use {
                        socketFactoryOptions.withClientP12Certificate(it.readBytes())
                            .withClientP12Password(tlsClientCrtPassword)
                    }
            }
            socketFactory = SocketFactory(socketFactoryOptions)
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
        tlsCaCrt,
        tlsClientCrt,
        tlsClientCrtPassword,
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
                    subTopic + waypointsTopicSuffix
                )
            } else {
                sortedSetOf(subTopic, subTopic + eventTopicSuffix, subTopic + waypointsTopicSuffix)
            }
        } else {
            sortedSetOf(subTopic)
        },
        subQos
    )
