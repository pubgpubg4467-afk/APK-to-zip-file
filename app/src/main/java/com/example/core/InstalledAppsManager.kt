package com.example.core

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import com.example.model.InstalledAppItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

object InstalledAppsManager {

    suspend fun getInstalledApps(context: Context): List<InstalledAppItem> = withContext(Dispatchers.IO) {
        val result = mutableListOf<InstalledAppItem>()
        val pm = context.packageManager
        val packages: List<PackageInfo> = try {
            pm.getInstalledPackages(0)
        } catch (e: Exception) {
            emptyList()
        }

        for (pkg in packages) {
            val appInfo = pkg.applicationInfo ?: continue
            val apkFile = File(appInfo.sourceDir ?: continue)
            if (!apkFile.exists()) continue

            val appName = try {
                pm.getApplicationLabel(appInfo).toString()
            } catch (e: Exception) {
                pkg.packageName
            }

            val isSystem = (appInfo.flags and ApplicationInfo.FLAG_SYSTEM) != 0
            val versionName = pkg.versionName ?: "1.0"

            result.add(
                InstalledAppItem(
                    appName = appName,
                    packageName = pkg.packageName,
                    versionName = versionName,
                    sourceDir = appInfo.sourceDir,
                    apkSize = apkFile.length(),
                    isSystemApp = isSystem
                )
            )
        }

        // Sort: user apps first, then alphabetically by app name
        result.sortedWith(
            compareBy<InstalledAppItem> { it.isSystemApp }
                .thenBy { it.appName.lowercase() }
        )
    }
}
