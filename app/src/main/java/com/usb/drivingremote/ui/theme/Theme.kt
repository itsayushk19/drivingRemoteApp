package com.usb.drivingremote.ui.theme

import androidx.compose.material3.*
import androidx.compose.runtime.Composable

private val DarkScheme = darkColorScheme(
    background = BgDark,
    surface = CardDark,
    primary = GreenGood,
    onPrimary = BgDark,
    onSurface = TextPrimary,
    onSurfaceVariant = TextSecondary
)

@Composable
fun DrivingRemoteTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = DarkScheme,
        typography = Typography(),
        content = content
    )
}
