package com.usb.drivingremote.ui.controls

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.usb.drivingremote.controls.Control
import com.usb.drivingremote.controls.HShifter
import kotlin.math.abs

/**
 * H-Shifter UI component for manual transmission control.
 * Displays a gear pattern and detects touch to select gears.
 */
@Composable
fun HShifterView(
    control: Control,
    modifier: Modifier = Modifier,
    onChange: () -> Unit
) {
    val shifterType = HShifter.getShifterType(control)
    val currentGear = HShifter.getGear(control)
    
    val gearPattern = remember(shifterType) {
        when (shifterType) {
            "STANDARD_6_SPEED" -> createStandard6SpeedPattern()
            "H_PATTERN_5_SPEED" -> createHPattern5SpeedPattern()
            "EATON_10_SPEED" -> createEaton10SpeedPattern()
            "EATON_13_SPEED" -> createEaton13SpeedPattern()
            "EATON_18_SPEED" -> createEaton18SpeedPattern()
            else -> createStandard6SpeedPattern()
        }
    }
    
    Box(
        modifier = modifier
            .background(Color.Black.copy(alpha = 0.8f))
            .pointerInput(Unit) {
                detectTapGestures { offset ->
                    val x = offset.x / size.width
                    val y = offset.y / size.height
                    
                    // Find closest gear position
                    val selectedGear = gearPattern.positions.minByOrNull { (_, pos) ->
                        val dx = pos.x - x
                        val dy = pos.y - y
                        dx * dx + dy * dy
                    }?.key ?: 0
                    
                    HShifter.setGear(control, selectedGear)
                    onChange()
                }
            }
    ) {
        ShifterPattern(
            pattern = gearPattern,
            currentGear = currentGear,
            modifier = Modifier.fillMaxSize()
        )
    }
}

/**
 * Render the shifter pattern with gates and labels.
 */
@Composable
private fun ShifterPattern(
    pattern: GearPattern,
    currentGear: Int,
    modifier: Modifier = Modifier
) {
    val textMeasurer = rememberTextMeasurer()
    
    Canvas(modifier = modifier) {
        val width = size.width
        val height = size.height
        
        // Draw gates/rails
        pattern.gates.forEach { gate ->
            val startX = gate.start.x * width
            val startY = gate.start.y * height
            val endX = gate.end.x * width
            val endY = gate.end.y * height
            
            drawLine(
                color = Color.Gray,
                start = Offset(startX, startY),
                end = Offset(endX, endY),
                strokeWidth = 3.dp.toPx()
            )
        }
        
        // Draw gear positions
        pattern.positions.forEach { (gear, position) ->
            val x = position.x * width
            val y = position.y * height
            val isSelected = gear == currentGear
            
            // Draw gear circle
            drawCircle(
                color = if (isSelected) Color.Cyan else Color.White,
                radius = if (isSelected) 25.dp.toPx() else 20.dp.toPx(),
                center = Offset(x, y)
            )
            
            drawCircle(
                color = Color.Black,
                radius = if (isSelected) 22.dp.toPx() else 17.dp.toPx(),
                center = Offset(x, y)
            )
            
            // Draw gear label
            val label = when (gear) {
                -1 -> "R"
                0 -> "N"
                else -> gear.toString()
            }
            
            val textLayoutResult = textMeasurer.measure(
                text = label,
                style = TextStyle(
                    color = if (isSelected) Color.Cyan else Color.White,
                    fontSize = 16.sp
                )
            )
            
            drawText(
                textLayoutResult = textLayoutResult,
                topLeft = Offset(
                    x - textLayoutResult.size.width / 2,
                    y - textLayoutResult.size.height / 2
                )
            )
        }
    }
}

/**
 * Data class representing a gear pattern.
 */
data class GearPattern(
    val positions: Map<Int, GearPosition>,  // gear -> position
    val gates: List<Gate>                   // visual rails/gates
)

data class GearPosition(
    val x: Float,  // 0.0 to 1.0
    val y: Float   // 0.0 to 1.0
)

data class Gate(
    val start: GearPosition,
    val end: GearPosition
)

/**
 * Create standard 6-speed H-pattern (car transmission).
 * Layout:
 *   1  3  5
 *   2  4  6
 *   R in top-left or bottom-left
 */
private fun createStandard6SpeedPattern(): GearPattern {
    val positions = mapOf(
        -1 to GearPosition(0.15f, 0.15f),  // Reverse
        0 to GearPosition(0.5f, 0.5f),     // Neutral (center)
        1 to GearPosition(0.25f, 0.25f),
        2 to GearPosition(0.25f, 0.75f),
        3 to GearPosition(0.5f, 0.25f),
        4 to GearPosition(0.5f, 0.75f),
        5 to GearPosition(0.75f, 0.25f),
        6 to GearPosition(0.75f, 0.75f)
    )
    
    val gates = listOf(
        // Vertical gates
        Gate(GearPosition(0.25f, 0.2f), GearPosition(0.25f, 0.8f)),
        Gate(GearPosition(0.5f, 0.2f), GearPosition(0.5f, 0.8f)),
        Gate(GearPosition(0.75f, 0.2f), GearPosition(0.75f, 0.8f)),
        // Horizontal connector
        Gate(GearPosition(0.2f, 0.5f), GearPosition(0.8f, 0.5f))
    )
    
    return GearPattern(positions, gates)
}

/**
 * Create 5-speed H-pattern.
 */
private fun createHPattern5SpeedPattern(): GearPattern {
    val positions = mapOf(
        -1 to GearPosition(0.15f, 0.15f),
        0 to GearPosition(0.5f, 0.5f),
        1 to GearPosition(0.3f, 0.25f),
        2 to GearPosition(0.3f, 0.75f),
        3 to GearPosition(0.5f, 0.25f),
        4 to GearPosition(0.5f, 0.75f),
        5 to GearPosition(0.7f, 0.5f)
    )
    
    val gates = listOf(
        Gate(GearPosition(0.3f, 0.2f), GearPosition(0.3f, 0.8f)),
        Gate(GearPosition(0.5f, 0.2f), GearPosition(0.5f, 0.8f)),
        Gate(GearPosition(0.7f, 0.3f), GearPosition(0.7f, 0.7f)),
        Gate(GearPosition(0.25f, 0.5f), GearPosition(0.75f, 0.5f))
    )
    
    return GearPattern(positions, gates)
}

/**
 * Create Eaton Fuller 10-speed pattern.
 * 2x5 layout with splitter for low/high range.
 */
private fun createEaton10SpeedPattern(): GearPattern {
    val positions = mapOf(
        -1 to GearPosition(0.1f, 0.1f),   // Reverse
        0 to GearPosition(0.5f, 0.5f),    // Neutral
        1 to GearPosition(0.2f, 0.3f),    // 1L
        2 to GearPosition(0.2f, 0.7f),    // 2L
        3 to GearPosition(0.35f, 0.3f),   // 3L
        4 to GearPosition(0.35f, 0.7f),   // 4L
        5 to GearPosition(0.5f, 0.3f),    // 5L
        6 to GearPosition(0.5f, 0.7f),    // 1H
        7 to GearPosition(0.65f, 0.3f),   // 2H
        8 to GearPosition(0.65f, 0.7f),   // 3H
        9 to GearPosition(0.8f, 0.3f),    // 4H
        10 to GearPosition(0.8f, 0.7f)    // 5H
    )
    
    val gates = listOf(
        Gate(GearPosition(0.2f, 0.25f), GearPosition(0.2f, 0.75f)),
        Gate(GearPosition(0.35f, 0.25f), GearPosition(0.35f, 0.75f)),
        Gate(GearPosition(0.5f, 0.25f), GearPosition(0.5f, 0.75f)),
        Gate(GearPosition(0.65f, 0.25f), GearPosition(0.65f, 0.75f)),
        Gate(GearPosition(0.8f, 0.25f), GearPosition(0.8f, 0.75f)),
        Gate(GearPosition(0.15f, 0.5f), GearPosition(0.85f, 0.5f))
    )
    
    return GearPattern(positions, gates)
}

/**
 * Create Eaton Fuller 13-speed pattern.
 */
private fun createEaton13SpeedPattern(): GearPattern {
    // Similar to 10-speed but with additional gears
    val positions = mutableMapOf(
        -1 to GearPosition(0.1f, 0.1f),
        0 to GearPosition(0.5f, 0.5f)
    )
    
    // Add 13 forward gears in a 2x6.5 pattern
    for (i in 1..13) {
        val row = (i - 1) % 2
        val col = (i - 1) / 2
        positions[i] = GearPosition(
            0.15f + col * 0.13f,
            0.3f + row * 0.4f
        )
    }
    
    val gates = listOf(
        Gate(GearPosition(0.15f, 0.25f), GearPosition(0.15f, 0.75f)),
        Gate(GearPosition(0.1f, 0.5f), GearPosition(0.9f, 0.5f))
    )
    
    return GearPattern(positions, gates)
}

/**
 * Create Eaton Fuller 18-speed pattern.
 */
private fun createEaton18SpeedPattern(): GearPattern {
    val positions = mutableMapOf(
        -1 to GearPosition(0.1f, 0.1f),
        0 to GearPosition(0.5f, 0.5f)
    )
    
    // Add 18 forward gears
    for (i in 1..18) {
        val row = (i - 1) % 2
        val col = (i - 1) / 2
        positions[i] = GearPosition(
            0.1f + col * 0.09f,
            0.3f + row * 0.4f
        )
    }
    
    val gates = listOf(
        Gate(GearPosition(0.1f, 0.25f), GearPosition(0.9f, 0.25f)),
        Gate(GearPosition(0.1f, 0.5f), GearPosition(0.9f, 0.5f)),
        Gate(GearPosition(0.1f, 0.75f), GearPosition(0.9f, 0.75f))
    )
    
    return GearPattern(positions, gates)
}
