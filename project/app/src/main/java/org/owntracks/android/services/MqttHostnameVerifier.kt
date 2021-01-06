package org.owntracks.android.services

import org.conscrypt.OkHostnameVerifier
import timber.log.Timber
import java.security.MessageDigest
import java.security.cert.Certificate
import javax.net.ssl.HostnameVerifier
import javax.net.ssl.SSLSession

/***
 * A Hostname verifier for use on MQTT connections that skips the verification if the server leaf
 * certificate matches the supplied CA certificate. This is typically for use with self-signed
 * certificates where the CA is the same as the leaf cert presented by the MQTT endpoint.
 */
class MqttHostnameVerifier(caCertificate: Certificate) : HostnameVerifier {
    private val caFingerprint: ByteArray = MessageDigest.getInstance("SHA-1").digest(caCertificate.encoded);

    override fun verify(hostname: String?, session: SSLSession?): Boolean {
        val peerCertificates = session?.peerCertificates
        if (peerCertificates == null || peerCertificates.isEmpty()) {
            Timber.e("No server peer certificates presented for SSL session.")
            return OkHostnameVerifier.INSTANCE.verify(emptyArray(), hostname, session)
        }
        val cert = peerCertificates[0].encoded ?: byteArrayOf()
        val fingerprint = MessageDigest.getInstance("SHA-1").digest(cert)

        return if (caFingerprint.contentEquals(fingerprint)) {
            Timber.i("CA Fingerprint matches server leaf cert: %s. Skipping hostname verification", fingerprint.joinToString("") { "%02x".format(it) })
            true
        } else {
            OkHostnameVerifier.INSTANCE.verify(emptyArray(), hostname, session)
        }
    }
}