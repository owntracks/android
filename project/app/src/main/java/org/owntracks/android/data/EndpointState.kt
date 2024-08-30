package org.owntracks.android.data

import android.content.Context
import java.io.EOFException
import java.io.IOException
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import java.security.cert.CertPathValidatorException
import javax.net.ssl.SSLException
import javax.net.ssl.SSLHandshakeException
import javax.net.ssl.SSLProtocolException
import org.eclipse.paho.client.mqttv3.MqttException
import org.owntracks.android.R
import org.owntracks.android.net.mqtt.MqttConnectionConfiguration
import org.owntracks.android.support.interfaces.ConfigurationIncompleteException
import timber.log.Timber

enum class EndpointState {
  INITIAL,
  IDLE,
  CONNECTING,
  CONNECTED,
  DISCONNECTED,
  ERROR,
  ERROR_CONFIGURATION;

  var message: String? = null
  var error: Throwable? = null
    private set

  fun withMessage(message: String): EndpointState {
    this.message = message
    return this
  }

  fun getLabel(context: Context): String =
      when (this) {
        INITIAL -> context.resources.getString(R.string.INITIAL)
        IDLE -> context.resources.getString(R.string.IDLE)
        CONNECTING -> context.resources.getString(R.string.CONNECTING)
        CONNECTED -> context.resources.getString(R.string.CONNECTED)
        DISCONNECTED -> context.resources.getString(R.string.DISCONNECTED)
        ERROR -> context.resources.getString(R.string.ERROR)
        ERROR_CONFIGURATION -> context.resources.getString(R.string.ERROR_CONFIGURATION)
      }

  fun getErrorLabel(context: Context): String =
      when (val e = error) {
        // Configuration validator has failed
        is ConfigurationIncompleteException -> {
          when (e.cause) {
            // MQTT Host wasn't provided
            is MqttConnectionConfiguration.MissingHostException ->
                context.getString(R.string.statusEndpointStateMessageMissingHost)
            // URL isn't a URL
            is IllegalArgumentException ->
                context.getString(R.string.statusEndpointStateMessageMalformedHostPort)
            else -> e.toString()
          }
        }
        is SocketTimeoutException ->
            context.getString(R.string.statusEndpointStateMessageSocketTimeout)
        // Client cert errors show up like this
        is SSLProtocolException ->
            if (e.message != null && e.message!!.contains("TLSV1_ALERT_CERTIFICATE_REQUIRED")) {
              context.getString(R.string.statusEndpointStateMessageTLSEndpointClientCertsRequired)
            } else {
              context.getString(R.string.statusEndpointStateMessageTLSError, e.message)
            }
        is MqttException ->
            when (val mqttExceptionCause = e.cause) {
              // DNS fail
              is UnknownHostException ->
                  context.getString(R.string.statusEndpointStateMessageUnknownHost)
              // Timeout
              is SocketTimeoutException ->
                  context.getString(R.string.statusEndpointStateMessageSocketTimeout)
              is ConnectException ->
                  if (mqttExceptionCause.message?.contains("ECONNREFUSED") == true) {
                    context.getString(R.string.statusEndpointStateMessageConnectionRefused)
                  } else {
                    context.getString(R.string.statusEndpointStateMessageUnableToConnect)
                  }
              //  TLS on a plain-text endpoint shows up like this
              is SSLHandshakeException -> {
                if (mqttExceptionCause.cause?.cause is CertPathValidatorException) {
                  context.getString(R.string.statusEndpointStateMessageTLSEndpointCANotTrustedError)
                } else if (mqttExceptionCause.message != null &&
                    mqttExceptionCause.message!!.contains("connection closed")) {
                  context.getString(R.string.statusEndpointStateMessageTLSConnectionClosed)
                } else {
                  context.getString(R.string.statusEndpointStateMessageTLSError, e.message)
                }
              }
              // Client cert errors show up like this
              is SSLProtocolException ->
                  if (mqttExceptionCause.message != null &&
                      mqttExceptionCause.message!!.contains("TLSV1_ALERT_CERTIFICATE_REQUIRED")) {
                    context.getString(
                        R.string.statusEndpointStateMessageTLSEndpointClientCertsRequired)
                  } else {
                    context.getString(R.string.statusEndpointStateMessageTLSError, e.message)
                  }
              is SSLException -> {
                context.getString(R.string.statusEndpointStateMessageTLSError, e.message)
              }
              // Usually non-TLS against a TLS (or non MQTT) endpoint
              is EOFException -> context.getString(R.string.statusEndpointStateMessageEOFError)
              is IOException ->
                  if (mqttExceptionCause.message?.startsWith("WebSocket Response header") == true) {
                    context.getString(
                        R.string.statusEndpointStateMessageEndpointDoesNotSupportWebsockets)
                  } else {
                    mqttExceptionCause.message ?: mqttExceptionCause.toString()
                  }
              // General MQTT broker error cases
              else ->
                  when (e.reasonCode.toShort()) {
                    MqttException.REASON_CODE_INVALID_PROTOCOL_VERSION ->
                        context.getString(R.string.statusEndpointStateMessageInvalidProtocolVersion)
                    MqttException.REASON_CODE_INVALID_CLIENT_ID ->
                        context.getString(R.string.statusEndpointStateMessageInvalidClientId)
                    MqttException.REASON_CODE_FAILED_AUTHENTICATION ->
                        context.getString(R.string.statusEndpointStateMessageAuthenticationFailed)
                    MqttException.REASON_CODE_NOT_AUTHORIZED ->
                        context.getString(R.string.statusEndpointStateMessageNotAuthorized)
                    MqttException.REASON_CODE_SUBSCRIBE_FAILED ->
                        context.getString(R.string.statusEndpointStateMessageSubscribeFailed)
                    MqttException.REASON_CODE_CLIENT_TIMEOUT ->
                        context.getString(R.string.statusEndpointStateMessageClientTimeout)
                    MqttException.REASON_CODE_WRITE_TIMEOUT ->
                        context.getString(R.string.statusEndpointStateMessageServerTimeout)
                    MqttException.REASON_CODE_SERVER_CONNECT_ERROR ->
                        context.getString(R.string.statusEndpointStateMessageUnableToConnect)
                    MqttException.REASON_CODE_SSL_CONFIG_ERROR ->
                        context.getString(R.string.statusEndpointStateMessageTLSConfigError)
                    MqttException.REASON_CODE_CONNECTION_LOST ->
                        context.getString(
                            R.string.statusEndpointStateMessageConnectionLost,
                            mqttExceptionCause.toString())
                    else -> mqttExceptionCause.toString()
                  }
            }
        else -> e.toString()
      }.also { Timber.v(error, "Rendering error as $it") }

  fun withError(error: Throwable): EndpointState {
    this.error = error
    return this
  }

  override fun toString(): String {
    return if (message != null) {
      "${super.toString()} ($message)"
    } else {
      super.toString()
    }
  }
}
