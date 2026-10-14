package com.example.ui.screens

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Android
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.FolderZip
import androidx.compose.material.icons.filled.InstallMobile
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.ConversionMode
import com.example.ui.MainViewModel
import com.example.ui.components.FormatBadge
import com.example.ui.components.InfoBadgeChip
import com.example.ui.components.ProgressBarWithStatus
import com.example.ui.components.formatFileSize
import com.example.ui.theme.CyanPrimaryDark
import com.example.ui.theme.EmeraldSuccessDark
import com.example.ui.theme.OrangeSecondaryDark

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ConvertScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val mode by viewModel.conversionMode.collectAsState()
    val selectedFile by viewModel.selectedFile.collectAsState()
    val apkDetails by viewModel.apkDetails.collectAsState()
    val isConverting by viewModel.isConverting.collectAsState()
    val progress by viewModel.conversionProgress.collectAsState()
    val statusMessage by viewModel.statusMessage.collectAsState()
    val resultFile by viewModel.conversionResult.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()

    val stripSignatures by viewModel.stripSignatures.collectAsState()
    val autoSignApk by viewModel.autoSignApk.collectAsState()

    // File Pickers
    val apkPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            val (name, size) = queryFileInfo(context, uri)
            viewModel.onFileSelected(uri, name, size)
        }
    }

    val zipPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            val (name, size) = queryFileInfo(context, uri)
            viewModel.onFileSelected(uri, name, size)
        }
    }

    // Save File Picker
    val saveFileLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument(
            if (mode == ConversionMode.APK_TO_ZIP) "application/zip" else "application/vnd.android.package-archive"
        )
    ) { destUri: Uri? ->
        if (destUri != null && resultFile != null) {
            viewModel.saveFileToExternalUri(resultFile!!, destUri)
        }
    }

    val scrollState = rememberScrollState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Mode Selector Tab Bar
        ModeSelectorCard(
            currentMode = mode,
            onModeSelected = { newMode ->
                viewModel.setConversionMode(newMode)
            }
        )

        // File Selection Section
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("file_selection_card"),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            shape = RoundedCornerShape(20.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (mode == ConversionMode.APK_TO_ZIP) "Input APK Package" else "Input ZIP Archive",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    FormatBadge(format = if (mode == ConversionMode.APK_TO_ZIP) "APK" else "ZIP")
                }

                if (selectedFile == null) {
                    // Empty dropzone button
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                            .border(
                                width = 1.dp,
                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f),
                                shape = RoundedCornerShape(16.dp)
                            )
                            .clickable {
                                if (mode == ConversionMode.APK_TO_ZIP) {
                                    apkPickerLauncher.launch(arrayOf("*/*"))
                                } else {
                                    zipPickerLauncher.launch(arrayOf("application/zip", "application/x-zip-compressed", "*/*"))
                                }
                            }
                            .padding(24.dp)
                            .testTag("select_file_touch_target"),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(52.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primaryContainer),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = if (mode == ConversionMode.APK_TO_ZIP) Icons.Default.Android else Icons.Default.FolderZip,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(28.dp)
                                )
                            }
                            Text(
                                text = if (mode == ConversionMode.APK_TO_ZIP) "Tap to Select .APK File" else "Tap to Select .ZIP File",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = if (mode == ConversionMode.APK_TO_ZIP) "Supports any standard Android package" else "Select archive to repackage as APK",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    if (mode == ConversionMode.APK_TO_ZIP) {
                        OutlinedButton(
                            onClick = { viewModel.setTab(1) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("pick_from_installed_apps_button"),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.Android, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Or Pick From Installed Apps")
                        }
                    }
                } else {
                    // Selected file details
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = if (selectedFile!!.isApk) Icons.Default.Android else Icons.Default.FolderZip,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = selectedFile!!.name,
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = formatFileSize(selectedFile!!.size),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            OutlinedButton(
                                onClick = {
                                    if (mode == ConversionMode.APK_TO_ZIP) {
                                        apkPickerLauncher.launch(arrayOf("*/*"))
                                    } else {
                                        zipPickerLauncher.launch(arrayOf("*/*"))
                                    }
                                },
                                modifier = Modifier.testTag("change_file_button")
                            ) {
                                Text("Change")
                            }
                        }

                        // Detailed APK info if available
                        if (apkDetails != null) {
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Package Analysis",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            FlowRow(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                InfoBadgeChip(label = "Package", value = apkDetails!!.packageName)
                                InfoBadgeChip(label = "Version", value = "${apkDetails!!.versionName} (${apkDetails!!.versionCode})")
                                InfoBadgeChip(label = "SDK", value = "Min ${apkDetails!!.minSdk} / Target ${apkDetails!!.targetSdk}")
                                InfoBadgeChip(label = "DEX Files", value = "${apkDetails!!.dexCount}")
                                InfoBadgeChip(label = "Entries", value = "${apkDetails!!.totalEntries}")
                                InfoBadgeChip(label = "Signature", value = apkDetails!!.signatureSummary)
                            }
                        }
                    }
                }
            }
        }

        // Conversion Settings / Options Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(20.dp)
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text(
                    text = "Conversion Options",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                if (mode == ConversionMode.APK_TO_ZIP) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Strip Old Signatures (META-INF)",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "Removes signature certs for clean editing or modding",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = stripSignatures,
                            onCheckedChange = { viewModel.setStripSignatures(it) },
                            modifier = Modifier.testTag("strip_signatures_switch")
                        )
                    }
                } else {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Sign APK with TestKey (Recommended)",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "Required by Android OS to allow installing the APK",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = autoSignApk,
                            onCheckedChange = { viewModel.setAutoSignApk(it) },
                            modifier = Modifier.testTag("auto_sign_apk_switch")
                        )
                    }
                }
            }
        }

        // Start Conversion Action Button
        Button(
            onClick = { viewModel.startConversion() },
            enabled = selectedFile != null && !isConverting,
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp)
                .testTag("convert_action_button"),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary
            )
        ) {
            Icon(
                imageVector = Icons.Default.SwapHoriz,
                contentDescription = null,
                modifier = Modifier.size(22.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = if (mode == ConversionMode.APK_TO_ZIP) "Convert APK to ZIP" else "Convert ZIP to APK",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
        }

        // Live Conversion Progress
        AnimatedVisibility(
            visible = isConverting,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            ProgressBarWithStatus(
                progress = progress,
                statusText = statusMessage
            )
        }

        // Error message if any
        if (errorMessage != null) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                shape = RoundedCornerShape(14.dp)
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(
                        Icons.Default.Warning,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error
                    )
                    Text(
                        text = errorMessage!!,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                }
            }
        }

        // Success Result Card
        AnimatedVisibility(
            visible = resultFile != null && !isConverting,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            if (resultFile != null) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("conversion_success_card"),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    ),
                    shape = RoundedCornerShape(20.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(CircleShape)
                                    .background(EmeraldSuccessDark.copy(alpha = 0.2f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = EmeraldSuccessDark
                                )
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Ready: ${resultFile!!.name}",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = "${formatFileSize(resultFile!!.length())} • ${statusMessage}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        // Action Buttons Row
                        FlowRow(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            // Inspect
                            FilledTonalButton(
                                onClick = {
                                    viewModel.openInInspector(Uri.fromFile(resultFile!!), resultFile!!.name)
                                },
                                modifier = Modifier.testTag("result_inspect_button")
                            ) {
                                Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Inspect")
                            }

                            // Share
                            FilledTonalButton(
                                onClick = { viewModel.shareFile(resultFile!!) },
                                modifier = Modifier.testTag("result_share_button")
                            ) {
                                Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Share")
                            }

                            // Install (if APK)
                            if (resultFile!!.name.endsWith(".apk", ignoreCase = true)) {
                                Button(
                                    onClick = { viewModel.installApk(resultFile!!) },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = EmeraldSuccessDark
                                    ),
                                    modifier = Modifier.testTag("result_install_button")
                                ) {
                                    Icon(Icons.Default.InstallMobile, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Install APK")
                                }
                            }

                            // Save to Storage
                            Button(
                                onClick = {
                                    saveFileLauncher.launch(resultFile!!.name)
                                },
                                modifier = Modifier.testTag("result_save_button")
                            ) {
                                Icon(Icons.Default.CloudDownload, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Save to Storage")
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ModeSelectorCard(
    currentMode: ConversionMode,
    onModeSelected: (ConversionMode) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(6.dp)
        ) {
            val isApkToZip = currentMode == ConversionMode.APK_TO_ZIP
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(12.dp))
                    .background(if (isApkToZip) MaterialTheme.colorScheme.surface else Color.Transparent)
                    .clickable { onModeSelected(ConversionMode.APK_TO_ZIP) }
                    .padding(vertical = 12.dp)
                    .testTag("mode_apk_to_zip_tab"),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        Icons.Default.Android,
                        contentDescription = null,
                        tint = if (isApkToZip) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = "APK → ZIP",
                        fontWeight = if (isApkToZip) FontWeight.Bold else FontWeight.Medium,
                        color = if (isApkToZip) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(12.dp))
                    .background(if (!isApkToZip) MaterialTheme.colorScheme.surface else Color.Transparent)
                    .clickable { onModeSelected(ConversionMode.ZIP_TO_APK) }
                    .padding(vertical = 12.dp)
                    .testTag("mode_zip_to_apk_tab"),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        Icons.Default.FolderZip,
                        contentDescription = null,
                        tint = if (!isApkToZip) OrangeSecondaryDark else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = "ZIP → APK",
                        fontWeight = if (!isApkToZip) FontWeight.Bold else FontWeight.Medium,
                        color = if (!isApkToZip) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

private fun queryFileInfo(context: Context, uri: Uri): Pair<String, Long> {
    var name = "archive_file"
    var size = 0L
    try {
        context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
            if (cursor.moveToFirst()) {
                if (nameIndex != -1) name = cursor.getString(nameIndex)
                if (sizeIndex != -1) size = cursor.getLong(sizeIndex)
            }
        }
    } catch (e: Exception) {
        e.printStackTrace()
    }
    return Pair(name, size)
}
