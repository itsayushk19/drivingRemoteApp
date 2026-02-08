package com.usb.drivingremote.controls

fun serializeControls(controls: List<Control>): Map<String, Any> {
    val out = mutableMapOf<String, Any>()

    for (control in controls) {
        when (val state = control.state) {
            is ControlState.Axis ->
                out[control.config.id] = state.value

            is ControlState.Button ->
                out[control.config.id] = state.pressed

            is ControlState.Toggle ->
                out[control.config.id] = state.on

            is ControlState.Hat ->
                out[control.config.id] = state.direction

            is ControlState.Shifter ->
                out[control.config.id] = state.gear
        }
    }

    return out
}

fun toJson(value: Any?): String {
    return when (value) {
        null -> "null"
        is String -> "\"${value.replace("\"", "\\\"")}\""
        is Number, is Boolean -> value.toString()
        is Map<*, *> -> value.entries.joinToString(
            prefix = "{",
            postfix = "}"
        ) { (k, v) ->
            "\"$k\":${toJson(v)}"
        }
        is Iterable<*> -> value.joinToString(
            prefix = "[",
            postfix = "]"
        ) { toJson(it) }
        else -> "\"$value\""
    }
}

