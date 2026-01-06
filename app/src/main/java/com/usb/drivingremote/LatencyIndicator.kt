package com.usb.drivingremote.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.usb.drivingremote.ui.theme.*

@Composable
fun LatencyIndicator(latencyMs: Long) {
    val (bars, color) = when {
        latencyMs < 20 -> 3 to GreenGood
        latencyMs < 50 -> 2 to YellowWarn
        else -> 1 to RedBad
    }

    Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {

        Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
            repeat(3) { index ->
                Box(
                    modifier = Modifier
                        .width(4.dp)
                        .height((8 + index * 6).dp)
                        .background(
                            if (index < bars) color else Color.DarkGray,
                            shape = androidx.compose.foundation.shape.RoundedCornerShape(2.dp)
                        )
                )
            }
        }

        Spacer(Modifier.width(8.dp))

        Text(
            text = "${latencyMs} ms",
            fontSize = 12.sp,
            color = TextSecondary
        )
    }
}
