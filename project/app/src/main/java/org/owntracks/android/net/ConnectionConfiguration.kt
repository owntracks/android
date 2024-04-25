package org.owntracks.android.net

import android.content.Context
import java.security.KeyStore
import java.util.concurrent.TimeUnit
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
          SocketFactory.SocketFactoryOptions().apply {
            socketTimeout = TimeUnit.SECONDS.toMillis(connectionTimeoutSeconds.toLong()).toInt()
            if (tls) {
              clientCertificateAlias = tlsClientCrt
            }
          },
          caKeyStore,
          context)
}
