package com.usb.drivingremote.ui.layouts

import android.app.Activity
import android.content.pm.ActivityInfo
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.FileOpen
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.usb.drivingremote.data.models.ControllerLayout
import com.usb.drivingremote.data.repository.LayoutRepository
import com.usb.drivingremote.ui.components.GlassCard
import java.text.SimpleDateFormat
import java.util.*

/**
 * Main layout management screen.
 * Shows a list of available controller layouts with options to open or edit them.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LayoutListScreen(
    layoutRepository: LayoutRepository,
    onOpenLayout: (String) -> Unit,
    onEditLayout: (String) -> Unit,
    onImportLayout: () -> Unit,
    onExportLayout: (String, String) -> Unit,  // Added export callback
    modifier: Modifier = Modifier
) {
    val layouts = remember { mutableStateOf(layoutRepository.getAllLayouts()) }
    var selectedLayout by remember { mutableStateOf<ControllerLayout?>(null) }
    var showOptionsDialog by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    
    // Refresh layouts list
    LaunchedEffect(Unit) {
        layouts.value = layoutRepository.getAllLayouts()
    }
    
    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text("Controller Layouts") },
                actions = {
                    IconButton(onClick = onImportLayout) {
                        Icon(Icons.Default.FileOpen, contentDescription = "Import Layout")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    // Create new custom layout
                    val newLayout = layoutRepository.createCustomLayout("New Layout")
                    layouts.value = layoutRepository.getAllLayouts()
                    onEditLayout(newLayout.id)
                }
            ) {
                Icon(Icons.Default.Add, contentDescription = "New Layout")
            }
        }
    ) { paddingValues ->
        if (layouts.value.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "No layouts available",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(layouts.value, key = { it.id }) { layout ->
                    LayoutCard(
                        layout = layout,
                        onLongPress = {
                            selectedLayout = layout
                            showOptionsDialog = true
                        },
                        onClick = {
                            onOpenLayout(layout.id)
                        }
                    )
                }
            }
        }
    }
    
    // Options dialog (shown on long press)
    if (showOptionsDialog && selectedLayout != null) {
        AlertDialog(
            onDismissRequest = { showOptionsDialog = false },
            title = { Text(selectedLayout!!.name) },
            text = {
                Column {
                    TextButton(
                        onClick = {
                            showOptionsDialog = false
                            onOpenLayout(selectedLayout!!.id)
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Open")
                    }
                    
                    TextButton(
                        onClick = {
                            showOptionsDialog = false
                            onEditLayout(selectedLayout!!.id)
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Edit")
                    }
                    
                    TextButton(
                        onClick = {
                            showOptionsDialog = false
                            val fileName = "${selectedLayout!!.name.replace(" ", "_")}.dr"
                            onExportLayout(selectedLayout!!.id, fileName)
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Export")
                    }
                    
                    if (!selectedLayout!!.isBuiltIn) {
                        TextButton(
                            onClick = {
                                showOptionsDialog = false
                                showDeleteDialog = true
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Delete", color = MaterialTheme.colorScheme.error)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showOptionsDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
    
    // Delete confirmation dialog
    if (showDeleteDialog && selectedLayout != null) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Delete Layout") },
            text = { Text("Are you sure you want to delete \"${selectedLayout!!.name}\"?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        layoutRepository.deleteLayout(selectedLayout!!.id)
                        layouts.value = layoutRepository.getAllLayouts()
                        showDeleteDialog = false
                        selectedLayout = null
                    }
                ) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

/**
 * Card displaying a single layout.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun LayoutCard(
    layout: ControllerLayout,
    onLongPress: () -> Unit,
    onClick: () -> Unit
) {
    val context = LocalContext.current
    val activity = context as? Activity
    
    GlassCard(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = {
                    // Force landscape orientation before opening layout
                    try {
                        activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
                    } catch (e: Exception) {
                        android.util.Log.e("LayoutCard", "Error setting landscape orientation", e)
                    }
                    onClick()
                },
                onLongClick = onLongPress
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = layout.name,
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                
                if (layout.isBuiltIn) {
                    Surface(
                        shape = MaterialTheme.shapes.small,
                        color = MaterialTheme.colorScheme.primaryContainer
                    ) {
                        Text(
                            text = "Built-in",
                            style = MaterialTheme.typography.labelSmall,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = "${layout.controls.size} controls",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            Text(
                text = "Last modified: ${formatDate(layout.lastModified)}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = "Long press for options",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
            )
        }
    }
}

private fun formatDate(timestamp: Long): String {
    val sdf = SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault())
    return sdf.format(Date(timestamp))
}
