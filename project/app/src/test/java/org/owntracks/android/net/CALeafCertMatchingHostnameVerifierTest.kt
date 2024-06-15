package org.owntracks.android.net

import java.io.ByteArrayInputStream
import java.io.InputStream
import java.io.OutputStream
import java.security.Key
import java.security.KeyStore
import java.security.KeyStoreSpi
import java.security.Principal
import java.security.Provider
import java.security.Security
import java.security.cert.Certificate
import java.security.cert.CertificateFactory
import java.util.Date
import java.util.Enumeration
import javax.net.ssl.SSLSession
import javax.net.ssl.SSLSessionContext
import javax.security.cert.X509Certificate
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class CALeafCertMatchingHostnameVerifierTest {
  private val letsEncryptRootCert =
      this.javaClass.getResource("/letsEncryptRootCA.pem")!!.readBytes()
  private val letsEncryptSignedLeaf =
      this.javaClass.getResource("/letsEncryptSignedLeafX509Certificate.pem")!!.readBytes()
  private val selfSignedCert =
      this.javaClass.getResource("/selfSignedX509Certificate.pem")!!.readBytes()

  @Before
  fun `Init Fake KeyStoreProvider`() {
    FakeAndroidKeyStoreProvider.setup()
  }

  @Test
  fun `Given a standard signed certificate, MqttHostnameVerifier should delegate to HTTPS implementation and succeed if hostnames are the same`() {

    val testCA =
        CertificateFactory.getInstance("X.509")
            .generateCertificate(ByteArrayInputStream(letsEncryptRootCert))
    val testLeaf =
        CertificateFactory.getInstance("X.509")
            .generateCertificate(ByteArrayInputStream(letsEncryptSignedLeaf))
    val sslSession = TestSSLSession(listOf(testLeaf, testCA))
    assertTrue(
        CALeafCertMatchingHostnameVerifier().verify("valid-isrgrootx1.letsencrypt.org", sslSession))
  }

  @Test
  fun `Given a standard signed certificate, MqttHostnameVerifier should delegate to HTTPS implementation and fail if hostnames are the different`() {
    val testCA =
        CertificateFactory.getInstance("X.509")
            .generateCertificate(ByteArrayInputStream(letsEncryptRootCert))
    val testLeaf =
        CertificateFactory.getInstance("X.509")
            .generateCertificate(ByteArrayInputStream(letsEncryptSignedLeaf))
    val sslSession = TestSSLSession(listOf(testLeaf, testCA))
    assertFalse(CALeafCertMatchingHostnameVerifier().verify("host.evil.org", sslSession))
  }

  @Test
  fun `Given a self-signed certificate, MqttHostnameVerifier should skip validation and succeed even if hostnames are different`() {
    val selfSigned =
        CertificateFactory.getInstance("X.509")
            .generateCertificate(ByteArrayInputStream(selfSignedCert))
    val sslSession = TestSSLSession(listOf(selfSigned))
    val keystore =
        KeyStore.getInstance("AndroidCAStore").apply {
          load(null)
          setCertificateEntry("selfsigned", selfSigned)
        }
    assertTrue(CALeafCertMatchingHostnameVerifier(keystore).verify("host.evil.org", sslSession))
  }

  @Test
  fun `Given a self-signed certificate, MqttHostnameVerifier should skip validation and succeed even if hostnames are the same`() {
    val selfSigned =
        CertificateFactory.getInstance("X.509")
            .generateCertificate(ByteArrayInputStream(selfSignedCert))
    val sslSession = TestSSLSession(listOf(selfSigned))
    assertTrue(CALeafCertMatchingHostnameVerifier().verify("test.example.com", sslSession))
  }

  private class TestSSLSession(private val testPeerCertificates: List<Certificate>) : SSLSession {
    override fun getId(): ByteArray {
      TODO("Not implemented")
    }

    override fun getSessionContext(): SSLSessionContext {
      TODO("Not implemented")
    }

    override fun getCreationTime(): Long {
      TODO("Not implemented")
    }

    override fun getLastAccessedTime(): Long {
      TODO("Not implemented")
    }

    override fun invalidate() {
      TODO("Not implemented")
    }

    override fun isValid(): Boolean {
      TODO("Not implemented")
    }

    override fun putValue(name: String?, value: Any?) {
      TODO("Not implemented")
    }

    override fun getValue(name: String?): Any {
      TODO("Not implemented")
    }

    override fun removeValue(name: String?) {
      TODO("Not implemented")
    }

    override fun getValueNames(): Array<String> {
      TODO("Not implemented")
    }

    override fun getPeerCertificates(): Array<Certificate> {
      return testPeerCertificates.toTypedArray()
    }

    override fun getLocalCertificates(): Array<Certificate> {
      TODO("Not implemented")
    }

    override fun getPeerCertificateChain(): Array<X509Certificate> {
      TODO("Not implemented")
    }

    override fun getPeerPrincipal(): Principal {
      TODO("Not implemented")
    }

    override fun getLocalPrincipal(): Principal {
      TODO("Not implemented")
    }

    override fun getCipherSuite(): String {
      TODO("Not implemented")
    }

    override fun getProtocol(): String {
      TODO("Not implemented")
    }

    override fun getPeerHost(): String {
      TODO("Not implemented")
    }

    override fun getPeerPort(): Int {
      TODO("Not implemented")
    }

    override fun getPacketBufferSize(): Int {
      TODO("Not implemented")
    }

    override fun getApplicationBufferSize(): Int {
      TODO("Not implemented")
    }
  }

  internal class FakeAndroidKeyStoreProvider :
      Provider("AndroidCAStore", 1.0, "Fake AndroidCAStore provider") {

    init {
      put("KeyStore.AndroidCAStore", TestingKeyStore::class.java.name)
    }

    companion object {
      fun setup() {
        Security.addProvider(FakeAndroidKeyStoreProvider())
      }
    }
  }

  class TestingKeyStore : KeyStoreSpi() {
    private val certs = mutableMapOf<String, Certificate>()

    override fun engineGetKey(alias: String?, password: CharArray?): Key {
      TODO("Not yet implemented")
    }

    override fun engineGetCertificateChain(alias: String?): Array<Certificate> {
      TODO("Not yet implemented")
    }

    override fun engineGetCertificate(alias: String): Certificate? {
      return certs[alias]
    }

    override fun engineGetCreationDate(alias: String?): Date {
      TODO("Not yet implemented")
    }

    override fun engineSetKeyEntry(
        alias: String?,
        key: Key?,
        password: CharArray?,
        chain: Array<out Certificate>?
    ) {
      TODO("Not yet implemented")
    }

    override fun engineSetKeyEntry(
        alias: String?,
        key: ByteArray?,
        chain: Array<out Certificate>?
    ) {
      TODO("Not yet implemented")
    }

    override fun engineSetCertificateEntry(alias: String, cert: Certificate) {
      certs[alias] = cert
    }

    override fun engineDeleteEntry(alias: String?) {
      TODO("Not yet implemented")
    }

    override fun engineAliases(): Enumeration<String> {
      return certs.keys.toList().toEnumeration()
    }

    override fun engineContainsAlias(alias: String?): Boolean {
      TODO("Not yet implemented")
    }

    override fun engineSize(): Int {
      TODO("Not yet implemented")
    }

    override fun engineIsKeyEntry(alias: String?): Boolean {
      TODO("Not yet implemented")
    }

    override fun engineIsCertificateEntry(alias: String?): Boolean {
      TODO("Not yet implemented")
    }

    override fun engineGetCertificateAlias(cert: Certificate?): String {
      TODO("Not yet implemented")
    }

    override fun engineStore(stream: OutputStream?, password: CharArray?) {
      TODO("Not yet implemented")
    }

    override fun engineLoad(stream: InputStream?, password: CharArray?) {
      // Noop
    }

    private fun <T> List<T>.toEnumeration(): Enumeration<T> {
      return object : Enumeration<T> {
        var count = 0

        override fun hasMoreElements(): Boolean {
          return this.count < size
        }

        override fun nextElement(): T {
          if (this.count < size) {
            return get(this.count++)
          }
          throw NoSuchElementException("List enumeration asked for more elements than present")
        }
      }
    }
  }
}
