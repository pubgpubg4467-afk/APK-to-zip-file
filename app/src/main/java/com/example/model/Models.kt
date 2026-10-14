package com.example.model

import android.net.Uri

enum class ConversionMode {
    APK_TO_ZIP,
    ZIP_TO_APK
}

enum class ConversionStatus {
    IDLE,
    CONVERTING,
    SUCCESS,
    ERROR
}

data class ArchiveEntryItem(
    val name: String,
    val size: Long,
    val compressedSize: Long,
    val isDirectory: Boolean,
    val crc: Long = 0L,
    val lastModified: Long = 0L
)

data class ApkDetails(
    val packageName: String,
    val appName: String,
    val versionName: String,
    val versionCode: Long,
    val minSdk: Int,
    val targetSdk: Int,
    val permissions: List<String> = emptyList(),
    val dexCount: Int = 0,
    val totalEntries: Int = 0,
    val totalSize: Long = 0L,
    val isSigned: Boolean = false,
    val signatureSummary: String = ""
)

data class InstalledAppItem(
    val appName: String,
    val packageName: String,
    val versionName: String,
    val sourceDir: String,
    val apkSize: Long,
    val isSystemApp: Boolean
)

data class ConversionHistoryItem(
    val id: String,
    val sourceName: String,
    val targetName: String,
    val sourceFormat: String,
    val targetFormat: String,
    val filePath: String,
    val fileSize: Long,
    val timestamp: Long,
    val isSigned: Boolean = false
)

data class SelectedFile(
    val uri: Uri,
    val name: String,
    val size: Long,
    val isApk: Boolean
)
