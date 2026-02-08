package com.usb.drivingremote.network

import okhttp3.*
import okio.ByteString
import java.util.concurrent.TimeUnit
import kotlin.jvm.Throws

class WebSocketClient(
    private val serverUrl: String,
    private val listener: Listener
){
    interface Listener {
        fun onConnected()
        fun onDisconnected()
        fun onMessage(text: String)
        fun onError(error: String)
        fun onLatencyUpdate(ms: Long)
    }

    private val client = OkHttpClient.Builder()

        .pingInterval(5, TimeUnit.SECONDS) // Keeps connection alive
        .build()

    private var webSocket: WebSocket? = null
    private var lastPingTime = 0L

    fun connect(){
        val request = Request.Builder()
            .url(serverUrl)
            .build()

        webSocket = client.newWebSocket(request, socketListener)
    }

    fun disconnect(){
        webSocket?.close(1000, "Client disconnect")
        webSocket = null
    }

    fun send(text: String){
        webSocket?.send(text)
    }
    fun send(bytes: ByteArray) {
        webSocket?.send(okio.ByteString.of(*bytes))
    }


    private val socketListener = object : WebSocketListener(){
        override fun onOpen(ws: WebSocket, response: Response){
            listener.onConnected()
        }
        override fun onMessage(ws: WebSocket, text: String) {
            listener.onMessage(text)
        }

        override fun onMessage(ws: WebSocket, bytes: ByteString) {
            // ignore binary
        }

        override fun onClosing(ws: WebSocket, code: Int, reason: String){
            ws.close(code, reason)
        }
        override fun onClosed(ws: WebSocket, code: Int, reason: String){
            listener.onDisconnected()
        }
        override fun onFailure(
            ws: WebSocket,
            t: Throwable,
            response: Response?
        ) {
            listener.onError(t.message ?: "Unknown socket error")
        }

    }

    fun sendPing() {
        val t = System.currentTimeMillis()
        webSocket?.send("""{"type":"ping","t":$t}""")
    }


    fun handlePong(sentTime: Long) {
        val rtt = System.currentTimeMillis() - sentTime
        listener.onLatencyUpdate(rtt)
    }



}