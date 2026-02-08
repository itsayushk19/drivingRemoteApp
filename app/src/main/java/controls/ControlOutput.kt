package com.usb.drivingremote.controls

sealed class ControlOutput {

    data class Axis(
        val axis: String,        // "X", "Y", "Z", "RX"
        val scale: Float = 1f
    ) : ControlOutput()

    data class Button(
        val button: Int          // button index
    ) : ControlOutput()

    data class Hat(
        val index: Int
    ) : ControlOutput()

    data class Gear(
        val gears: List<Int>     // supported gears
    ) : ControlOutput()
}
