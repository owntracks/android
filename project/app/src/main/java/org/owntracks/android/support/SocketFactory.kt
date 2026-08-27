package org.owntracks.android.support

import android.content.Context
import android.security.KeyChain
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.IOException
import java.net.InetAddress
import java.net.Socket
import java.security.KeyStore
import java.security.Principal
import java.security.PrivateKey
import java.security.cert.X509Certificate
import javax.net.ssl.KeyManager
import javax.net.ssl.SSLContext
import javax.net.ssl.SSLEngine
import javax.net.ssl.SSLSocket
import javax.net.ssl.SSLSocketFactory
import javax.net.ssl.TrustManagerFactory
import javax.net.ssl.X509ExtendedKeyManager
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

  // This needs to be init off the main thread, as KeyChain operations are blocking
  init {
    Timber.v("initializing CustomSocketFactory")
    val trustManagerFactory =
        TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm())
    socketTimeout = options.socketTimeout

    trustManagerFactory.init(caKeyStore)

    /*
    Built directly from the KeyChain alias's key material rather than via a KeyStore and
    KeyManagerFactory: Android's own vendored BouncyCastle has a history of failing to load a
    PKCS12 KeyStore constructed this way on certain devices (see issue #1225 — a MAC-key type its
    own installed provider then couldn't handle). Skipping the KeyStore/PKCS12 step avoids that
    failure mode entirely rather than depending on a differently-behaved provider to get it right,
    and does not need BouncyCastle as a dependency at all.
     */
    val keyManagers: Array<KeyManager>? =
        options.clientCertificateAlias
            .takeIf { it.isNotEmpty() }
            ?.let { alias ->
              val privateKey = KeyChain.getPrivateKey(context, alias)
              val certificateChain = KeyChain.getCertificateChain(context, alias)
              if (privateKey != null && certificateChain != null) {
                arrayOf(ClientCertificateKeyManager(alias, privateKey, certificateChain))
              } else {
                null
              }
            }

    // Create an SSLContext that uses our TrustManager
    factory =
        SSLContext.getInstance("TLS")
            .apply { init(keyManagers, trustManagerFactory.trustManagers, null) }
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

/**
 * Presents a single, fixed client identity — an Android [KeyChain] alias's private key and
 * certificate chain — to a TLS handshake, without going via a [KeyStore][java.security.KeyStore].
 */
private class ClientCertificateKeyManager(
    private val alias: String,
    private val privateKey: PrivateKey,
    private val certificateChain: Array<X509Certificate>
) : X509ExtendedKeyManager() {
  override fun getPrivateKey(alias: String?): PrivateKey = privateKey

  override fun getCertificateChain(alias: String?): Array<X509Certificate> = certificateChain

  override fun getClientAliases(keyType: String?, issuers: Array<out Principal>?): Array<String> =
      arrayOf(alias)

  override fun chooseClientAlias(
      keyType: Array<out String>?,
      issuers: Array<out Principal>?,
      socket: Socket?
  ): String = alias

  override fun chooseEngineClientAlias(
      keyType: Array<out String>?,
      issuers: Array<out Principal>?,
      engine: SSLEngine?
  ): String = alias

  // This KeyManager only ever presents a client identity, never a server one.
  override fun getServerAliases(keyType: String?, issuers: Array<out Principal>?): Array<String>? =
      null

  override fun chooseServerAlias(
      keyType: String?,
      issuers: Array<out Principal>?,
      socket: Socket?
  ): String? = null
}
