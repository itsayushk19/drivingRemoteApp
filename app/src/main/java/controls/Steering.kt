package com.usb.drivingremote.controls

import kotlin.math.max
import kotlin.math.min

/**
 * Steering wheel axis
 * Range: -maxDegrees .. +maxDegrees
 */
object Steering {

    private const val DEFAULT_MAX_DEGREES = 1900f

    fun create(
        id: String = "steering",
        label: String = "Steering",
        outputAxis: String = "X",
        maxDegrees: Float = DEFAULT_MAX_DEGREES,
        initialDegrees: Float = 0f,
        inverted: Boolean = false
    ): Control {

        val normalized =
            (initialDegrees / maxDegrees)
                .coerceIn(-1f, 1f)

        return Control(
            ControlConfig(
                id = id,
                type = ControlType.AXIS,
                label = label,
                min = -1f,
                max = 1f,
                center = 0f,
                inverted = inverted,
                output = ControlOutput.Axis(outputAxis)
            ),
            ControlState.Axis(normalized)
        )
    }

    /** Set steering using degrees (-1900 .. +1900) */
    fun setDegrees(
        control: Control,
        degrees: Float,
        maxDegrees: Float = DEFAULT_MAX_DEGREES
    ) {
        require(control.config.type == ControlType.AXIS)

        val clampedDeg = degrees.coerceIn(-maxDegrees, maxDegrees)
        val normalized = clampedDeg / maxDegrees

        control.state = ControlState.Axis(
            if (control.config.inverted) -normalized else normalized
        )
    }

    /** Get steering in degrees */
    fun getDegrees(
        control: Control,
        maxDegrees: Float = DEFAULT_MAX_DEGREES
    ): Float {
        require(control.config.type == ControlType.AXIS)
        return (control.state as ControlState.Axis).value * maxDegrees
    }

    /** Get normalized value (-1..1) */
    fun getNormalized(control: Control): Float {
        require(control.config.type == ControlType.AXIS)
        return (control.state as ControlState.Axis).value
    }
}
