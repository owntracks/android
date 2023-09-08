package org.owntracks.android.support

import java.io.ByteArrayInputStream
import java.io.IOException
import java.net.InetAddress
import java.net.Socket
import java.security.KeyStore
import java.security.Security
import javax.net.ssl.KeyManagerFactory
import javax.net.ssl.SSLContext
import javax.net.ssl.SSLSocket
import javax.net.ssl.SSLSocketFactory
import javax.net.ssl.TrustManagerFactory
import timber.log.Timber

class SocketFactory(options: SocketFactoryOptions) : SSLSocketFactory() {
    private val factory: SSLSocketFactory
    private val protocols = arrayOf("TLSv1", "TLSv1.1", "TLSv1.2", "TLSv1.3")

    private val socketTimeout: Int

    data class SocketFactoryOptions(
        var clientP12Certificate: ByteArray? = null,

        var caClientP12Password: String? = null,
        var socketTimeout: Int = 0
    ) {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false

            other as SocketFactoryOptions

            if (clientP12Certificate != null) {
                if (other.clientP12Certificate == null) return false
                if (!clientP12Certificate.contentEquals(other.clientP12Certificate)) return false
            } else if (other.clientP12Certificate != null) return false
            if (caClientP12Password != other.caClientP12Password) return false
            if (socketTimeout != other.socketTimeout) return false

            return true
        }

        override fun hashCode(): Int {
            var result = (clientP12Certificate?.contentHashCode() ?: 0)
            result = 31 * result + (caClientP12Password?.hashCode() ?: 0)
            result = 31 * result + socketTimeout
            return result
        }
    }

    init {
        Timber.v("initializing CustomSocketFactory")
        val tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm())
        val kmf = KeyManagerFactory.getInstance("X509")
        socketTimeout = options.socketTimeout

        val keyStore = KeyStore.getInstance("AndroidCAStore")
        keyStore.load(null)
        tmf.init(keyStore)

        if (options.clientP12Certificate != null) {
            Timber.v("options.hasClientP12Crt(): true")
            val clientKeyStore = KeyStore.getInstance("PKCS12", Security.getProvider("BC"))
            val password = if (options.caClientP12Password != null) {
                options.caClientP12Password!!.toCharArray()
            } else {
                CharArray(0)
            }
            clientKeyStore.load(ByteArrayInputStream(options.clientP12Certificate), password)
            kmf.init(clientKeyStore, password)
            Timber.v("Client .p12 Keystore content: ")
            val aliasesClientCert = clientKeyStore.aliases()
            while (aliasesClientCert.hasMoreElements()) {
                val o = aliasesClientCert.nextElement()
                Timber.v("Alias: $o")
            }
        } else {
            kmf.init(null, null)
        }

        // Create an SSLContext that uses our TrustManager
        val context = SSLContext.getInstance("TLS")
        context.init(kmf.keyManagers, tmf.trustManagers, null)
        factory = context.socketFactory
    }

    override fun getDefaultCipherSuites(): Array<String> = factory.defaultCipherSuites

    override fun getSupportedCipherSuites(): Array<String> = factory.supportedCipherSuites

    @Throws(IOException::class)
    override fun createSocket(): Socket = (factory.createSocket() as SSLSocket).apply {
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
    ): Socket = (factory.createSocket(host, port, localHost, localPort) as SSLSocket).apply {
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
    ): Socket = (factory.createSocket(address, port, localAddress, localPort) as SSLSocket).apply {
        enabledProtocols = protocols
        soTimeout = socketTimeout
    }
}
