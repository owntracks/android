package org.owntracks.android.services

import android.content.Context
import java.security.cert.CertificateFactory
import java.security.cert.X509Certificate
import org.owntracks.android.support.SocketFactory
import timber.log.Timber

interface ConnectionConfiguration {
    fun validate()
    fun getCaCert(context: Context, tlsCaCrtPath: String): X509Certificate? {
        if (tlsCaCrtPath.isBlank()) {
            return null
        }
        try {
            context.openFileInput(tlsCaCrtPath)
                .use { caFileInputStream ->
                    return CertificateFactory.getInstance("X.509", "BC")
                        .generateCertificate(caFileInputStream) as X509Certificate
                }
        } catch (e: Exception) {
            Timber.e(e, "Could not create CA certificate from $tlsCaCrtPath")
            return null
        }
    }

    fun getSocketFactory(
        connectionTimeoutSeconds: Int,
        tls: Boolean,
        tlsCaCrt: X509Certificate?,
        tlsClientCrt: String,
        tlsClientCrtPassword: String,
        context: Context
    ): SocketFactory {
        val socketFactoryOptions = SocketFactory.SocketFactoryOptions()
            .withSocketTimeout(connectionTimeoutSeconds)
        if (tls) {
            if (tlsCaCrt != null) {
                socketFactoryOptions.withCaCertificate(tlsCaCrt.encoded)
            }
        }
        if (tlsClientCrt.isNotEmpty()) {
            context.openFileInput(tlsClientCrt)
                .use {
                    socketFactoryOptions.withClientP12Certificate(it.readBytes())
                        .withClientP12Password(tlsClientCrtPassword)
                }
        }

        return SocketFactory(socketFactoryOptions)
    }
}
