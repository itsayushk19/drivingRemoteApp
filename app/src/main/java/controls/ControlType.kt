package com.usb.drivingremote.controls

enum class ControlType {
    AXIS,        // steering, throttle, brake
    BUTTON,      // momentary button
    TOGGLE,      // on/off switch
    HAT,         // d-pad / POV
    SHIFTER,     // gear selector
    SENSOR       // gyro, accel
}
