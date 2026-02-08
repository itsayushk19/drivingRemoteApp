package com.usb.drivingremote.controls

/**
 * Generic continuous axis
 * Range: min .. max (normalized or raw)
 */
object Axis {

    fun create(
        id: String,
        label: String,
        outputAxis: String,
        min: Float = -1f,
        max: Float = 1f,
        initial: Float = 0f,
        center: Float? = null,
        inverted: Boolean = false
    ): Control {
        return Control(
            ControlConfig(
                id = id,
                type = ControlType.AXIS,
                label = label,
                min = min,
                max = max,
                center = center,
                inverted = inverted,
                output = ControlOutput.Axis(outputAxis)
            ),
            ControlState.Axis(initial.coerceIn(min, max))
        )
    }

    fun set(control: Control, rawValue: Float) {
        require(control.config.type == ControlType.AXIS)

        val min = control.config.min
        val max = control.config.max

        val clamped = rawValue.coerceIn(min, max)
        val value =
            if (control.config.inverted)
                max - (clamped - min)
            else
                clamped

        control.state = ControlState.Axis(value)
    }

    fun get(control: Control): Float {
        require(control.config.type == ControlType.AXIS)
        return (control.state as ControlState.Axis).value
    }
}
