package com.usb.drivingremote.controls

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

class Control(
    val config: ControlConfig,
    initialState: ControlState
) {
    var state by mutableStateOf(initialState)
}
