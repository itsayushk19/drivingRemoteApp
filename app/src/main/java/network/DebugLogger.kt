package com.usb.drivingremote.network

import android.util.Log

object DebugLogger {

    private const val TAG = "DrivingRemote"

    fun logControls(
        state: WebSocketState,
        latencyMs: Long?,
        controls: Map<String, Any>
    ) {
        val body = buildString {
            append("{\n")
            controls.forEach { (k, v) ->
                append("  \"$k\": $v,\n")
            }
            if (controls.isNotEmpty()) delete(length - 2, length)
            append("\n}")
        }

        Log.i(
            TAG,
            """
            ┌──────────── CONTROLS ────────────
            │ State : ${state::class.simpleName}
            │ RTT   : ${latencyMs ?: "--"} ms
            │ Data  :
            │ $body
            └──────────────────────────────────
            """.trimIndent()
        )
    }
}
