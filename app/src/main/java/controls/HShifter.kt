package com.usb.drivingremote.controls

/**
 * H-Shifter control for manual transmission.
 * Supports different truck shifter patterns (Eaton Fuller, etc.)
 */
object HShifter {
    
    fun create(
        id: String,
        label: String,
        shifterType: String = "STANDARD_6_SPEED",
        initialGear: Int = 0  // 0 = neutral
    ): Control {
        return Control(
            ControlConfig(
                id = id,
                type = ControlType.SHIFTER,
                label = label,
                min = -1f,  // Reverse gear
                max = 18f,  // Max 18 forward gears (Eaton 18-speed)
                center = 0f,  // Neutral
                inverted = false,
                output = ControlOutput.Axis("Shifter")
            ),
            ControlState.Shifter(initialGear, shifterType)
        )
    }
    
    fun setGear(control: Control, gear: Int) {
        require(control.config.type == ControlType.SHIFTER)
        val currentState = control.state as ControlState.Shifter
        control.state = ControlState.Shifter(gear, currentState.shifterType)
    }
    
    fun getGear(control: Control): Int {
        require(control.config.type == ControlType.SHIFTER)
        return (control.state as ControlState.Shifter).currentGear
    }
    
    fun getShifterType(control: Control): String {
        require(control.config.type == ControlType.SHIFTER)
        return (control.state as ControlState.Shifter).shifterType
    }
}
