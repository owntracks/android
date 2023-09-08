package org.owntracks.android.services

import java.io.InputStream
import java.io.OutputStream
import java.security.Key
import java.security.KeyStoreSpi
import java.security.Provider
import java.security.Security
import java.security.cert.Certificate
import java.util.Date
import java.util.Enumeration

internal class FakeAndroidKeyStoreProvider : Provider(
    "AndroidCAStore",
    1.0,
    "Fake AndroidCAStore provider"
) {

    init {
        put(
            "KeyStore.AndroidCAStore",
            TestingKeyStore::class.java.name
        )
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

    override fun engineSetKeyEntry(alias: String?, key: Key?, password: CharArray?, chain: Array<out Certificate>?) {
        TODO("Not yet implemented")
    }

    override fun engineSetKeyEntry(alias: String?, key: ByteArray?, chain: Array<out Certificate>?) {
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
