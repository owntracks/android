package org.owntracks.android.services

import android.content.Context
import android.util.Base64
import android.util.Base64.NO_WRAP
import org.owntracks.android.support.SocketFactory

interface ConnectionConfiguration {
    fun validate()

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
        tlsClientCrt: ByteArray?,
        tlsClientCrtPassword: String,
        context: Context
    ): SocketFactory =
        SocketFactory(
            SocketFactory.SocketFactoryOptions()
                .apply {
                    socketTimeout = connectionTimeoutSeconds
                    if (tls) {
                        if (tlsClientCrt != null) {
                            clientP12Certificate = tlsClientCrt
                            caClientP12Password = tlsClientCrtPassword
                        }
                    }
                }
        )
}
