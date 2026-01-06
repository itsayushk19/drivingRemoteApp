package com.usb.drivingremote.network

import androidx.compose.runtime.*

class Device(
    val id: String,                // ws://ip:port/ws
    initialName: String            // discovered name
) {
    var name by mutableStateOf(initialName)
    var isConnected by mutableStateOf(false)
    var lastLatencyMs by mutableStateOf<Long?>(null)
}
