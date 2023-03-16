package org.owntracks.android.services

import android.content.Context
import android.util.Base64
import android.util.Base64.NO_WRAP
import java.io.ByteArrayInputStream
import java.security.cert.CertificateFactory
import java.security.cert.X509Certificate
import org.owntracks.android.support.SocketFactory
import timber.log.Timber

interface ConnectionConfiguration {
    fun validate()
    fun getCaCert(tlsCaCrtBase64: String): X509Certificate? {
        if (tlsCaCrtBase64.isBlank()) {
            return null
        }

        try {
            val decoded = Base64.decode(tlsCaCrtBase64, NO_WRAP)
            ByteArrayInputStream(decoded).use {
                return CertificateFactory.getInstance("X.509", "BC")
                    .generateCertificate(it) as X509Certificate
            }
        } catch (e: Exception) {
            Timber.e(e, "Could not create CA certificate")
            return null
        }
    }

    fun getClientCert(tlsClientCrtBase64: String): ByteArray? {
        return if (tlsClientCrtBase64.isBlank()) {
            null
        } else {
            try {
                Base64.decode(tlsClientCrtBase64, NO_WRAP)
            } catch (e: Exception) {
                null
            }
        }
    }

    fun getSocketFactory(
        connectionTimeoutSeconds: Int,
        tls: Boolean,
        tlsCaCrt: X509Certificate?,
        tlsClientCrt: ByteArray?,
        tlsClientCrtPassword: String,
        context: Context
    ): SocketFactory =
        SocketFactory(
            SocketFactory.SocketFactoryOptions()
                .apply {
                    socketTimeout = connectionTimeoutSeconds
                    if (tls) {
                        if (tlsCaCrt != null) {
                            caCrt = tlsCaCrt.encoded
                        }
                        if (tlsClientCrt != null) {
                            clientP12Certificate = tlsClientCrt
                            caClientP12Password = tlsClientCrtPassword
                        }
                    }
                }
        )
}
