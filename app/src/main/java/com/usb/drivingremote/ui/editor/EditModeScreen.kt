package com.usb.drivingremote.ui.editor

import android.app.Activity
import android.content.pm.ActivityInfo
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Done
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.usb.drivingremote.data.models.*
import com.usb.drivingremote.data.repository.LayoutRepository
import androidx.compose.material3.Slider as MSlider

/**
 * Edit mode screen - allows editing layout configuration.
 * Forces landscape orientation.
 */
@Composable
fun EditModeScreen(
    layoutId: String,
    layoutRepository: LayoutRepository,
    onClose: () -> Unit
) {
    // Load layout or handle null case outside of remember
    val initialLayout = layoutRepository.getLayout(layoutId)

    // Early return if layout is null
    if (initialLayout == null) {
        onClose()
        return
    }

    var layout by remember {
        mutableStateOf(initialLayout)
    }
    var selectedControl by remember { mutableStateOf<Int?>(null) }
    var showAddControlDialog by remember { mutableStateOf(false) }
    var showConfigDialog by remember { mutableStateOf(false) }

    val context = LocalContext.current
    val activity = context as? Activity

    // Force landscape orientation
    DisposableEffect(Unit) {
        activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
        onDispose {
            activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(Color.DarkGray)) {
        // Render controls with selection
        layout.controls.forEachIndexed { index, layoutControl ->
            EditableControl(
                layoutControl = layoutControl,
                isSelected = selectedControl == index,
                onSelect = {
                    selectedControl = index
                    showConfigDialog = true
                },
                onMove = { dx, dy ->
                    val controls = layout.controls.toMutableList()
                    controls[index] = layoutControl.copy(
                        x = (layoutControl.x + dx).coerceIn(0f, 1f),
                        y = (layoutControl.y + dy).coerceIn(0f, 1f)
                    )
                    layout = layout.copy(controls = controls)
                }
            )
        }

        // Top bar with save and close
        Row(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .background(Color.Black.copy(alpha = 0.7f))
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            IconButton(onClick = onClose) {
                Icon(Icons.Default.Close, "Close", tint = Color.White)
            }

            Text(
                layout.name,
                color = Color.White,
                style = MaterialTheme.typography.titleMedium
            )

            IconButton(onClick = {
                layoutRepository.updateLayout(layout)
                onClose()
            }) {
                Icon(Icons.Default.Done, "Save", tint = Color.White)
            }
        }

        // FAB for adding controls
        FloatingActionButton(
            onClick = { showAddControlDialog = true },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp)
        ) {
            Icon(Icons.Default.Add, "Add Control")
        }
    }

    // Add control dialog
    if (showAddControlDialog) {
        AddControlDialog(
            onDismiss = { showAddControlDialog = false },
            onAdd = { controlType ->
                val newControl = LayoutControl(
                    controlType = controlType,
                    x = 0.4f,
                    y = 0.4f,
                    width = 0.2f,
                    height = 0.2f,
                    config = ControlConfiguration(
                        id = "control_${System.currentTimeMillis()}",
                        label = controlType.name
                    )
                )
                layout = layout.copy(controls = layout.controls + newControl)
                showAddControlDialog = false
            }
        )
    }

    // Config dialog for selected control
    if (showConfigDialog && selectedControl != null) {
        val controlIndex = selectedControl!!
        if (controlIndex < layout.controls.size) {
            ControlConfigDialog(
                layoutControl = layout.controls[controlIndex],
                onDismiss = { showConfigDialog = false },
                onSave = { updatedControl ->
                    val controls = layout.controls.toMutableList()
                    controls[controlIndex] = updatedControl
                    layout = layout.copy(controls = controls)
                    showConfigDialog = false
                },
                onDelete = {
                    if (!layout.isBuiltIn) {
                        val controls = layout.controls.toMutableList()
                        controls.removeAt(controlIndex)
                        layout = layout.copy(controls = controls)
                    }
                    showConfigDialog = false
                }
            )
        }
    }
}

@Composable
private fun EditableControl(
    layoutControl: LayoutControl,
    isSelected: Boolean,
    onSelect: () -> Unit,
    onMove: (Float, Float) -> Unit
) {
    val density = LocalDensity.current

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val screenWidth = with(density) { maxWidth.toPx() }
        val screenHeight = with(density) { maxHeight.toPx() }

        val x = layoutControl.x * screenWidth
        val y = layoutControl.y * screenHeight
        val width = layoutControl.width * screenWidth
        val height = layoutControl.height * screenHeight

        Box(
            modifier = Modifier
                .offset(
                    x = with(density) { x.toDp() },
                    y = with(density) { y.toDp() }
                )
                .size(
                    width = with(density) { width.toDp() },
                    height = with(density) { height.toDp() }
                )
                .border(
                    width = if (isSelected) 3.dp else 1.dp,
                    color = if (isSelected) Color.Cyan else Color.Gray,
                    shape = RoundedCornerShape(8.dp)
                )
                .background(
                    Color.White.copy(alpha = 0.1f),
                    shape = RoundedCornerShape(8.dp)
                )
                .pointerInput(Unit) {
                    detectDragGestures { change, dragAmount ->
                        change.consume()
                        val dx = dragAmount.x / screenWidth
                        val dy = dragAmount.y / screenHeight
                        onMove(dx, dy)
                    }
                },
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = layoutControl.config.label.ifEmpty { layoutControl.controlType.name },
                color = Color.White,
                style = MaterialTheme.typography.labelSmall
            )

            // Tap to configure
            Surface(
                onClick = onSelect,
                modifier = Modifier.fillMaxSize(),
                color = Color.Transparent
            ) {}
        }
    }
}

@Composable
private fun AddControlDialog(
    onDismiss: () -> Unit,
    onAdd: (ControlKind) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Control") },
        text = {
            Column {
                ControlKind.values().forEach { type ->
                    TextButton(
                        onClick = { onAdd(type) },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(type.name)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
private fun ControlConfigDialog(
    layoutControl: LayoutControl,
    onDismiss: () -> Unit,
    onSave: (LayoutControl) -> Unit,
    onDelete: () -> Unit
) {
    var config by remember { mutableStateOf(layoutControl.config) }
    var width by remember { mutableStateOf(layoutControl.width) }
    var height by remember { mutableStateOf(layoutControl.height) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            tonalElevation = 8.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Text(
                    "Configure Control",
                    style = MaterialTheme.typography.titleMedium
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Label
                OutlinedTextField(
                    value = config.label,
                    onValueChange = { config = config.copy(label = it) },
                    label = { Text("Label") },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Output axis
                OutlinedTextField(
                    value = config.outputAxis,
                    onValueChange = { config = config.copy(outputAxis = it) },
                    label = { Text("Output Axis (X, Y, Z, RX, RY, RZ, Slider1, Slider2)") },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Width
                Text("Width: ${(width * 100).toInt()}%")
                MSlider(
                    value = width,
                    onValueChange = { width = it },
                    valueRange = 0.05f..1f
                )

                // Height
                Text("Height: ${(height * 100).toInt()}%")
                MSlider(
                    value = height,
                    onValueChange = { height = it },
                    valueRange = 0.05f..1f
                )

                // Deadzone
                Text("Deadzone: ${(config.deadzone * 100).toInt()}%")
                MSlider(
                    value = config.deadzone,
                    onValueChange = { config = config.copy(deadzone = it) },
                    valueRange = 0f..0.5f
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    TextButton(onClick = onDelete) {
                        Text("Delete", color = MaterialTheme.colorScheme.error)
                    }

                    Row {
                        TextButton(onClick = onDismiss) {
                            Text("Cancel")
                        }
                        Button(onClick = {
                            onSave(
                                layoutControl.copy(
                                    config = config,
                                    width = width,
                                    height = height
                                )
                            )
                        }) {
                            Text("Save")
                        }
                    }
                }
            }
        }
    }
}