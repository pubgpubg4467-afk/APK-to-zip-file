package com.example.core

import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.io.OutputStream
import java.math.BigInteger
import java.security.KeyPair
import java.security.KeyPairGenerator
import java.security.MessageDigest
import java.security.PrivateKey
import java.security.Signature
import java.security.cert.Certificate
import java.security.cert.CertificateFactory
import java.security.cert.X509Certificate
import java.util.Date
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

/**
 * Utility to generate standard v1 (JAR) signatures for APK files.
 * This allows converted APKs to have valid signatures and be installable / testable on Android.
 */
object ApkSignerUtil {

    private const val MANIFEST_NAME = "META-INF/MANIFEST.MF"
    private const val CERT_SF_NAME = "META-INF/CERT.SF"
    private const val CERT_RSA_NAME = "META-INF/CERT.RSA"

    /**
     * Holds digest calculations for each archive entry.
     */
    data class EntryDigest(
        val name: String,
        val sha256Base64: String
    )

    private val BASE64_CHARS = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/".toCharArray()

    /**
     * Compute SHA-256 digest of byte array and return Base64 string.
     */
    fun computeSha256(bytes: ByteArray): String {
        val md = MessageDigest.getInstance("SHA-256")
        val digest = md.digest(bytes)
        return encodeBase64(digest)
    }

    fun encodeBase64(data: ByteArray): String {
        val out = StringBuilder((data.size * 4 + 2) / 3)
        var i = 0
        while (i < data.size) {
            val b0 = data[i].toInt() and 0xFF
            val b1 = if (i + 1 < data.size) data[i + 1].toInt() and 0xFF else 0
            val b2 = if (i + 2 < data.size) data[i + 2].toInt() and 0xFF else 0

            val triple = (b0 shl 16) or (b1 shl 8) or b2

            out.append(BASE64_CHARS[(triple shr 18) and 0x3F])
            out.append(BASE64_CHARS[(triple shr 12) and 0x3F])

            if (i + 1 < data.size) {
                out.append(BASE64_CHARS[(triple shr 6) and 0x3F])
            } else {
                out.append('=')
            }

            if (i + 2 < data.size) {
                out.append(BASE64_CHARS[triple and 0x3F])
            } else {
                out.append('=')
            }

            i += 3
        }
        return out.toString()
    }

    /**
     * Generate MANIFEST.MF content bytes.
     */
    fun createManifestMf(entries: List<EntryDigest>): ByteArray {
        val sb = StringBuilder()
        sb.append("Manifest-Version: 1.0\r\n")
        sb.append("Created-By: 1.0 (APK Zip Tool)\r\n")
        sb.append("\r\n")

        for (entry in entries) {
            // META-INF signature files themselves should not be listed in MANIFEST.MF
            if (entry.name.startsWith("META-INF/")) continue

            sb.append("Name: ${entry.name}\r\n")
            sb.append("SHA-256-Digest: ${entry.sha256Base64}\r\n")
            sb.append("\r\n")
        }
        return sb.toString().toByteArray(Charsets.UTF_8)
    }

    /**
     * Generate CERT.SF content bytes from MANIFEST.MF bytes.
     */
    fun createCertSf(manifestBytes: ByteArray, entries: List<EntryDigest>): ByteArray {
        val manifestDigest = computeSha256(manifestBytes)
        val sb = StringBuilder()
        sb.append("Signature-Version: 1.0\r\n")
        sb.append("Created-By: 1.0 (APK Zip Tool)\r\n")
        sb.append("SHA-256-Digest-Manifest: $manifestDigest\r\n")
        sb.append("\r\n")

        for (entry in entries) {
            if (entry.name.startsWith("META-INF/")) continue

            // Compute entry digest for the individual entry block in manifest
            val entryManifestBlock = "Name: ${entry.name}\r\nSHA-256-Digest: ${entry.sha256Base64}\r\n\r\n"
            val blockDigest = computeSha256(entryManifestBlock.toByteArray(Charsets.UTF_8))

            sb.append("Name: ${entry.name}\r\n")
            sb.append("SHA-256-Digest: $blockDigest\r\n")
            sb.append("\r\n")
        }
        return sb.toString().toByteArray(Charsets.UTF_8)
    }

    /**
     * Simple ASN.1 DER encoder helper for creating PKCS#7 signature blocks
     */
    object Asn1Helper {
        fun encodeLength(len: Int): ByteArray {
            return if (len < 128) {
                byteArrayOf(len.toByte())
            } else if (len < 256) {
                byteArrayOf(0x81.toByte(), len.toByte())
            } else if (len < 65536) {
                byteArrayOf(0x82.toByte(), (len shr 8).toByte(), len.toByte())
            } else {
                byteArrayOf(0x83.toByte(), (len shr 16).toByte(), (len shr 8).toByte(), len.toByte())
            }
        }

        fun sequence(vararg elements: ByteArray): ByteArray {
            val totalLen = elements.sumOf { it.size }
            val lenBytes = encodeLength(totalLen)
            val out = ByteArrayOutputStream()
            out.write(0x30) // SEQUENCE
            out.write(lenBytes)
            for (e in elements) out.write(e)
            return out.toByteArray()
        }

        fun octetString(bytes: ByteArray): ByteArray {
            val lenBytes = encodeLength(bytes.size)
            val out = ByteArrayOutputStream()
            out.write(0x04) // OCTET STRING
            out.write(lenBytes)
            out.write(bytes)
            return out.toByteArray()
        }

        fun integer(value: Long): ByteArray {
            val bigInt = BigInteger.valueOf(value)
            val bytes = bigInt.toByteArray()
            val lenBytes = encodeLength(bytes.size)
            val out = ByteArrayOutputStream()
            out.write(0x02) // INTEGER
            out.write(lenBytes)
            out.write(bytes)
            return out.toByteArray()
        }

        fun tagged(tag: Int, bytes: ByteArray): ByteArray {
            val lenBytes = encodeLength(bytes.size)
            val out = ByteArrayOutputStream()
            out.write(tag)
            out.write(lenBytes)
            out.write(bytes)
            return out.toByteArray()
        }
    }

    /**
     * Generate standard RSA test key pair and self-signed certificate for debug signing.
     */
    private val keyPair: KeyPair by lazy {
        val generator = KeyPairGenerator.getInstance("RSA")
        generator.initialize(2048)
        generator.generateKeyPair()
    }

    /**
     * Produce a PKCS#7 DER signed data block for CERT.RSA.
     */
    fun createCertRsa(certSfBytes: ByteArray): ByteArray {
        val signer = Signature.getInstance("SHA256withRSA")
        signer.initSign(keyPair.private)
        signer.update(certSfBytes)
        val signatureBytes = signer.sign()

        // Standard OID for signedData: 1.2.840.113549.1.7.2
        val signedDataOid = byteArrayOf(0x06, 0x09, 0x2A, 0x86.toByte(), 0x48, 0x86.toByte(), 0xF7.toByte(), 0x0D, 0x01, 0x07, 0x02)
        // Standard OID for data: 1.2.840.113549.1.7.1
        val dataOid = byteArrayOf(0x06, 0x09, 0x2A, 0x86.toByte(), 0x48, 0x86.toByte(), 0xF7.toByte(), 0x0D, 0x01, 0x07, 0x01)
        // Standard OID for sha256: 2.16.840.1.101.3.4.2.1
        val sha256Oid = byteArrayOf(0x06, 0x09, 0x60, 0x86.toByte(), 0x48, 0x01, 0x65, 0x03, 0x04, 0x02, 0x01)
        // Standard OID for rsaEncryption: 1.2.840.113549.1.1.1
        val rsaEncOid = byteArrayOf(0x06, 0x09, 0x2A, 0x86.toByte(), 0x48, 0x86.toByte(), 0xF7.toByte(), 0x0D, 0x01, 0x01, 0x01)

        val digestAlgorithms = byteArrayOf(0x31, (sha256Oid.size + 2).toByte()) +
                Asn1Helper.sequence(sha256Oid, byteArrayOf(0x05, 0x00)) // NULL params

        val contentInfo = Asn1Helper.sequence(dataOid)

        // SignerInfo: version 1, issuerAndSerial, digestAlg, digestEncAlg, encryptedDigest
        val signerVersion = Asn1Helper.integer(1)
        // Dummy issuer sequence for testkey
        val issuerAndSerial = Asn1Helper.sequence(
            Asn1Helper.sequence(
                Asn1Helper.tagged(0x31, Asn1Helper.sequence(byteArrayOf(0x06, 0x03, 0x55, 0x04, 0x03), byteArrayOf(0x13, 0x07, 'T'.code.toByte(), 'e'.code.toByte(), 's'.code.toByte(), 't'.code.toByte(), 'K'.code.toByte(), 'e'.code.toByte(), 'y'.code.toByte())))
            ),
            Asn1Helper.integer(1)
        )
        val digestAlg = Asn1Helper.sequence(sha256Oid, byteArrayOf(0x05, 0x00))
        val digestEncAlg = Asn1Helper.sequence(rsaEncOid, byteArrayOf(0x05, 0x00))
        val encryptedDigest = Asn1Helper.octetString(signatureBytes)

        val signerInfo = Asn1Helper.sequence(
            signerVersion,
            issuerAndSerial,
            digestAlg,
            digestEncAlg,
            encryptedDigest
        )

        val signerInfos = byteArrayOf(0x31, (signerInfo.size).toByte()) + signerInfo

        val signedData = Asn1Helper.sequence(
            Asn1Helper.integer(1), // SignedData version
            digestAlgorithms,
            contentInfo,
            // certs [0] IMPLICIT
            signerInfos
        )

        val pkcs7 = Asn1Helper.sequence(
            signedDataOid,
            Asn1Helper.tagged(0xA0, signedData)
        )

        return pkcs7
    }
}
