package com.example.ui

import android.app.Application
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.core.ApkParser
import com.example.core.ApkToZipEngine
import com.example.core.ArchiveInspectorEngine
import com.example.core.InstalledAppsManager
import com.example.core.ZipToApkEngine
import com.example.data.HistoryRepository
import com.example.model.ApkDetails
import com.example.model.ArchiveEntryItem
import com.example.model.ConversionHistoryItem
import com.example.model.ConversionMode
import com.example.model.InstalledAppItem
import com.example.model.SelectedFile
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.UUID

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val context: Context get() = getApplication()
    val historyRepo = HistoryRepository(context)

    // Current Navigation Tab: 0=Convert, 1=Apps, 2=Inspector, 3=History, 4=Guide
    private val _currentTab = MutableStateFlow(0)
    val currentTab: StateFlow<Int> = _currentTab.asStateFlow()

    fun setTab(index: Int) {
        _currentTab.value = index
    }

    // Active Mode in Convert Tab
    private val _conversionMode = MutableStateFlow(ConversionMode.APK_TO_ZIP)
    val conversionMode: StateFlow<ConversionMode> = _conversionMode.asStateFlow()

    fun setConversionMode(mode: ConversionMode) {
        _conversionMode.value = mode
        _selectedFile.value = null
        _apkDetails.value = null
        _conversionResult.value = null
        _errorMessage.value = null
    }

    // Selected input file
    private val _selectedFile = MutableStateFlow<SelectedFile?>(null)
    val selectedFile: StateFlow<SelectedFile?> = _selectedFile.asStateFlow()

    // Analyzed APK details if selected file is an APK
    private val _apkDetails = MutableStateFlow<ApkDetails?>(null)
    val apkDetails: StateFlow<ApkDetails?> = _apkDetails.asStateFlow()

    // Options
    private val _stripSignatures = MutableStateFlow(false)
    val stripSignatures: StateFlow<Boolean> = _stripSignatures.asStateFlow()

    fun setStripSignatures(value: Boolean) {
        _stripSignatures.value = value
    }

    private val _autoSignApk = MutableStateFlow(true)
    val autoSignApk: StateFlow<Boolean> = _autoSignApk.asStateFlow()

    fun setAutoSignApk(value: Boolean) {
        _autoSignApk.value = value
    }

    // Progress and state
    private val _isConverting = MutableStateFlow(false)
    val isConverting: StateFlow<Boolean> = _isConverting.asStateFlow()

    private val _conversionProgress = MutableStateFlow(0f)
    val conversionProgress: StateFlow<Float> = _conversionProgress.asStateFlow()

    private val _statusMessage = MutableStateFlow("")
    val statusMessage: StateFlow<String> = _statusMessage.asStateFlow()

    private val _conversionResult = MutableStateFlow<File?>(null)
    val conversionResult: StateFlow<File?> = _conversionResult.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    // Installed Apps Tab State
    private val _installedApps = MutableStateFlow<List<InstalledAppItem>>(emptyList())
    val installedApps: StateFlow<List<InstalledAppItem>> = _installedApps.asStateFlow()

    private val _isLoadingApps = MutableStateFlow(false)
    val isLoadingApps: StateFlow<Boolean> = _isLoadingApps.asStateFlow()

    private val _appsSearchQuery = MutableStateFlow("")
    val appsSearchQuery: StateFlow<String> = _appsSearchQuery.asStateFlow()

    private val _appsFilterSystem = MutableStateFlow(false) // false = all/user, true = system
    val appsFilterSystem: StateFlow<Boolean> = _appsFilterSystem.asStateFlow()

    // Inspector Tab State
    private val _inspectorFileUri = MutableStateFlow<Uri?>(null)
    val inspectorFileUri: StateFlow<Uri?> = _inspectorFileUri.asStateFlow()

    private val _inspectorFileName = MutableStateFlow("")
    val inspectorFileName: StateFlow<String> = _inspectorFileName.asStateFlow()

    private val _inspectorEntries = MutableStateFlow<List<ArchiveEntryItem>>(emptyList())
    val inspectorEntries: StateFlow<List<ArchiveEntryItem>> = _inspectorEntries.asStateFlow()

    private val _inspectorSearch = MutableStateFlow("")
    val inspectorSearch: StateFlow<String> = _inspectorSearch.asStateFlow()

    private val _isLoadingInspector = MutableStateFlow(false)
    val isLoadingInspector: StateFlow<Boolean> = _isLoadingInspector.asStateFlow()

    private val _previewEntryContent = MutableStateFlow<Pair<String, String>?>(null) // name to content
    val previewEntryContent: StateFlow<Pair<String, String>?> = _previewEntryContent.asStateFlow()

    init {
        loadInstalledApps()
    }

    fun onFileSelected(uri: Uri, fileName: String, fileSize: Long) {
        val isApk = fileName.endsWith(".apk", ignoreCase = true)
        _selectedFile.value = SelectedFile(uri, fileName, fileSize, isApk)
        _conversionResult.value = null
        _errorMessage.value = null

        if (isApk) {
            // Analyze APK
            viewModelScope.launch {
                try {
                    // Copy to temp to parse with package manager
                    val temp = File(context.cacheDir, "temp_parse.apk")
                    context.contentResolver.openInputStream(uri)?.use { input ->
                        FileOutputStream(temp).use { output ->
                            input.copyTo(output)
                        }
                    }
                    val details = ApkParser.parseApk(context, temp)
                    _apkDetails.value = details
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        } else {
            _apkDetails.value = null
        }
    }

    fun startConversion() {
        val file = _selectedFile.value ?: return
        if (_isConverting.value) return

        _isConverting.value = true
        _conversionProgress.value = 0.05f
        _statusMessage.value = "Starting conversion..."
        _conversionResult.value = null
        _errorMessage.value = null

        viewModelScope.launch {
            if (_conversionMode.value == ConversionMode.APK_TO_ZIP) {
                val targetName = "${file.name.substringBeforeLast(".")}.zip"
                val result = ApkToZipEngine.convertApkToZip(
                    context = context,
                    inputUri = file.uri,
                    outputFileName = targetName,
                    options = ApkToZipEngine.ConversionOptions(stripSignatures = _stripSignatures.value),
                    onProgress = { progress, msg ->
                        _conversionProgress.value = progress
                        _statusMessage.value = msg
                    }
                )

                _isConverting.value = false
                if (result.success && result.outputFile != null) {
                    _conversionResult.value = result.outputFile
                    _statusMessage.value = "Converted to ZIP successfully! (${result.totalEntries} files)"
                    historyRepo.addHistoryItem(
                        ConversionHistoryItem(
                            id = UUID.randomUUID().toString(),
                            sourceName = file.name,
                            targetName = targetName,
                            sourceFormat = "APK",
                            targetFormat = "ZIP",
                            filePath = result.outputFile.absolutePath,
                            fileSize = result.outputSizeBytes,
                            timestamp = System.currentTimeMillis(),
                            isSigned = false
                        )
                    )
                } else {
                    _errorMessage.value = result.errorMessage ?: "Conversion failed"
                }
            } else {
                // ZIP to APK
                val targetName = "${file.name.substringBeforeLast(".")}.apk"
                val result = ZipToApkEngine.convertZipToApk(
                    context = context,
                    inputUri = file.uri,
                    outputFileName = targetName,
                    options = ZipToApkEngine.PackagingOptions(autoSign = _autoSignApk.value),
                    onProgress = { progress, msg ->
                        _conversionProgress.value = progress
                        _statusMessage.value = msg
                    }
                )

                _isConverting.value = false
                if (result.success && result.outputFile != null) {
                    _conversionResult.value = result.outputFile
                    val statusText = if (result.isSigned) "APK created & signed with testkey!" else "APK created (Unsigned)"
                    _statusMessage.value = statusText
                    if (result.warningMessage != null) {
                        _errorMessage.value = result.warningMessage
                    }
                    historyRepo.addHistoryItem(
                        ConversionHistoryItem(
                            id = UUID.randomUUID().toString(),
                            sourceName = file.name,
                            targetName = targetName,
                            sourceFormat = "ZIP",
                            targetFormat = "APK",
                            filePath = result.outputFile.absolutePath,
                            fileSize = result.outputSizeBytes,
                            timestamp = System.currentTimeMillis(),
                            isSigned = result.isSigned
                        )
                    )
                } else {
                    _errorMessage.value = result.errorMessage ?: "Failed to build APK"
                }
            }
        }
    }

    fun loadInstalledApps() {
        viewModelScope.launch {
            _isLoadingApps.value = true
            _installedApps.value = InstalledAppsManager.getInstalledApps(context)
            _isLoadingApps.value = false
        }
    }

    fun setAppsSearchQuery(q: String) {
        _appsSearchQuery.value = q
    }

    fun setAppsFilterSystem(showSystem: Boolean) {
        _appsFilterSystem.value = showSystem
    }

    fun convertInstalledApp(app: InstalledAppItem, toZip: Boolean) {
        val apkFile = File(app.sourceDir)
        if (!apkFile.exists()) return

        val uri = Uri.fromFile(apkFile)
        _conversionMode.value = if (toZip) ConversionMode.APK_TO_ZIP else ConversionMode.APK_TO_ZIP
        onFileSelected(uri, "${app.appName}.apk", apkFile.length())
        _currentTab.value = 0 // Navigate to convert tab

        if (toZip) {
            startConversion()
        }
    }

    fun openInInspector(uri: Uri, name: String) {
        _inspectorFileUri.value = uri
        _inspectorFileName.value = name
        _currentTab.value = 2 // Inspector Tab
        loadArchiveEntries(uri)
    }

    fun setInspectorSearch(q: String) {
        _inspectorSearch.value = q
    }

    fun loadArchiveEntries(uri: Uri) {
        viewModelScope.launch {
            _isLoadingInspector.value = true
            _inspectorEntries.value = ArchiveInspectorEngine.getArchiveEntries(context, uri)
            _isLoadingInspector.value = false
        }
    }

    fun previewEntry(entryName: String) {
        val uri = _inspectorFileUri.value ?: return
        viewModelScope.launch {
            val content = ArchiveInspectorEngine.readEntryTextPreview(context, uri, entryName)
            _previewEntryContent.value = Pair(entryName, content)
        }
    }

    fun closePreview() {
        _previewEntryContent.value = null
    }

    fun shareFile(file: File) {
        try {
            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = if (file.name.endsWith(".apk")) "application/vnd.android.package-archive" else "application/zip"
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(Intent.createChooser(intent, "Share ${file.name}").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun installApk(file: File) {
        try {
            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "application/vnd.android.package-archive")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun saveFileToExternalUri(sourceFile: File, targetUri: Uri) {
        viewModelScope.launch {
            try {
                context.contentResolver.openOutputStream(targetUri)?.use { out ->
                    FileInputStream(sourceFile).use { input ->
                        input.copyTo(out)
                    }
                }
                _statusMessage.value = "Saved successfully to storage!"
            } catch (e: Exception) {
                e.printStackTrace()
                _errorMessage.value = "Failed to save file: ${e.message}"
            }
        }
    }
}
