package org.owntracks.android.net

import java.security.KeyStore
import java.security.MessageDigest
import java.security.cert.Certificate
import javax.net.ssl.HostnameVerifier
import javax.net.ssl.SSLSession
import org.conscrypt.OkHostnameVerifier
import timber.log.Timber

/***
 * A Hostname verifier for use on connections that skips the verification if the server leaf
 * certificate matches the supplied CA certificate. This is typically for use with self-signed
 * certificates where the CA is the same as the leaf cert presented by the endpoint.
 */
class CALeafCertMatchingHostnameVerifier(keystore: KeyStore = KeyStore.getInstance("AndroidCAStore")) :
    HostnameVerifier {
    private fun Certificate.getFingerPrint(): ByteArray = MessageDigest.getInstance("SHA-1")
        .digest(this.encoded)

    private val caKeyStore = keystore.apply { load(null) }

    private fun caStoreContains(fingerprint: ByteArray): Boolean =
        caKeyStore.aliases().asSequence().firstOrNull {
            caKeyStore.getCertificate(it).getFingerPrint().contentEquals(fingerprint)
        } != null

    override fun verify(hostname: String?, session: SSLSession?): Boolean {
        val peerCertificates = session?.peerCertificates
        if (peerCertificates.isNullOrEmpty()) {
            Timber.e("No server peer certificates presented for SSL session.")
            return OkHostnameVerifier.INSTANCE.verify(emptyArray(), hostname, session)
        }
        val cert = peerCertificates[0].encoded ?: byteArrayOf()
        val fingerprint = MessageDigest.getInstance("SHA-1")
            .digest(cert)

        return if (caStoreContains(fingerprint)) {
            Timber.i("CA Fingerprint matches server leaf cert: $fingerprint. Skipping hostname verification")
            true
        } else {
            OkHostnameVerifier.INSTANCE.verify(emptyArray(), hostname, session)
        }
    }
}
