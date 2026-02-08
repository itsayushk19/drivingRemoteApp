package com.usb.drivingremote.controls

/**
 * Slider is a UI helper for AXIS
 */
object Slider {

    fun set(control: Control, normalizedValue: Float) {
        Axis.set(control, normalizedValue)
    }

    fun get(control: Control): Float {
        return Axis.get(control)
    }
}
