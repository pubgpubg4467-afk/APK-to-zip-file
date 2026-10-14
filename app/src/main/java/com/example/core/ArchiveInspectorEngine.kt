package com.example.core

import android.content.Context
import android.net.Uri
import com.example.model.ArchiveEntryItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedInputStream
import java.io.BufferedReader
import java.io.File
import java.io.FileOutputStream
import java.io.InputStreamReader
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream

object ArchiveInspectorEngine {

    suspend fun getArchiveEntries(
        context: Context,
        uri: Uri
    ): List<ArchiveEntryItem> = withContext(Dispatchers.IO) {
        val result = mutableListOf<ArchiveEntryItem>()
        val inputStream = context.contentResolver.openInputStream(uri) ?: return@withContext emptyList()
        ZipInputStream(BufferedInputStream(inputStream)).use { zis ->
            var entry: ZipEntry? = zis.nextEntry
            while (entry != null) {
                result.add(
                    ArchiveEntryItem(
                        name = entry.name,
                        size = entry.size,
                        compressedSize = entry.compressedSize,
                        isDirectory = entry.isDirectory,
                        crc = entry.crc,
                        lastModified = entry.time
                    )
                )
                zis.closeEntry()
                entry = zis.nextEntry
            }
        }
        result
    }

    suspend fun readEntryTextPreview(
        context: Context,
        uri: Uri,
        entryName: String,
        maxLines: Int = 100
    ): String = withContext(Dispatchers.IO) {
        val inputStream = context.contentResolver.openInputStream(uri) ?: return@withContext "Unable to open file"
        ZipInputStream(BufferedInputStream(inputStream)).use { zis ->
            var entry: ZipEntry? = zis.nextEntry
            while (entry != null) {
                if (entry.name == entryName) {
                    val reader = BufferedReader(InputStreamReader(zis))
                    val sb = StringBuilder()
                    var linesRead = 0
                    var line: String? = reader.readLine()
                    while (line != null && linesRead < maxLines) {
                        sb.append(line).append("\n")
                        linesRead++
                        line = reader.readLine()
                    }
                    if (linesRead >= maxLines) {
                        sb.append("\n... [Preview truncated at $maxLines lines]")
                    }
                    return@withContext sb.toString().ifEmpty { "(Empty file)" }
                }
                zis.closeEntry()
                entry = zis.nextEntry
            }
        }
        "Entry '$entryName' not found"
    }

    suspend fun extractEntryToTemp(
        context: Context,
        uri: Uri,
        entryName: String
    ): File? = withContext(Dispatchers.IO) {
        val inputStream = context.contentResolver.openInputStream(uri) ?: return@withContext null
        val tempDir = File(context.cacheDir, "extracted_entries").apply { mkdirs() }
        val fileName = File(entryName).name
        val outFile = File(tempDir, fileName)

        ZipInputStream(BufferedInputStream(inputStream)).use { zis ->
            var entry: ZipEntry? = zis.nextEntry
            while (entry != null) {
                if (entry.name == entryName) {
                    FileOutputStream(outFile).use { fos ->
                        val buffer = ByteArray(16 * 1024)
                        var read: Int
                        while (zis.read(buffer).also { read = it } != -1) {
                            fos.write(buffer, 0, read)
                        }
                    }
                    return@withContext outFile
                }
                zis.closeEntry()
                entry = zis.nextEntry
            }
        }
        null
    }
}
