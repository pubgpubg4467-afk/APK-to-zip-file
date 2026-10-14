package com.example.core

import android.content.Context
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.os.Build
import com.example.model.ApkDetails
import com.example.model.ArchiveEntryItem
import java.io.File
import java.io.InputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipFile
import java.util.zip.ZipInputStream

object ApkParser {

    /**
     * Parses an APK file from a local File path using PackageManager and Zip inspection.
     */
    fun parseApk(context: Context, apkFile: File): ApkDetails {
        var packageName = "Unknown"
        var appName = apkFile.nameWithoutExtension
        var versionName = "1.0"
        var versionCode = 1L
        var minSdk = 21
        var targetSdk = 34
        val permissions = mutableListOf<String>()
        var isSigned = false
        var signatureSummary = "Unsigned"

        try {
            val pm = context.packageManager
            val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                PackageManager.GET_PERMISSIONS or PackageManager.GET_SIGNING_CERTIFICATES
            } else {
                @Suppress("DEPRECATION")
                PackageManager.GET_PERMISSIONS or PackageManager.GET_SIGNATURES
            }

            val pkgInfo: PackageInfo? = pm.getPackageArchiveInfo(apkFile.absolutePath, flags)
            if (pkgInfo != null) {
                packageName = pkgInfo.packageName ?: "Unknown"
                versionName = pkgInfo.versionName ?: "1.0"
                versionCode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    pkgInfo.longVersionCode
                } else {
                    @Suppress("DEPRECATION")
                    pkgInfo.versionCode.toLong()
                }

                pkgInfo.applicationInfo?.let { appInfo ->
                    appInfo.sourceDir = apkFile.absolutePath
                    appInfo.publicSourceDir = apkFile.absolutePath
                    val label = pm.getApplicationLabel(appInfo)
                    if (!label.isNullOrEmpty()) {
                        appName = label.toString()
                    }
                    targetSdk = appInfo.targetSdkVersion
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                        minSdk = appInfo.minSdkVersion
                    }
                }

                pkgInfo.requestedPermissions?.let {
                    permissions.addAll(it)
                }

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    val signingInfo = pkgInfo.signingInfo
                    if (signingInfo != null) {
                        val certs = if (signingInfo.hasMultipleSigners()) {
                            signingInfo.apkContentsSigners
                        } else {
                            signingInfo.signingCertificateHistory
                        }
                        if (!certs.isNullOrEmpty()) {
                            isSigned = true
                            signatureSummary = "Signed (${certs.size} certificate(s))"
                        }
                    }
                } else {
                    @Suppress("DEPRECATION")
                    val sigs = pkgInfo.signatures
                    if (!sigs.isNullOrEmpty()) {
                        isSigned = true
                        signatureSummary = "Signed (${sigs.size} signature(s))"
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        // Now inspect zip entries to count DEX files and total entries
        var dexCount = 0
        var totalEntries = 0
        try {
            ZipFile(apkFile).use { zip ->
                val entries = zip.entries()
                while (entries.hasMoreElements()) {
                    val entry = entries.nextElement()
                    totalEntries++
                    if (entry.name.endsWith(".dex")) {
                        dexCount++
                    }
                    if (!isSigned && (entry.name.startsWith("META-INF/") && (entry.name.endsWith(".RSA") || entry.name.endsWith(".DSA") || entry.name.endsWith(".EC")))) {
                        isSigned = true
                        signatureSummary = "Signed (v1 signature detected)"
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        return ApkDetails(
            packageName = packageName,
            appName = appName,
            versionName = versionName,
            versionCode = versionCode,
            minSdk = minSdk,
            targetSdk = targetSdk,
            permissions = permissions,
            dexCount = dexCount,
            totalEntries = totalEntries,
            totalSize = apkFile.length(),
            isSigned = isSigned,
            signatureSummary = signatureSummary
        )
    }

    /**
     * Reads all archive entries from a stream or file.
     */
    fun listArchiveEntries(file: File): List<ArchiveEntryItem> {
        val result = mutableListOf<ArchiveEntryItem>()
        ZipFile(file).use { zip ->
            val entries = zip.entries()
            while (entries.hasMoreElements()) {
                val entry = entries.nextElement()
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
            }
        }
        return result.sortedBy { it.name }
    }
}
