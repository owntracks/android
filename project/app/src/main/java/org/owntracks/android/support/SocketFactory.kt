package org.owntracks.android.support

import timber.log.Timber
import java.io.IOException
import java.io.InputStream
import java.net.InetAddress
import java.net.Socket
import java.security.KeyStore
import java.security.cert.CertificateFactory
import java.security.cert.X509Certificate
import javax.net.ssl.*

class SocketFactory(options: SocketFactoryOptions) : SSLSocketFactory() {
    private val factory: SSLSocketFactory
    private val protocols = arrayOf("TLSv1", "TLSv1.1", "TLSv1.2", "TLSv1.3")

    class SocketFactoryOptions {
        var caCrtInputStream: InputStream? = null
            private set
        var caClientP12InputStream: InputStream? = null
            private set
        var caClientP12Password: String? = null
            private set

        fun withCaInputStream(stream: InputStream?): SocketFactoryOptions {
            caCrtInputStream = stream
            return this
        }

        fun withClientP12InputStream(stream: InputStream?): SocketFactoryOptions {
            caClientP12InputStream = stream
            return this
        }

        fun withClientP12Password(password: String?): SocketFactoryOptions {
            caClientP12Password = password
            return this
        }

        fun hasCaCrt(): Boolean {
            return caCrtInputStream != null
        }

        fun hasClientP12Crt(): Boolean {
            return caClientP12Password != null
        }

        fun hasClientP12Password(): Boolean {
            return caClientP12Password != null && caClientP12Password != ""
        }
    }

    private val tmf: TrustManagerFactory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm())
    val trustManagers: Array<TrustManager>
        get() = tmf.trustManagers

    override fun getDefaultCipherSuites(): Array<String> {
        return factory.defaultCipherSuites
    }

    override fun getSupportedCipherSuites(): Array<String> {
        return factory.supportedCipherSuites
    }

    @Throws(IOException::class)
    override fun createSocket(): Socket {
        val r = factory.createSocket() as SSLSocket
        r.enabledProtocols = protocols
        return r
    }

    @Throws(IOException::class)
    override fun createSocket(s: Socket, host: String, port: Int, autoClose: Boolean): Socket {
        val r = factory.createSocket(s, host, port, autoClose) as SSLSocket
        r.enabledProtocols = protocols
        return r
    }

    @Throws(IOException::class)
    override fun createSocket(host: String, port: Int): Socket {
        val r = factory.createSocket(host, port) as SSLSocket
        r.enabledProtocols = protocols
        return r
    }

    @Throws(IOException::class)
    override fun createSocket(
            host: String,
            port: Int,
            localHost: InetAddress,
            localPort: Int
    ): Socket {
        val r = factory.createSocket(host, port, localHost, localPort) as SSLSocket
        r.enabledProtocols = protocols
        return r
    }

    @Throws(IOException::class)
    override fun createSocket(host: InetAddress, port: Int): Socket {
        val r = factory.createSocket(host, port) as SSLSocket
        r.enabledProtocols = protocols
        return r
    }

    @Throws(IOException::class)
    override fun createSocket(
            address: InetAddress,
            port: Int,
            localAddress: InetAddress,
            localPort: Int
    ): Socket {
        val r = factory.createSocket(address, port, localAddress, localPort) as SSLSocket
        r.enabledProtocols = protocols
        return r
    }

    init {
        val kmf = KeyManagerFactory.getInstance("X509")
        if (options.hasCaCrt()) {
            Timber.v("options.hasCaCrt(): true")
            val caKeyStore = KeyStore.getInstance(KeyStore.getDefaultType())
            caKeyStore.load(null, null)
            val caCF = CertificateFactory.getInstance("X.509")
            val ca = caCF.generateCertificate(options.caCrtInputStream) as X509Certificate
            val alias = ca.subjectX500Principal.name
            // Set propper alias name
            caKeyStore.setCertificateEntry(alias, ca)
            tmf.init(caKeyStore)
            Timber.v("Certificate Owner: %s", ca.subjectDN.toString())
            Timber.v("Certificate Issuer: %s", ca.issuerDN.toString())
            Timber.v("Certificate Serial Number: %s", ca.serialNumber.toString())
            Timber.v("Certificate Algorithm: %s", ca.sigAlgName)
            Timber.v("Certificate Version: %s", ca.version)
            Timber.v("Certificate OID: %s", ca.sigAlgOID)
            val aliasesCA = caKeyStore.aliases()
            while (aliasesCA.hasMoreElements()) {
                val o = aliasesCA.nextElement()
                Timber.v(
                        "Alias: %s isKeyEntry:%s isCertificateEntry:%s",
                        o,
                        caKeyStore.isKeyEntry(o),
                        caKeyStore.isCertificateEntry(o)
                )
            }
        } else {
            Timber.v("CA sideload: false, using system keystore")
            val keyStore = KeyStore.getInstance("AndroidCAStore")
            keyStore.load(null)
            tmf.init(keyStore)
        }
        if (options.hasClientP12Crt()) {
            Timber.v("options.hasClientP12Crt(): true")
            val clientKeyStore = KeyStore.getInstance("PKCS12")
            clientKeyStore.load(
                    options.caClientP12InputStream,
                    if (options.hasClientP12Password()) options.caClientP12Password!!.toCharArray() else CharArray(
                            0
                    )
            )
            kmf.init(
                    clientKeyStore,
                    if (options.hasClientP12Password()) options.caClientP12Password!!.toCharArray() else CharArray(
                            0
                    )
            )
            Timber.v("Client .p12 Keystore content: ")
            val aliasesClientCert = clientKeyStore.aliases()
            while (aliasesClientCert.hasMoreElements()) {
                val o = aliasesClientCert.nextElement()
                Timber.v("Alias: %s", o)
            }
        } else {
            Timber.v("Client .p12 sideload: false, using null client cert")
            kmf.init(null, null)
        }

        // Create an SSLContext that uses our TrustManager
        val context = SSLContext.getInstance("TLS")
        context.init(kmf.keyManagers, trustManagers, null)
        factory = context.socketFactory
    }
}