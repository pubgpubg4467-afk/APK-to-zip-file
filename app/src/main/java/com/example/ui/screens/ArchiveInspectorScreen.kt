package com.example.ui.screens

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.FolderZip
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.TextSnippet
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.ArchiveEntryItem
import com.example.ui.MainViewModel
import com.example.ui.components.EmptyPlaceholder
import com.example.ui.components.InfoBadgeChip
import com.example.ui.components.formatFileSize

@Composable
fun ArchiveInspectorScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val currentUri by viewModel.inspectorFileUri.collectAsState()
    val currentName by viewModel.inspectorFileName.collectAsState()
    val entries by viewModel.inspectorEntries.collectAsState()
    val isLoading by viewModel.isLoadingInspector.collectAsState()
    val searchQuery by viewModel.inspectorSearch.collectAsState()
    val previewContent by viewModel.previewEntryContent.collectAsState()

    var activeCategoryFilter by remember { mutableStateOf("All") }

    val filePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            val name = queryDisplayName(context, uri)
            viewModel.openInInspector(uri, name)
        }
    }

    val filteredEntries = entries.filter { entry ->
        val matchesSearch = searchQuery.isEmpty() || entry.name.contains(searchQuery, ignoreCase = true)
        val matchesCategory = when (activeCategoryFilter) {
            "DEX" -> entry.name.endsWith(".dex", ignoreCase = true)
            "XML" -> entry.name.endsWith(".xml", ignoreCase = true)
            "Images" -> entry.name.endsWith(".png", true) || entry.name.endsWith(".webp", true) || entry.name.endsWith(".jpg", true)
            "META-INF" -> entry.name.startsWith("META-INF/")
            "assets" -> entry.name.startsWith("assets/")
            "res" -> entry.name.startsWith("res/")
            else -> true
        }
        matchesSearch && matchesCategory
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Top Card: Select Archive or Loaded Archive Info
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(18.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (currentUri != null) currentName else "Archive Explorer",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = if (currentUri != null) "${entries.size} total entries" else "Inspect files inside any APK or ZIP",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Button(
                        onClick = { filePicker.launch(arrayOf("*/*")) },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.testTag("inspector_pick_file_button")
                    ) {
                        Text(if (currentUri != null) "Open Other" else "Pick Archive")
                    }
                }

                if (entries.isNotEmpty()) {
                    val totalUncompressed = entries.sumOf { if (it.size > 0) it.size else 0L }
                    val totalCompressed = entries.sumOf { if (it.compressedSize > 0) it.compressedSize else 0L }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        InfoBadgeChip(label = "Raw Size", value = formatFileSize(totalUncompressed))
                        InfoBadgeChip(label = "Pack Size", value = formatFileSize(totalCompressed))
                        val savings = if (totalUncompressed > 0) {
                            ((1.0 - (totalCompressed.toDouble() / totalUncompressed)) * 100).coerceAtLeast(0.0).toInt()
                        } else 0
                        InfoBadgeChip(label = "Saved", value = "$savings%")
                    }
                }
            }
        }

        if (currentUri == null) {
            EmptyPlaceholder(
                icon = Icons.Default.FolderZip,
                title = "No Archive Loaded",
                subtitle = "Pick an APK or ZIP file or use 'Inspect' from Convert or Installed Apps"
            )
        } else {
            // Search Input
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { viewModel.setInspectorSearch(it) },
                placeholder = { Text("Filter entries (e.g. classes.dex, .png)...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { viewModel.setInspectorSearch("") }) {
                            Icon(Icons.Default.Clear, contentDescription = "Clear")
                        }
                    }
                },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("inspector_search_input"),
                shape = RoundedCornerShape(14.dp)
            )

            // Category Filter Chips
            val categories = listOf("All", "DEX", "XML", "Images", "META-INF", "assets", "res")
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                items(categories) { cat ->
                    FilterChip(
                        selected = activeCategoryFilter == cat,
                        onClick = { activeCategoryFilter = cat },
                        label = { Text(cat) },
                        modifier = Modifier.testTag("inspector_filter_${cat.lowercase()}")
                    )
                }
            }

            // Entries List
            if (isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else if (filteredEntries.isEmpty()) {
                EmptyPlaceholder(
                    icon = Icons.Default.Search,
                    title = "No Matching Entries",
                    subtitle = "No files match '$searchQuery' in category '$activeCategoryFilter'"
                )
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(filteredEntries, key = { it.name }) { entry ->
                        ArchiveEntryRow(
                            entry = entry,
                            onPreview = {
                                val isText = entry.name.endsWith(".txt", true) ||
                                        entry.name.endsWith(".json", true) ||
                                        entry.name.endsWith(".properties", true) ||
                                        entry.name.endsWith(".MF", true) ||
                                        entry.name.endsWith(".SF", true)
                                if (isText) {
                                    viewModel.previewEntry(entry.name)
                                }
                            }
                        )
                    }
                }
            }
        }
    }

    // Text File Preview Dialog
    if (previewContent != null) {
        val (name, text) = previewContent!!
        AlertDialog(
            onDismissRequest = { viewModel.closePreview() },
            title = {
                Text(
                    text = name.substringAfterLast("/"),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            },
            text = {
                val scroll = rememberScrollState()
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(300.dp)
                        .verticalScroll(scroll)
                        .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(8.dp))
                        .padding(12.dp)
                ) {
                    Text(
                        text = text,
                        style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { viewModel.closePreview() }) {
                    Text("Close")
                }
            }
        )
    }
}

@Composable
fun ArchiveEntryRow(
    entry: ArchiveEntryItem,
    onPreview: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isPreviewable = entry.name.endsWith(".txt", true) ||
            entry.name.endsWith(".json", true) ||
            entry.name.endsWith(".properties", true) ||
            entry.name.endsWith(".MF", true) ||
            entry.name.endsWith(".SF", true)

    val icon: ImageVector = when {
        entry.isDirectory -> Icons.Default.Folder
        entry.name.endsWith(".dex", true) -> Icons.Default.Code
        entry.name.endsWith(".xml", true) -> Icons.Default.Description
        entry.name.endsWith(".png", true) || entry.name.endsWith(".webp", true) || entry.name.endsWith(".jpg", true) -> Icons.Default.Image
        entry.name.startsWith("META-INF/") -> Icons.Default.Lock
        isPreviewable -> Icons.Default.TextSnippet
        else -> Icons.Default.FolderZip
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(enabled = isPreviewable) { onPreview() }
            .testTag("entry_item_${entry.name}"),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.5.dp)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = entry.name,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = formatFileSize(entry.size),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (entry.compressedSize > 0 && entry.size > entry.compressedSize) {
                        Text(
                            text = "Compressed: ${formatFileSize(entry.compressedSize)}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    if (isPreviewable) {
                        Text(
                            text = "• Tap to view",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.secondary
                        )
                    }
                }
            }
        }
    }
}

private fun queryDisplayName(context: Context, uri: Uri): String {
    var name = "archive"
    try {
        context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            val index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (cursor.moveToFirst() && index != -1) {
                name = cursor.getString(index)
            }
        }
    } catch (e: Exception) {
        e.printStackTrace()
    }
    return name
}
