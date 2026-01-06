package com.usb.drivingremote.network

import androidx.compose.runtime.*
import kotlinx.coroutines.*
import java.util.UUID

class WebSocketManager(
    initialUrl: String = ""
) : WebSocketClient.Listener {

    /* ======================= STATE ======================= */

    var state by mutableStateOf<WebSocketState>(WebSocketState.Disconnected)
        private set

    var latencyMs by mutableStateOf<Long?>(null)
        private set

    val devices = mutableStateListOf<Device>()

    /* ======================= INTERNAL ======================= */

    private var socket: WebSocketClient? = null
    private var scope: CoroutineScope? = null
    private var currentUrl: String = initialUrl

    /* ======================= DEVICE MGMT ======================= */

    fun addOrUpdateDevice(device: Device) {
        if (devices.none { it.id == device.id }) {
            devices.add(device)
        }
    }

    private fun getDevice(url: String): Device {
        return devices.firstOrNull { it.id == url }
            ?: Device(
                id = url,
                initialName = deriveName(url)
            ).also { devices.add(it) }
    }

    /* ======================= PUBLIC API ======================= */

    fun connectTo(url: String) {
        if (state == WebSocketState.Connected && url == currentUrl) return

        // Mark all devices disconnected FIRST
        devices.forEach { it.isConnected = false }

        disconnect()

        currentUrl = url
        state = WebSocketState.Connecting

        val device = getDevice(url)
        device.isConnected = false

        socket = WebSocketClient(url, this)
        socket?.connect()

        scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
        startPingLoop()
    }


    fun disconnect() {
        scope?.cancel()
        scope = null

        socket?.disconnect()
        socket = null

        devices.forEach { it.isConnected = false }
        state = WebSocketState.Disconnected
        latencyMs = null
    }

    fun send(text: String) {
        socket?.send(text)
    }

    fun disconnectDevice(device: Device) {
        if (device.isConnected) {
            disconnect()
        }
    }

    fun reconnectDevice(device: Device) {
        connectTo(device.id)
    }


    /* ======================= PING LOOP ======================= */

    private fun startPingLoop() {
        scope?.launch {
            while (isActive) {
                delay(1000)
                socket?.sendPing()
            }
        }
    }

    /* ======================= CALLBACKS ======================= */

    override fun onConnected() {
        state = WebSocketState.Connected
        getDevice(currentUrl).isConnected = true
    }

    override fun onDisconnected() {
        state = WebSocketState.Disconnected
        latencyMs = null
        devices.forEach { it.isConnected = false }
    }

    override fun onMessage(text: String) {
        try {
            val json = org.json.JSONObject(text)
            if (json.optString("type") == "pong") {
                val t = json.getLong("t")
                socket?.handlePong(t)
            }
        } catch (_: Exception) {}
    }


    override fun onLatencyUpdate(ms: Long) {
        latencyMs = ms
        devices.firstOrNull { it.isConnected }?.lastLatencyMs = ms
    }

    override fun onError(error: String) {
        state = WebSocketState.Error(error)
    }

    /* ======================= HELPERS ======================= */

    private fun deriveName(url: String): String {
        return try {
            val host = url.removePrefix("ws://").substringBefore(":")
            "PC ($host)"
        } catch (_: Exception) {
            "Device ${UUID.randomUUID().toString().take(4)}"
        }
    }
}
