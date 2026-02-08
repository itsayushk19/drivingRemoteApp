package com.usb.drivingremote.ui.controls

import androidx.compose.foundation.Image
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.usb.drivingremote.R
import com.usb.drivingremote.controls.Control
import com.usb.drivingremote.controls.Steering
import kotlin.math.atan2

@Composable
fun SteeringWheel(
    control: Control,
    maxDegrees: Float = 1900f,
    sizeDp: Int = 280,
    onChange: () -> Unit
) {
    var rotationDeg by remember {
        mutableStateOf(Steering.getDegrees(control))
    }

    var lastAngle by remember { mutableStateOf<Float?>(null) }

    Box(
        modifier = Modifier
            .size(sizeDp.dp)
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragEnd = { lastAngle = null },
                    onDragCancel = { lastAngle = null }
                ) { change, _ ->

                    val center = Offset(
                        sizeDp.dp.toPx() / 2f,
                        sizeDp.dp.toPx() / 2f
                    )

                    val angle = Math.toDegrees(
                        atan2(
                            (change.position.y - center.y).toDouble(),
                            (change.position.x - center.x).toDouble()
                        )
                    ).toFloat()

                    lastAngle?.let { prev ->
                        var delta = angle - prev

                        // 🔑 FIX: unwrap angle jump
                        if (delta > 180f) delta -= 360f
                        if (delta < -180f) delta += 360f

                        // optional micro-deadzone
                        if (kotlin.math.abs(delta) > 0.1f) {
                            rotationDeg =
                                (rotationDeg + delta)
                                    .coerceIn(-maxDegrees, maxDegrees)

                            Steering.setDegrees(control, rotationDeg, maxDegrees)
                            onChange()
                        }
                    }

                    lastAngle = angle
                }
            }
    ) {
        Image(
            painter = painterResource(R.drawable.steering_wheel),
            contentDescription = "Steering Wheel",
            modifier = Modifier
                .matchParentSize()
                .rotate(rotationDeg)
        )
    }
}
