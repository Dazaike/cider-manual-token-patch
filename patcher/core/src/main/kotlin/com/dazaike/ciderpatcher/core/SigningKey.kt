package com.dazaike.ciderpatcher.core

import org.bouncycastle.asn1.x500.X500Name
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter
import org.bouncycastle.cert.jcajce.JcaX509v3CertificateBuilder
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder
import java.io.File
import java.math.BigInteger
import java.security.KeyPairGenerator
import java.security.KeyStore
import java.security.MessageDigest
import java.security.PrivateKey
import java.security.cert.X509Certificate
import java.util.Calendar
import java.util.Date

/** The patcher's own APK signing identity. Persist it so later Cider updates install over earlier ones. */
class SigningKey(val privateKey: PrivateKey, val certificate: X509Certificate) {

    fun certSha256(): ByteArray = MessageDigest.getInstance("SHA-256").digest(certificate.encoded)

    fun saveTo(file: File) {
        val store = KeyStore.getInstance("PKCS12")
        store.load(null, null)
        store.setKeyEntry(ALIAS, privateKey, PASSWORD, arrayOf(certificate))
        file.outputStream().use { store.store(it, PASSWORD) }
    }

    companion object {
        private const val ALIAS = "cider-patcher"
        private val PASSWORD = "cider-patcher".toCharArray()

        fun generate(): SigningKey {
            val pair = KeyPairGenerator.getInstance("RSA").apply { initialize(2048) }.generateKeyPair()
            val name = X500Name("CN=Cider Patcher")
            val notBefore = Date(System.currentTimeMillis() - 24L * 60 * 60 * 1000)
            val notAfter = Calendar.getInstance().apply { time = notBefore; add(Calendar.YEAR, 30) }.time
            val holder = JcaX509v3CertificateBuilder(name, BigInteger.ONE, notBefore, notAfter, name, pair.public)
                .build(JcaContentSignerBuilder("SHA256withRSA").build(pair.private))
            return SigningKey(pair.private, JcaX509CertificateConverter().getCertificate(holder))
        }

        fun loadFrom(file: File): SigningKey {
            val store = KeyStore.getInstance("PKCS12")
            file.inputStream().use { store.load(it, PASSWORD) }
            val key = store.getKey(ALIAS, PASSWORD) as PrivateKey
            return SigningKey(key, store.getCertificate(ALIAS) as X509Certificate)
        }
    }
}
