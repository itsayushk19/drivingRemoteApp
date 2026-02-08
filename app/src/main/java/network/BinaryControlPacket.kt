package com.usb.drivingremote.controls

import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.roundToInt

/**
 * Binary control packet format (v1)
 *
 * Header:
 * [1]  packet type (1 = control)
 * [1]  axis count
 * [1]  button count
 * [1]  toggle count
 * [4]  timestamp (ms, int32)
 *
 * AXIS ENTRY:
 * [1]  control id
 * [2]  int16 value (-32768..32767)
 *
 * BUTTON ENTRY:
 * [1]  control id
 * [1]  pressed (0/1)
 *
 * TOGGLE ENTRY:
 * [1]  control id
 * [1]  on (0/1)
 */
object BinaryControlPacket {

    private const val TYPE_CONTROL: Byte = 1

    fun build(
        controls: Collection<Control>,
        idMap: Map<String, Byte>
    ): ByteArray {

        val axes = controls.filter { it.state is ControlState.Axis }
        val buttons = controls.filter { it.state is ControlState.Button }
        val toggles = controls.filter { it.state is ControlState.Toggle }

        val size =
            1 + 1 + 1 + 1 + 4 + // header + timestamp
                    axes.size * 3 +
                    buttons.size * 2 +
                    toggles.size * 2

        val buffer = ByteBuffer
            .allocate(size)
            .order(ByteOrder.LITTLE_ENDIAN)

        buffer.put(TYPE_CONTROL)
        buffer.put(axes.size.toByte())
        buffer.put(buttons.size.toByte())
        buffer.put(toggles.size.toByte())
        buffer.putInt((System.currentTimeMillis() and 0xFFFFFFFFL).toInt())

        // AXES
        for (c in axes) {
            val id = idMap[c.config.id]!!
            val v = (c.state as ControlState.Axis).value
            val packed = (v.coerceIn(-1f, 1f) * 32767f).toInt()

            buffer.put(id)
            buffer.putShort(packed.toShort())
        }

        // BUTTONS
        for (c in buttons) {
            val id = idMap[c.config.id]!!
            val pressed = (c.state as ControlState.Button).pressed

            buffer.put(id)
            buffer.put(if (pressed) 1 else 0)
        }

        // TOGGLES
        for (c in toggles) {
            val id = idMap[c.config.id]!!
            val on = (c.state as ControlState.Toggle).on

            buffer.put(id)
            buffer.put(if (on) 1 else 0)
        }

        return buffer.array()
    }
}
