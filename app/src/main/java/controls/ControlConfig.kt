package com.usb.drivingremote.controls

data class ControlConfig(
    val id: String,                 // "steering", "throttle"
    val type: ControlType,
    val label: String,              // UI name
    val min: Float = 0f,             // for axes
    val max: Float = 1f,
    val center: Float? = null,       // steering center
    val steps: Int? = null,          // for discrete controls
    val inverted: Boolean = false,
    val output: ControlOutput
)
