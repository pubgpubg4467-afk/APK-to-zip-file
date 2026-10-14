package com.example.core

import android.content.Context
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.util.zip.Deflater
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

object ApkToZipEngine {

    data class ConversionOptions(
        val stripSignatures: Boolean = false,
        val compressionLevel: Int = Deflater.DEFAULT_COMPRESSION // 0 = STORE, 6 = DEFAULT, 9 = BEST
    )

    data class ConversionResult(
        val success: Boolean,
        val outputFile: File?,
        val totalEntries: Int,
        val outputSizeBytes: Long,
        val errorMessage: String? = null
    )

    suspend fun convertApkToZip(
        context: Context,
        inputUri: Uri,
        outputFileName: String,
        options: ConversionOptions = ConversionOptions(),
        onProgress: (progress: Float, statusMessage: String) -> Unit
    ): ConversionResult = withContext(Dispatchers.IO) {
        try {
            onProgress(0.05f, "Preparing APK stream...")

            val outputDir = File(context.filesDir, "converted_zips").apply { mkdirs() }
            val cleanName = if (outputFileName.endsWith(".zip", ignoreCase = true)) {
                outputFileName
            } else {
                "${outputFileName.substringBeforeLast(".")}.zip"
            }
            val targetFile = File(outputDir, cleanName)
            if (targetFile.exists()) targetFile.delete()

            val contentResolver = context.contentResolver
            val inputStream = contentResolver.openInputStream(inputUri)
                ?: return@withContext ConversionResult(false, null, 0, 0L, "Could not open input APK stream")

            var totalProcessed = 0
            val buffer = ByteArray(32 * 1024)

            ZipInputStream(BufferedInputStream(inputStream)).use { zis ->
                ZipOutputStream(BufferedOutputStream(FileOutputStream(targetFile))).use { zos ->
                    zos.setLevel(options.compressionLevel)

                    var entry: ZipEntry? = zis.nextEntry
                    while (entry != null) {
                        val entryName = entry.name

                        // If user requested stripping old signatures:
                        val isSignatureFile = entryName.startsWith("META-INF/") && (
                                entryName.endsWith(".SF", ignoreCase = true) ||
                                        entryName.endsWith(".RSA", ignoreCase = true) ||
                                        entryName.endsWith(".DSA", ignoreCase = true) ||
                                        entryName.endsWith(".EC", ignoreCase = true) ||
                                        entryName.equals("META-INF/MANIFEST.MF", ignoreCase = true)
                                )

                        if (!(options.stripSignatures && isSignatureFile)) {
                            val newEntry = ZipEntry(entryName).apply {
                                time = entry.time
                                comment = entry.comment
                            }
                            zos.putNextEntry(newEntry)
                            var read: Int
                            while (zis.read(buffer).also { read = it } != -1) {
                                zos.write(buffer, 0, read)
                            }
                            zos.closeEntry()
                            totalProcessed++

                            if (totalProcessed % 50 == 0) {
                                onProgress(0.1f + (totalProcessed * 0.001f).coerceAtMost(0.85f), "Repackaging: $entryName")
                            }
                        }

                        zis.closeEntry()
                        entry = zis.nextEntry
                    }
                }
            }

            onProgress(1.0f, "Conversion completed!")
            ConversionResult(
                success = true,
                outputFile = targetFile,
                totalEntries = totalProcessed,
                outputSizeBytes = targetFile.length()
            )
        } catch (e: Exception) {
            e.printStackTrace()
            ConversionResult(false, null, 0, 0L, e.localizedMessage ?: "Conversion failed")
        }
    }
}
