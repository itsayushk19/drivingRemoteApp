package com.usb.drivingremote.data.models

/**
 * Represents a single control within a layout.
 *
 * @property controlType Type of control (STEERING, SLIDER, BUTTON, etc.)
 * @property x Horizontal position (0.0 to 1.0, percentage of screen width)
 * @property y Vertical position (0.0 to 1.0, percentage of screen height)
 * @property width Control width (0.0 to 1.0, percentage of screen width)
 * @property height Control height (0.0 to 1.0, percentage of screen height)
 * @property config Control-specific configuration
 */
data class LayoutControl(
    val controlType: ControlKind,
    val x: Float,
    val y: Float,
    val width: Float,
    val height: Float,
    val config: ControlConfiguration
)

/**
 * Control type enumeration for layout management.
 */
enum class ControlKind {
    STEERING,
    SLIDER,
    BUTTON_HOLD,
    BUTTON_TOGGLE,
    H_SHIFTER
}

/**
 * Configuration specific to each control type.
 *
 * @property id Unique identifier for this control instance
 * @property label Display label
 * @property outputAxis Output axis mapping ("X", "Y", "Z", "RX", "RY", "RZ", "Slider1", "Slider2")
 * @property deadzone Deadzone percentage (0.0 to 0.5)
 * @property min Minimum output value
 * @property max Maximum output value
 * @property curve Response curve type
 * @property buttonIndex Button index for button controls
 * @property sliderOrientation Orientation for slider controls (VERTICAL, HORIZONTAL)
 * @property shifterType Type of H-shifter pattern
 */
data class ControlConfiguration(
    val id: String,
    val label: String = "",
    val outputAxis: String = "X",
    val deadzone: Float = 0.05f,
    val min: Float = -1f,
    val max: Float = 1f,
    val curve: ResponseCurve = ResponseCurve.LINEAR,
    val buttonIndex: Int? = null,
    val sliderOrientation: SliderOrientation = SliderOrientation.VERTICAL,
    val shifterType: ShifterType = ShifterType.STANDARD_6_SPEED
)

/**
 * Response curve for axis inputs.
 */
enum class ResponseCurve {
    LINEAR,
    QUADRATIC,
    CUBIC,
    EXPONENTIAL
}

/**
 * Slider orientation.
 */
enum class SliderOrientation {
    VERTICAL,
    HORIZONTAL
}

/**
 * H-Shifter pattern types.
 */
enum class ShifterType {
    STANDARD_6_SPEED,
    H_PATTERN_5_SPEED,
    EATON_10_SPEED,
    EATON_13_SPEED,
    EATON_18_SPEED
}
