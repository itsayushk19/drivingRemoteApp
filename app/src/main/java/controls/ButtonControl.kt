package com.usb.drivingremote.controls

/**
 * Momentary button (pressed / released)
 * Example: horn, starter, indicator
 */
object ButtonControl {

    fun create(
        id: String,
        label: String,
        buttonIndex: Int,
        initialPressed: Boolean = false
    ): Control {
        return Control(
            ControlConfig(
                id = id,
                type = ControlType.BUTTON,
                label = label,
                output = ControlOutput.Button(buttonIndex)
            ),
            ControlState.Button(initialPressed)
        )
    }

    fun press(control: Control) {
        require(control.config.type == ControlType.BUTTON)
        control.state = ControlState.Button(true)
    }

    fun release(control: Control) {
        require(control.config.type == ControlType.BUTTON)
        control.state = ControlState.Button(false)
    }

    fun isPressed(control: Control): Boolean {
        require(control.config.type == ControlType.BUTTON)
        return (control.state as ControlState.Button).pressed
    }
}
