package com.usb.drivingremote.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.usb.drivingremote.ui.theme.BorderSoft
import com.usb.drivingremote.ui.theme.CardDark

@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .background(CardDark.copy(alpha = 0.85f))
            .border(
                width = 1.dp,
                color = BorderSoft,
                shape = RoundedCornerShape(20.dp)
            )
            .padding(16.dp)
    ) {
        content()
    }
}
