package com.usb.drivingremote.controls

/**
 * Latching on/off toggle
 * Example: engine, lights, ignition
 */
object Toggle {

    fun create(
        id: String,
        label: String,
        buttonIndex: Int,
        initialOn: Boolean = false
    ): Control {
        return Control(
            ControlConfig(
                id = id,
                type = ControlType.TOGGLE,
                label = label,
                output = ControlOutput.Button(buttonIndex)
            ),
            ControlState.Toggle(initialOn)
        )
    }

    fun set(control: Control, on: Boolean) {
        require(control.config.type == ControlType.TOGGLE)
        control.state = ControlState.Toggle(on)
    }

    fun toggle(control: Control) {
        require(control.config.type == ControlType.TOGGLE)
        val current = (control.state as ControlState.Toggle).on
        control.state = ControlState.Toggle(!current)
    }

    fun isOn(control: Control): Boolean {
        require(control.config.type == ControlType.TOGGLE)
        return (control.state as ControlState.Toggle).on
    }
}
