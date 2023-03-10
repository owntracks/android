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
    ): SocketFactory =
        SocketFactory(
            SocketFactory.SocketFactoryOptions()
                .apply {
                    socketTimeout = connectionTimeoutSeconds
                    if (tls) {
                        if (tlsCaCrt != null) {
                            caCrt = tlsCaCrt.encoded
                        }
                        if (tlsClientCrt.isNotEmpty()) {
                            context.openFileInput(tlsClientCrt)
                                .use {
                                    clientP12Certificate = it.readBytes()
                                    caClientP12Password = tlsClientCrtPassword
                                }
                        }
                    }
                }
        )
}
