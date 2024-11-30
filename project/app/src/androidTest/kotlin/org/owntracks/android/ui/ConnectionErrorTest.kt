package org.owntracks.android.ui

import android.content.Intent
import android.net.Uri
import androidx.test.espresso.Espresso
import androidx.test.filters.MediumTest
import androidx.test.platform.app.InstrumentationRegistry
import com.adevinta.android.barista.assertion.BaristaVisibilityAssertions.assertContains
import com.adevinta.android.barista.interaction.BaristaClickInteractions.clickOn
import com.fasterxml.jackson.databind.ObjectMapper
import dagger.hilt.android.testing.HiltAndroidTest
import java.net.ConnectException
import java.net.InetSocketAddress
import java.net.Socket
import kotlin.concurrent.thread
import kotlin.random.Random
import mqtt.broker.Broker
import mqtt.broker.interfaces.Authentication
import org.eclipse.paho.client.mqttv3.internal.websocket.Base64
import org.junit.Test
import org.owntracks.android.R
import org.owntracks.android.preferences.Preferences
import org.owntracks.android.preferences.types.ConnectionMode
import org.owntracks.android.testutils.TestWithAnActivity
import org.owntracks.android.testutils.use
import org.owntracks.android.ui.preferences.load.LoadActivity
import org.owntracks.android.ui.status.StatusActivity
import socket.tls.TLSSettings
import timber.log.Timber

@MediumTest
@HiltAndroidTest
class ConnectionErrorTest : TestWithAnActivity<StatusActivity>(startActivity = true) {

  @Test
  fun given_a_config_with_http_mode_and_invalid_url_when_viewing_the_connecting_status_then_a_config_incomplete_message_is_shown() {
    val username = "user"
    val password = "password"
    val port = Random.nextInt(10000, 20000)

    val config =
        encodeConfig(
            getConfig(port, username, password).apply {
              this[Preferences::mode.name] = ConnectionMode.HTTP.value
              this[Preferences::url.name] = "not a url"
              remove(Preferences::host.name)
              remove(Preferences::port.name)
            })
    setupActivity(config)
    assertContains(
        R.id.connectedStatusMessage, R.string.statusEndpointStateMessageMalformedHostPort)
  }

  @Test
  fun given_a_config_with_no_host_when_viewing_the_connecting_status_then_a_config_incomplete_message_is_shown() {
    val username = "user"
    val password = "password"
    val port = Random.nextInt(10000, 20000)
    getBroker(port, username, password).use {
      val config =
          encodeConfig(
              getConfig(port, username, password).apply { this.remove(Preferences::host.name) })
      setupActivity(config)
      assertContains(R.id.connectedStatusMessage, R.string.statusEndpointStateMessageMissingHost)
    }
  }

  @Test
  fun given_a_config_with_the_wrong_host_when_viewing_the_connecting_status_then_a_DNS_fail_message_is_shown() {
    val username = "user"
    val password = "password"
    val port = Random.nextInt(10000, 20000)
    getBroker(port, username, password).use {
      val config =
          encodeConfig(
              getConfig(port, username, password).apply {
                this[Preferences::host.name] = "unknown"
              })
      setupActivity(config)
      mqttConnectionIdlingResource.use { Espresso.onIdle() }
      assertContains(R.id.connectedStatusMessage, R.string.statusEndpointStateMessageUnknownHost)
    }
  }

  @Test
  fun given_a_config_with_the_wrong_port_when_viewing_the_connecting_status_then_a_refused_message_is_shown() {
    val username = "user"
    val password = "password"
    val port = Random.nextInt(10000, 20000)
    getBroker(port, username, password).use {
      val config =
          encodeConfig(
              getConfig(port, username, password).apply { this[Preferences::port.name] = 1234 })
      setupActivity(config)
      mqttConnectionIdlingResource.use { Espresso.onIdle() }
      assertContains(
          R.id.connectedStatusMessage, R.string.statusEndpointStateMessageConnectionRefused)
    }
  }

  @Test
  fun given_a_config_with_tls_disabled_against_a_tls_endpoint_when_viewing_the_connecting_status_then_an_eoferror_is_shown() {
    val username = "user"
    val password = "password"
    val port = Random.nextInt(10000, 20000)
    val tlsSettings = getTLSSettings(this)
    getBroker(port, username, password, tlsSettings).use {
      val config = encodeConfig(getConfig(port, username, password))
      setupActivity(config)
      mqttConnectionIdlingResource.use { Espresso.onIdle() }
      assertContains(R.id.connectedStatusMessage, R.string.statusEndpointStateMessageEOFError)
    }
  }

  @Test
  fun given_a_config_against_a_tls_endpoint_with_self_signed_certs_when_viewing_the_connecting_status_then_a_ca_not_trusted_error_is_shown() {
    val username = "user"
    val password = "password"
    val port = Random(1).nextInt(10000, 20000)
    val tlsSettings = getTLSSettings(this)
    getBroker(port, username, password, tlsSettings).use {
      val config =
          encodeConfig(
              getConfig(port, username, password).apply { this[Preferences::tls.name] = true })
      setupActivity(config)
      mqttConnectionIdlingResource.use { Espresso.onIdle() }
      assertContains(
          R.id.connectedStatusMessage,
          R.string.statusEndpointStateMessageTLSEndpointCANotTrustedError)
    }
  }

  @Test
  fun given_a_config_with_websockets_enabled_against_a_tls_endpoint_when_viewing_the_connecting_status_then_an_eoferror_is_showna() {
    val username = "user"
    val password = "password"
    val port = Random.nextInt(10000, 20000)
    getBroker(port, username, password).use {
      val config =
          encodeConfig(
              getConfig(port, username, password).apply { this[Preferences::ws.name] = true })
      setupActivity(config)
      mqttConnectionIdlingResource.use { Espresso.onIdle() }
      assertContains(
          R.id.connectedStatusMessage,
          R.string.statusEndpointStateMessageEndpointDoesNotSupportWebsockets)
    }
  }

  private fun setupActivity(config: String) {
    InstrumentationRegistry.getInstrumentation()
        .targetContext
        .startActivity(
            Intent(Intent.ACTION_VIEW).apply {
              data = Uri.parse("owntracks:///config?inline=$config")
              flags = Intent.FLAG_ACTIVITY_NEW_TASK
            })
    waitUntilActivityVisible(LoadActivity::class.java)
    saveConfigurationIdlingResource.use { clickOn(R.id.save) }
    waitUntilActivityVisible()
  }
}

@OptIn(ExperimentalUnsignedTypes::class)
@Suppress("PARAMETER_NAME_CHANGED_ON_OVERRIDE", "SameParameterValue")
private fun getBroker(
    mqttPort: Int,
    username: String,
    password: String,
    tlsSettings: TLSSettings? = null
) =
    Broker(
        host = "127.0.0.1",
        port = mqttPort,
        tlsSettings = tlsSettings,
        authentication =
            object : Authentication {
              override fun authenticate(
                  clientId: String,
                  givenUsername: String?,
                  givenPassword: UByteArray?
              ): Boolean {
                return givenUsername == username &&
                    givenPassword.contentEquals(password.toByteArray().toUByteArray())
              }
            })

private fun encodeConfig(config: Map<String, Any>): String =
    Base64.encode(ObjectMapper().writeValueAsString(config))

private fun getTLSSettings(connectionErrorTest: ConnectionErrorTest): TLSSettings {
  val dataBytes = connectionErrorTest.javaClass.getResource("/rootCA.p12")!!.readBytes()
  InstrumentationRegistry.getInstrumentation().targetContext.filesDir.run {
    mkdirs()
    resolve("rootCA.p12").run { outputStream().use { it.write(dataBytes) } }
  }
  val keyStorePath =
      InstrumentationRegistry.getInstrumentation()
          .targetContext
          .filesDir
          .resolve("rootCA.p12")
          .absolutePath
  return TLSSettings(
      keyStoreFilePath = keyStorePath, keyStorePassword = "aaaa", requireClientCertificate = true)
}

@Suppress("SameParameterValue")
private fun getConfig(mqttPort: Int, username: String, password: String): MutableMap<String, Any> =
    mutableMapOf(
        "_type" to "configuration",
        "host" to "localhost",
        "password" to password,
        "port" to mqttPort,
        "mqttProtocolLevel" to 4,
        "username" to username,
        "tls" to false,
        "keepalive" to 5,
        "connectionTimeoutSeconds" to 1,
        "reverseGeocodeProvider" to "None")

private fun Broker.use(block: () -> Unit) {
  var shouldBeRunning = true
  val brokerThread = thread {
    while (shouldBeRunning) {
      Timber.i("Calling MQTT Broker listen")
      listen()
      Timber.i("MQTT Broker no longer listening")
    }
    Timber.i("MQTT Broker Thread ending")
  }
  var listening = true
  while (!listening) {
    Socket().use {
      try {
        it.apply { connect(InetSocketAddress("localhost", this.port)) }
        listening = true
        Timber.i("Test MQTT Broker listening on port ${this.port}")
      } catch (e: ConnectException) {
        Timber.i("broker not listening on ${this.port} yet")
        listening = false
        Thread.sleep(5000)
      }
    }
  }
  block()
  shouldBeRunning = false
  stop()
  Timber.i("Waiting to join thread")
  brokerThread.join()
  Timber.i("MQTT Broker stopped")
}
