package com.example.core

import android.content.Context
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

object ZipToApkEngine {

    data class PackagingOptions(
        val autoSign: Boolean = true,
        val removeOldSignatures: Boolean = true
    )

    data class PackagingResult(
        val success: Boolean,
        val outputFile: File?,
        val totalEntries: Int,
        val outputSizeBytes: Long,
        val isSigned: Boolean,
        val hasAndroidManifest: Boolean,
        val warningMessage: String? = null,
        val errorMessage: String? = null
    )

    data class CachedZipEntry(
        val name: String,
        val data: ByteArray,
        val time: Long
    )

    suspend fun convertZipToApk(
        context: Context,
        inputUri: Uri,
        outputFileName: String,
        options: PackagingOptions = PackagingOptions(),
        onProgress: (progress: Float, statusMessage: String) -> Unit
    ): PackagingResult = withContext(Dispatchers.IO) {
        try {
            onProgress(0.05f, "Reading input ZIP archive...")

            val outputDir = File(context.filesDir, "converted_apks").apply { mkdirs() }
            val cleanName = if (outputFileName.endsWith(".apk", ignoreCase = true)) {
                outputFileName
            } else {
                "${outputFileName.substringBeforeLast(".")}.apk"
            }
            val targetFile = File(outputDir, cleanName)
            if (targetFile.exists()) targetFile.delete()

            val contentResolver = context.contentResolver
            val inputStream = contentResolver.openInputStream(inputUri)
                ?: return@withContext PackagingResult(
                    success = false,
                    outputFile = null,
                    totalEntries = 0,
                    outputSizeBytes = 0L,
                    isSigned = false,
                    hasAndroidManifest = false,
                    errorMessage = "Unable to open input ZIP stream"
                )

            var hasAndroidManifest = false
            val entriesList = mutableListOf<CachedZipEntry>()
            val digests = mutableListOf<ApkSignerUtil.EntryDigest>()

            // Pass 1: Read all entries
            val buffer = ByteArray(32 * 1024)
            ZipInputStream(BufferedInputStream(inputStream)).use { zis ->
                var entry: ZipEntry? = zis.nextEntry
                var counter = 0
                while (entry != null) {
                    val name = entry.name
                    if (name.equals("AndroidManifest.xml", ignoreCase = true)) {
                        hasAndroidManifest = true
                    }

                    val isOldSig = name.startsWith("META-INF/") && (
                            name.endsWith(".SF", ignoreCase = true) ||
                                    name.endsWith(".RSA", ignoreCase = true) ||
                                    name.endsWith(".DSA", ignoreCase = true) ||
                                    name.endsWith(".EC", ignoreCase = true) ||
                                    name.equals("META-INF/MANIFEST.MF", ignoreCase = true)
                            )

                    if (!options.removeOldSignatures || !isOldSig) {
                        if (!entry.isDirectory) {
                            val baos = ByteArrayOutputStream()
                            var len: Int
                            while (zis.read(buffer).also { len = it } != -1) {
                                baos.write(buffer, 0, len)
                            }
                            val bytes = baos.toByteArray()
                            entriesList.add(CachedZipEntry(name, bytes, entry.time))

                            if (options.autoSign && !name.startsWith("META-INF/")) {
                                val sha256 = ApkSignerUtil.computeSha256(bytes)
                                digests.add(ApkSignerUtil.EntryDigest(name, sha256))
                            }
                        } else {
                            // Directory entry
                            entriesList.add(CachedZipEntry(name, ByteArray(0), entry.time))
                        }
                    }

                    counter++
                    if (counter % 40 == 0) {
                        onProgress(0.1f + (counter * 0.001f).coerceAtMost(0.45f), "Reading: $name")
                    }

                    zis.closeEntry()
                    entry = zis.nextEntry
                }
            }

            if (entriesList.isEmpty()) {
                return@withContext PackagingResult(
                    success = false,
                    outputFile = null,
                    totalEntries = 0,
                    outputSizeBytes = 0L,
                    isSigned = false,
                    hasAndroidManifest = false,
                    errorMessage = "ZIP archive is empty"
                )
            }

            var warning: String? = null
            if (!hasAndroidManifest) {
                warning = "Warning: AndroidManifest.xml not detected in ZIP. This APK may not be directly installable."
            }

            // If signing is enabled, compute MANIFEST.MF, CERT.SF, CERT.RSA
            val sigEntries = mutableListOf<CachedZipEntry>()
            if (options.autoSign) {
                onProgress(0.6f, "Generating APK digital signatures (v1 testkey)...")
                val manifestBytes = ApkSignerUtil.createManifestMf(digests)
                val certSfBytes = ApkSignerUtil.createCertSf(manifestBytes, digests)
                val certRsaBytes = ApkSignerUtil.createCertRsa(certSfBytes)
                val now = System.currentTimeMillis()

                sigEntries.add(CachedZipEntry("META-INF/MANIFEST.MF", manifestBytes, now))
                sigEntries.add(CachedZipEntry("META-INF/CERT.SF", certSfBytes, now))
                sigEntries.add(CachedZipEntry("META-INF/CERT.RSA", certRsaBytes, now))
            }

            // Pass 2: Write out to target APK
            onProgress(0.75f, "Writing final APK package...")
            ZipOutputStream(BufferedOutputStream(FileOutputStream(targetFile))).use { zos ->
                // Write signatures first if signing
                for (sig in sigEntries) {
                    val ze = ZipEntry(sig.name).apply { time = sig.time }
                    zos.putNextEntry(ze)
                    zos.write(sig.data)
                    zos.closeEntry()
                }

                var writeCounter = 0
                for (item in entriesList) {
                    val ze = ZipEntry(item.name).apply { time = item.time }
                    zos.putNextEntry(ze)
                    if (item.data.isNotEmpty()) {
                        zos.write(item.data)
                    }
                    zos.closeEntry()
                    writeCounter++
                    if (writeCounter % 50 == 0) {
                        onProgress(0.75f + (writeCounter.toFloat() / entriesList.size * 0.2f), "Packaging ${item.name}")
                    }
                }
            }

            onProgress(1.0f, "APK successfully built!")
            PackagingResult(
                success = true,
                outputFile = targetFile,
                totalEntries = entriesList.size + sigEntries.size,
                outputSizeBytes = targetFile.length(),
                isSigned = options.autoSign,
                hasAndroidManifest = hasAndroidManifest,
                warningMessage = warning
            )
        } catch (e: Exception) {
            e.printStackTrace()
            PackagingResult(
                success = false,
                outputFile = null,
                totalEntries = 0,
                outputSizeBytes = 0L,
                isSigned = false,
                hasAndroidManifest = false,
                errorMessage = e.localizedMessage ?: "Failed to build APK"
            )
        }
    }
}
