package com.usb.drivingremote.controls

import com.usb.drivingremote.network.DebugLogger
import com.usb.drivingremote.network.WebSocketManager
import kotlinx.coroutines.*
import java.util.concurrent.atomic.AtomicBoolean

class ControlManager(
    private val webSocketManager: WebSocketManager
) {

    /* ---------------- STATE ---------------- */

    private val controls = mutableMapOf<String, Control>()
    private var lastControlsSnapshot: Map<String, Any> = emptyMap()

    private val sending = AtomicBoolean(false)
    private var scope: CoroutineScope? = null

    private val logIntervalMs = 1000L

    private val idMap = mutableMapOf<String, Byte>()
    private var nextId: Byte = 1


    /* ---------------- LIFECYCLE ---------------- */

    fun start() {
        if (scope != null) return

        scope = CoroutineScope(Dispatchers.Default + SupervisorJob())

        // 🪵 LOG LOOP (ALWAYS, even idle)
        scope?.launch {
            while (isActive) {
                DebugLogger.logControls(
                    state = webSocketManager.state,
                    latencyMs = webSocketManager.latencyMs,
                    controls = lastControlsSnapshot
                )
                delay(logIntervalMs)
            }
        }

    }

    fun stop() {
        scope?.cancel()
        scope = null
    }

    /* ---------------- REGISTRY ---------------- */

    fun register(control: Control) {
        if (!idMap.containsKey(control.config.id)) {
            idMap[control.config.id] = nextId++
        }

        controls[control.config.id] = control
        updateSnapshot()
        emitImmediately()
        println("REGISTERED CONTROL: ${control.config.id}")
    }

    fun unregister(controlId: String) {
        controls.remove(controlId)
        updateSnapshot()
        emitImmediately()
    }

    fun markDirty() {
        updateSnapshot()
        emitImmediately()
    }

    fun sendDescriptor() {
        val list = controls.values.map {
            mapOf(
                "id" to idMap[it.config.id]!!,
                "kind" to it.config.type.name.lowercase(),
                "label" to it.config.label,
                "min" to it.config.min,
                "max" to it.config.max,
                "center" to it.config.center
            )
        }

        val json = toJson(
            mapOf(
                "type" to "control_descriptor",
                "controls" to list
            )
        )

        webSocketManager.send(json)
    }


    /* ---------------- INTERNAL ---------------- */

    private fun updateSnapshot() {
        lastControlsSnapshot =
            if (controls.isEmpty()) emptyMap()
            else serializeControls(controls.values.toList())
    }

    private fun emitImmediately() {
        // prevent overlapping sends
        if (!sending.compareAndSet(false, true)) return

        scope?.launch {
            try {
                sendPacket()
            } finally {
                sending.set(false)
            }
        }
    }

    private fun sendPacket() {
        val packet = BinaryControlPacket.build(
            controls = controls.values,
            idMap = idMap
        )
        webSocketManager.sendBinary(packet)
    }



}
