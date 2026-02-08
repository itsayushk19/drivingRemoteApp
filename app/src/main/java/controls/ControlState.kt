package com.usb.drivingremote.controls

sealed class ControlState {

    data class Axis(
        val value: Float         // normalized 0..1 or -1..1
    ) : ControlState()

    data class Button(
        val pressed: Boolean
    ) : ControlState()

    data class Toggle(
        val on: Boolean
    ) : ControlState()

    data class Hat(
        val direction: Int       // 0–7 or -1
    ) : ControlState()

    data class Shifter(
        val gear: Int
    ) : ControlState()
}
