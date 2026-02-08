package com.usb.drivingremote.ui.editor

import android.app.Activity
import android.content.pm.ActivityInfo
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.Edit
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
import kotlinx.coroutines.delay
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
    var showDeleteLayoutDialog by remember { mutableStateOf(false) }
    
    // Position for draggable pencil button
    var pencilButtonX by remember { mutableStateOf(0.85f) }
    var pencilButtonY by remember { mutableStateOf(0.1f) }

    val context = LocalContext.current
    val activity = context as? Activity
    
    // Back button handling with "press again to exit"
    var backPressedOnce by remember { mutableStateOf(false) }
    
    BackHandler {
        if (backPressedOnce) {
            // Save and close
            layoutRepository.updateLayout(layout)
            onClose()
        } else {
            backPressedOnce = true
            Toast.makeText(context, "Press back again to save and exit", Toast.LENGTH_SHORT).show()
            
            // Reset after 2 seconds
            LaunchedEffect(Unit) {
                delay(2000)
                backPressedOnce = false
            }
        }
    }

    // Force landscape orientation
    DisposableEffect(Unit) {
        try {
            activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
        } catch (e: Exception) {
            android.util.Log.e("EditModeScreen", "Error setting orientation", e)
        }
        onDispose {
            try {
                activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
            } catch (e: Exception) {
                android.util.Log.e("EditModeScreen", "Error resetting orientation", e)
            }
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

        // Draggable pencil icon button for adding controls
        val density = LocalDensity.current
        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val screenWidth = with(density) { maxWidth.toPx() }
            val screenHeight = with(density) { maxHeight.toPx() }
            
            FloatingActionButton(
                onClick = { showAddControlDialog = true },
                modifier = Modifier
                    .offset(
                        x = with(density) { (pencilButtonX * screenWidth).toDp() },
                        y = with(density) { (pencilButtonY * screenHeight).toDp() }
                    )
                    .size(56.dp)
                    .pointerInput(Unit) {
                        detectDragGestures { change, dragAmount ->
                            change.consume()
                            pencilButtonX = (pencilButtonX + dragAmount.x / screenWidth).coerceIn(0f, 0.9f)
                            pencilButtonY = (pencilButtonY + dragAmount.y / screenHeight).coerceIn(0f, 0.9f)
                        }
                    },
                containerColor = MaterialTheme.colorScheme.primary
            ) {
                Icon(Icons.Default.Edit, "Add Control", tint = MaterialTheme.colorScheme.onPrimary)
            }
        }
        
        // Close and save buttons in corners
        IconButton(
            onClick = onClose,
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(16.dp)
        ) {
            Icon(Icons.Default.Close, "Close", tint = MaterialTheme.colorScheme.onSurface)
        }
        
        IconButton(
            onClick = {
                layoutRepository.updateLayout(layout)
                onClose()
            },
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(16.dp)
        ) {
            Icon(Icons.Default.Done, "Save", tint = MaterialTheme.colorScheme.onSurface)
        }
    }

    // Add control dialog
    if (showAddControlDialog) {
        AddControlDialog(
            onDismiss = { showAddControlDialog = false },
            onAdd = { controlType ->
                // Set appropriate default size based on control type
                val (defaultWidth, defaultHeight) = when (controlType) {
                    ControlKind.SLIDER -> Pair(0.12f, 0.8f)  // Tall vertical slider
                    ControlKind.STEERING -> Pair(0.3f, 0.3f)  // Square steering wheel
                    ControlKind.H_SHIFTER -> Pair(0.25f, 0.35f)  // Rectangular shifter
                    else -> Pair(0.2f, 0.2f)  // Default for buttons
                }
                
                val newControl = LayoutControl(
                    controlType = controlType,
                    x = 0.4f,
                    y = 0.1f,  // Start near top for vertical sliders
                    width = defaultWidth,
                    height = defaultHeight,
                    config = ControlConfiguration(
                        id = "control_${System.currentTimeMillis()}",
                        label = controlType.name
                    )
                )
                layout = layout.copy(controls = layout.controls + newControl)
                showAddControlDialog = false
            },
            onDeleteLayout = {
                showAddControlDialog = false
                showDeleteLayoutDialog = true
            }
        )
    }
    
    // Delete layout confirmation dialog
    if (showDeleteLayoutDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteLayoutDialog = false },
            title = { Text("Delete Layout") },
            text = { Text("Are you sure you want to delete \"${layout.name}\"? This action cannot be undone.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (!layout.isBuiltIn) {
                            layoutRepository.deleteLayout(layout.id)
                        }
                        showDeleteLayoutDialog = false
                        onClose()
                    }
                ) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteLayoutDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
                val newControl = LayoutControl(
                    controlType = controlType,
                    x = 0.4f,
                    y = 0.1f,  // Start near top for vertical sliders
                    width = defaultWidth,
                    height = defaultHeight,
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
                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                    shape = RoundedCornerShape(8.dp)
                )
                .background(
                    MaterialTheme.colorScheme.surface.copy(alpha = 0.1f),
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
            // Render control preview based on type
            when (layoutControl.controlType) {
                ControlKind.STEERING -> {
                    // Show a circular preview for steering wheel
                    Box(
                        modifier = Modifier
                            .fillMaxSize(0.9f)
                            .background(
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.3f),
                                shape = androidx.compose.foundation.shape.CircleShape
                            )
                            .border(
                                2.dp,
                                MaterialTheme.colorScheme.primary,
                                shape = androidx.compose.foundation.shape.CircleShape
                            )
                    )
                }
                ControlKind.SLIDER -> {
                    // Show vertical or horizontal bar based on orientation
                    if (layoutControl.config.sliderOrientation == SliderOrientation.VERTICAL) {
                        Box(
                            modifier = Modifier
                                .fillMaxHeight(0.9f)
                                .width(40.dp)
                                .background(
                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.3f),
                                    shape = RoundedCornerShape(20.dp)
                                )
                                .border(
                                    2.dp,
                                    MaterialTheme.colorScheme.primary,
                                    shape = RoundedCornerShape(20.dp)
                                )
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(0.9f)
                                .height(40.dp)
                                .background(
                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.3f),
                                    shape = RoundedCornerShape(20.dp)
                                )
                                .border(
                                    2.dp,
                                    MaterialTheme.colorScheme.primary,
                                    shape = RoundedCornerShape(20.dp)
                                )
                        )
                    }
                }
                ControlKind.BUTTON_HOLD, ControlKind.BUTTON_TOGGLE -> {
                    // Show button preview
                    Box(
                        modifier = Modifier
                            .fillMaxSize(0.9f)
                            .background(
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.3f),
                                shape = RoundedCornerShape(8.dp)
                            )
                            .border(
                                2.dp,
                                MaterialTheme.colorScheme.primary,
                                shape = RoundedCornerShape(8.dp)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = layoutControl.config.label.ifEmpty { "BTN" },
                            color = MaterialTheme.colorScheme.onSurface,
                            style = MaterialTheme.typography.labelMedium
                        )
                    }
                }
                ControlKind.H_SHIFTER -> {
                    // Show H-shifter grid preview
                    Box(
                        modifier = Modifier
                            .fillMaxSize(0.9f)
                            .background(
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.3f),
                                shape = RoundedCornerShape(8.dp)
                            )
                            .border(
                                2.dp,
                                MaterialTheme.colorScheme.primary,
                                shape = RoundedCornerShape(8.dp)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "H",
                            color = MaterialTheme.colorScheme.onSurface,
                            style = MaterialTheme.typography.titleLarge
                        )
                    }
                }
            }
            
            // Label overlay
            Text(
                text = layoutControl.config.label.ifEmpty { layoutControl.controlType.name },
                color = MaterialTheme.colorScheme.onSurface,
                style = MaterialTheme.typography.labelSmall,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(4.dp)
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
    onAdd: (ControlKind) -> Unit,
    onDeleteLayout: () -> Unit = {}
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
                
                Divider(modifier = Modifier.padding(vertical = 8.dp))
                
                // Delete Layout button
                TextButton(
                    onClick = onDeleteLayout,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Delete Layout", color = MaterialTheme.colorScheme.error)
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