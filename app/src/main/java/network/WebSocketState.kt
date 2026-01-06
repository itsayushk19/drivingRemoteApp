package com.usb.drivingremote.network

sealed class WebSocketState{
    object Disconnected : WebSocketState()
    object Connecting : WebSocketState()
    object Connected : WebSocketState()
    data class Error(val reason: String) : WebSocketState()
}