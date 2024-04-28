package org.owntracks.android.support

import android.content.Context
import android.security.KeyChain
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.IOException
import java.net.InetAddress
import java.net.Socket
import java.security.KeyStore
import javax.net.ssl.KeyManagerFactory
import javax.net.ssl.SSLContext
import javax.net.ssl.SSLSocket
import javax.net.ssl.SSLSocketFactory
import javax.net.ssl.TrustManagerFactory
import timber.log.Timber

class SocketFactory(
    options: SocketFactoryOptions,
    caKeyStore: KeyStore,
    @ApplicationContext context: Context
) : SSLSocketFactory() {
  private val factory: SSLSocketFactory
  private val protocols = arrayOf("TLSv1.2", "TLSv1.3")

  private val socketTimeout: Int

  data class SocketFactoryOptions(
      var clientCertificateAlias: String = "",
      var socketTimeout: Int = 0
  )

  init {
    Timber.v("initializing CustomSocketFactory")
    val trustManagerFactory =
        TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm())
    val keyManagerFactory = KeyManagerFactory.getInstance("X509")
    socketTimeout = options.socketTimeout

    trustManagerFactory.init(caKeyStore)

    keyManagerFactory.init(null, null)
    if (options.clientCertificateAlias.isNotEmpty()) {
      val clientCertPrivateKey = KeyChain.getPrivateKey(context, options.clientCertificateAlias)
      if (clientCertPrivateKey != null) {
        val clientCert = KeyChain.getCertificateChain(context, options.clientCertificateAlias)
        KeyStore.getInstance("PKCS12", "BC")
            .apply {
              load(null, null)
              setKeyEntry(
                  options.clientCertificateAlias,
                  clientCertPrivateKey,
                  "".toCharArray(),
                  clientCert)
            }
            .let { keyManagerFactory.init(it, null) }
      }
    }

    // Create an SSLContext that uses our TrustManager
    factory =
        SSLContext.getInstance("TLS")
            .apply { init(keyManagerFactory.keyManagers, trustManagerFactory.trustManagers, null) }
            .socketFactory
  }

  override fun getDefaultCipherSuites(): Array<String> = factory.defaultCipherSuites

  override fun getSupportedCipherSuites(): Array<String> = factory.supportedCipherSuites

  @Throws(IOException::class)
  override fun createSocket(): Socket =
      (factory.createSocket() as SSLSocket).apply {
        enabledProtocols = protocols
        soTimeout = socketTimeout
      }

  @Throws(IOException::class)
  override fun createSocket(s: Socket, host: String, port: Int, autoClose: Boolean): Socket =
      (factory.createSocket(s, host, port, autoClose) as SSLSocket).apply {
        enabledProtocols = protocols
        soTimeout = socketTimeout
      }

  @Throws(IOException::class)
  override fun createSocket(host: String, port: Int): Socket =
      (factory.createSocket(host, port) as SSLSocket).apply {
        enabledProtocols = protocols
        soTimeout = socketTimeout
      }

  @Throws(IOException::class)
  override fun createSocket(
      host: String,
      port: Int,
      localHost: InetAddress,
      localPort: Int
  ): Socket =
      (factory.createSocket(host, port, localHost, localPort) as SSLSocket).apply {
        enabledProtocols = protocols
        soTimeout = socketTimeout
      }

  @Throws(IOException::class)
  override fun createSocket(host: InetAddress, port: Int): Socket =
      (factory.createSocket(host, port) as SSLSocket).apply {
        enabledProtocols = protocols
        soTimeout = socketTimeout
      }

  @Throws(IOException::class)
  override fun createSocket(
      address: InetAddress,
      port: Int,
      localAddress: InetAddress,
      localPort: Int
  ): Socket =
      (factory.createSocket(address, port, localAddress, localPort) as SSLSocket).apply {
        enabledProtocols = protocols
        soTimeout = socketTimeout
      }
}
