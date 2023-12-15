package org.owntracks.android.services

import android.content.Context
import java.security.KeyStore
import org.owntracks.android.support.SocketFactory

interface ConnectionConfiguration {
    fun validate()

    fun getSocketFactory(
        connectionTimeoutSeconds: Int,
        tls: Boolean,
        tlsClientCrt: String,
        context: Context,
        caKeyStore: KeyStore
    ): SocketFactory =
        SocketFactory(
            SocketFactory.SocketFactoryOptions()
                .apply {
                    socketTimeout = connectionTimeoutSeconds
                    if (tls) {
                        clientCertificateAlias = tlsClientCrt
                    }
                },
            caKeyStore,
            context
        )
}
