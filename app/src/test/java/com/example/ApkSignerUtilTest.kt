package com.example

import com.example.core.ApkSignerUtil
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ApkSignerUtilTest {

    @Test
    fun testSha256Computation() {
        val input = "Hello Android APK and ZIP converter".toByteArray(Charsets.UTF_8)
        val hash = ApkSignerUtil.computeSha256(input)
        assertNotNull(hash)
        assertTrue(hash.isNotEmpty())
    }

    @Test
    fun testManifestAndCertGeneration() {
        val digests = listOf(
            ApkSignerUtil.EntryDigest("AndroidManifest.xml", "dummyDigest1"),
            ApkSignerUtil.EntryDigest("classes.dex", "dummyDigest2")
        )
        val manifestBytes = ApkSignerUtil.createManifestMf(digests)
        val manifestStr = String(manifestBytes, Charsets.UTF_8)
        assertTrue(manifestStr.contains("Manifest-Version: 1.0"))
        assertTrue(manifestStr.contains("Name: AndroidManifest.xml"))
        assertTrue(manifestStr.contains("SHA-256-Digest: dummyDigest1"))

        val certSfBytes = ApkSignerUtil.createCertSf(manifestBytes, digests)
        val certSfStr = String(certSfBytes, Charsets.UTF_8)
        assertTrue(certSfStr.contains("Signature-Version: 1.0"))
        assertTrue(certSfStr.contains("SHA-256-Digest-Manifest:"))

        val certRsaBytes = ApkSignerUtil.createCertRsa(certSfBytes)
        assertNotNull(certRsaBytes)
        assertTrue(certRsaBytes.isNotEmpty())
    }
}
