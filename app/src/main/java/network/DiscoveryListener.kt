package com.usb.drivingremote.network

import android.content.Context
import android.net.wifi.WifiManager
import kotlinx.coroutines.*
import org.json.JSONObject
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.nio.charset.Charset

class DiscoveryListener(
    context: Context,
    private val onDeviceFound: (Device) -> Unit
) {

    private var socket: DatagramSocket? = null
    private var scope: CoroutineScope? = null
    private val multicastLock: WifiManager.MulticastLock

    init {
        val wifi =
            context.applicationContext.getSystemService(Context.WIFI_SERVICE)
                    as WifiManager
        multicastLock = wifi.createMulticastLock("drivingremote")
    }

    fun start() {
        multicastLock.acquire()

        scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
        scope?.launch {
            try {
                socket = DatagramSocket(5001).apply { broadcast = true }
                val buffer = ByteArray(1024)

                while (isActive) {
                    val packet = DatagramPacket(buffer, buffer.size)
                    socket?.receive(packet)

                    val msg = String(
                        packet.data, 0, packet.length,
                        Charset.forName("UTF-8")
                    )

                    val json = JSONObject(msg)

                    if (json.optString("type") == "discover") {
                        val name = json.optString("name", "DrivingRemote-PC")
                        val url = json.optString("url")

                        if (url.isNotBlank()) {
                            onDeviceFound(    Device(
                                id = url,
                                initialName = name
                            ))
                        }
                    }
                }
            } catch (_: Exception) {}
        }
    }

    fun stop() {
        scope?.cancel()
        socket?.close()
        multicastLock.release()
    }
}
