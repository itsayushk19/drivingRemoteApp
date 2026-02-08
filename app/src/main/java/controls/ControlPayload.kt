package com.usb.drivingremote.controls

data class ControlPayload(
    val type: String = "control",
    val timestamp: Long,
    val controls: Map<String, Any>
)
