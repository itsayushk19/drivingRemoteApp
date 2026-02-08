package com.usb.drivingremote.ui.playmode

import android.app.Activity
import android.content.pm.ActivityInfo
import android.view.MotionEvent
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInteropFilter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.usb.drivingremote.controls.*
import com.usb.drivingremote.data.models.*
import com.usb.drivingremote.data.repository.LayoutRepository
import com.usb.drivingremote.ui.controls.SteeringWheel
import kotlinx.coroutines.delay
import androidx.compose.material3.Slider as MSlider

/**
 * Play mode screen - displays and operates controls from a layout.
 * Forces landscape orientation.
 */
@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun PlayModeScreen(
    layoutId: String,
    layoutRepository: LayoutRepository,
    controlManager: ControlManager,
    onClose: () -> Unit
) {
    val layout = remember { layoutRepository.getLayout(layoutId) }
    val context = LocalContext.current
    val activity = context as? Activity
    
    // Back button handling with "press again to exit"
    var backPressedOnce by remember { mutableStateOf(false) }
    
    BackHandler {
        if (backPressedOnce) {
            onClose()
        } else {
            backPressedOnce = true
            Toast.makeText(context, "Press back again to exit", Toast.LENGTH_SHORT).show()
            
            // Reset after 2 seconds
            LaunchedEffect(Unit) {
                delay(2000)
                backPressedOnce = false
            }
        }
    }
    
    // Force landscape orientation
    DisposableEffect(Unit) {
        activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
        onDispose {
            activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        }
    }
    
    // Register controls with ControlManager
    val controls = remember {
        layout?.controls?.mapNotNull { layoutControl ->
            createControlFromLayout(layoutControl)
        } ?: emptyList()
    }
    
    LaunchedEffect(controls) {
        controls.forEach { control ->
            controlManager.register(control)
        }
        controlManager.sendDescriptor()
    }
    
    DisposableEffect(Unit) {
        onDispose {
            controls.forEach { control ->
                controlManager.unregister(control.config.id)
            }
        }
    }
    
    if (layout == null) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Layout not found")
                Button(onClick = onClose) {
                    Text("Go Back")
                }
            }
        }
        return
    }
    
    Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
        // Render controls
        layout.controls.forEachIndexed { index, layoutControl ->
            val control = controls.getOrNull(index)
            if (control != null) {
                RenderControl(
                    layoutControl = layoutControl,
                    control = control,
                    onUpdate = { controlManager.markDirty() }
                )
            }
        }
        
        // Close button
        IconButton(
            onClick = onClose,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(16.dp)
        ) {
            Icon(
                Icons.Default.Close,
                contentDescription = "Close",
                tint = Color.White
            )
        }
    }
}

/**
 * Render a single control based on its type.
 */
@OptIn(ExperimentalComposeUiApi::class)
@Composable
private fun RenderControl(
    layoutControl: LayoutControl,
    control: Control,
    onUpdate: () -> Unit
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
        ) {
            when (layoutControl.controlType) {
                ControlKind.STEERING -> {
                    SteeringWheel(
                        control = control,
                        maxDegrees = 1900f,
                        sizeDp = with(density) { width.coerceAtMost(height).toInt() },
                        onChange = onUpdate
                    )
                }
                
                ControlKind.SLIDER -> {
                    if (layoutControl.config.sliderOrientation == SliderOrientation.VERTICAL) {
                        VerticalSlider(
                            control = control,
                            onChange = onUpdate
                        )
                    } else {
                        HorizontalSlider(
                            control = control,
                            onChange = onUpdate
                        )
                    }
                }
                
                ControlKind.BUTTON_HOLD -> {
                    HoldButton(
                        control = control,
                        label = layoutControl.config.label,
                        onChange = onUpdate
                    )
                }
                
                ControlKind.BUTTON_TOGGLE -> {
                    ToggleButton(
                        control = control,
                        label = layoutControl.config.label,
                        onChange = onUpdate
                    )
                }
                
                ControlKind.H_SHIFTER -> {
                    com.usb.drivingremote.ui.controls.HShifterView(
                        control = control,
                        modifier = Modifier.fillMaxSize(),
                        onChange = onUpdate
                    )
                }
            }
        }
    }
}

@Composable
private fun VerticalSlider(
    control: Control,
    onChange: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        MSlider(
            value = Axis.get(control),
            onValueChange = {
                Axis.set(control, it)
                onChange()
            },
            modifier = Modifier
                .fillMaxHeight()
                .width(60.dp),
            valueRange = control.config.min..control.config.max
        )
    }
}

@Composable
private fun HorizontalSlider(
    control: Control,
    onChange: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxSize(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        MSlider(
            value = Axis.get(control),
            onValueChange = {
                Axis.set(control, it)
                onChange()
            },
            modifier = Modifier.fillMaxWidth(),
            valueRange = control.config.min..control.config.max
        )
    }
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
private fun HoldButton(
    control: Control,
    label: String,
    onChange: () -> Unit
) {
    Button(
        onClick = {},
        modifier = Modifier
            .fillMaxSize()
            .pointerInteropFilter { event ->
                when (event.action) {
                    MotionEvent.ACTION_DOWN -> {
                        ButtonControl.press(control)
                        onChange()
                        true
                    }
                    MotionEvent.ACTION_UP,
                    MotionEvent.ACTION_CANCEL -> {
                        ButtonControl.release(control)
                        onChange()
                        true
                    }
                    else -> false
                }
            }
    ) {
        Text(label)
    }
}

@Composable
private fun ToggleButton(
    control: Control,
    label: String,
    onChange: () -> Unit
) {
    Button(
        onClick = {
            Toggle.toggle(control)
            onChange()
        },
        modifier = Modifier.fillMaxSize(),
        colors = if (Toggle.isOn(control)) {
            ButtonDefaults.buttonColors()
        } else {
            ButtonDefaults.outlinedButtonColors()
        }
    ) {
        Text(label)
    }
}

/**
 * Create a Control instance from a LayoutControl.
 */
private fun createControlFromLayout(layoutControl: LayoutControl): Control? {
    val config = layoutControl.config
    
    return when (layoutControl.controlType) {
        ControlKind.STEERING -> {
            Steering.create(
                id = config.id,
                label = config.label,
                outputAxis = config.outputAxis,
                maxDegrees = 1900f
            )
        }
        
        ControlKind.SLIDER -> {
            Axis.create(
                id = config.id,
                label = config.label,
                outputAxis = config.outputAxis,
                min = config.min,
                max = config.max
            )
        }
        
        ControlKind.BUTTON_HOLD -> {
            config.buttonIndex?.let { index ->
                ButtonControl.create(
                    id = config.id,
                    label = config.label,
                    buttonIndex = index
                )
            }
        }
        
        ControlKind.BUTTON_TOGGLE -> {
            config.buttonIndex?.let { index ->
                Toggle.create(
                    id = config.id,
                    label = config.label,
                    buttonIndex = index
                )
            }
        }
        
        ControlKind.H_SHIFTER -> {
            HShifter.create(
                id = config.id,
                label = config.label,
                shifterType = config.shifterType.name,
                initialGear = 0  // Start in neutral
            )
        }
    }
}
