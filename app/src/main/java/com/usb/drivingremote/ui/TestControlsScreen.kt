package com.usb.drivingremote.ui

import android.view.MotionEvent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInteropFilter
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.usb.drivingremote.controls.*
import com.usb.drivingremote.ui.controls.SteeringWheel
import kotlin.math.roundToInt
import androidx.compose.material3.Slider as MSlider


@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun TestControlsScreen(
    controlManager: ControlManager,
    onClose: () -> Unit
) {

    /* ================= CONTROLS ================= */

    val throttle = remember {
        Axis.create(
            id = "throttle",
            label = "Throttle",
            outputAxis = "Y",
            min = 0f,
            max = 1f
        )
    }

    val steering = remember {
        Axis.create(
            id = "steering",
            label = "Steering",
            outputAxis = "X",
            min = -1f,
            max = 1f,
            center = 0f
        )
    }

    val horn = remember {
        ButtonControl.create(
            id = "horn",
            label = "Horn",
            buttonIndex = 1
        )
    }

    val engine = remember {
        Toggle.create(
            id = "engine",
            label = "Engine",
            buttonIndex = 5
        )
    }

    /* ================= REGISTER ONCE ================= */

    LaunchedEffect(Unit) {
        controlManager.register(throttle)
        controlManager.register(steering)
        controlManager.register(horn)
        controlManager.register(engine)
        controlManager.sendDescriptor()
    }

    /* ================= UI ================= */

    Surface(
        modifier = Modifier.fillMaxSize(),
        tonalElevation = 4.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {

            Text(
                text = "TEST CONTROLS",
                style = MaterialTheme.typography.titleLarge
            )

            /* ---------- STEERING ---------- */

            val steeringDegrees =
                (Axis.get(steering) * 1900f).roundToInt()

            Text(
                text = "Steering: $steeringDegrees°",
                fontSize = 14.sp
            )

            SteeringWheel(
                control = steering,
                maxDegrees = 1900f,
                onChange = {
                    controlManager.markDirty()
                }
            )

            Divider()

            /* ---------- THROTTLE ---------- */

            Text(
                text = "Throttle: ${(Axis.get(throttle) * 100).roundToInt()}%"
            )

            MSlider(
                value = Axis.get(throttle),
                onValueChange = {
                    Axis.set(throttle, it)
                    controlManager.markDirty()
                },
                valueRange = 0f..1f
            )


            Divider()

            /* ---------- HORN (MOMENTARY) ---------- */

            Button(
                onClick = {},
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .pointerInteropFilter { event ->
                        when (event.action) {
                            MotionEvent.ACTION_DOWN -> {
                                ButtonControl.press(horn)
                                controlManager.markDirty()
                                true
                            }
                            MotionEvent.ACTION_UP,
                            MotionEvent.ACTION_CANCEL -> {
                                ButtonControl.release(horn)
                                controlManager.markDirty()
                                true
                            }
                            else -> false
                        }
                    }
            ) {
                Text("Horn (Hold)")
            }

            Divider()

            /* ---------- ENGINE TOGGLE ---------- */

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text("Engine")
                Switch(
                    checked = Toggle.isOn(engine),
                    onCheckedChange = {
                        Toggle.set(engine, it)
                        controlManager.markDirty()
                    }
                )
            }

            Spacer(modifier = Modifier.weight(1f))

            OutlinedButton(
                onClick = onClose,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Close Test Screen")
            }
        }
    }
}
