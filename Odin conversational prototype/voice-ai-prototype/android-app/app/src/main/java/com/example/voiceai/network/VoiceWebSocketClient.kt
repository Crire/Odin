package com.example.voiceai.network

import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import okio.ByteString.Companion.toByteString
import java.util.concurrent.TimeUnit

class VoiceWebSocketClient(
    private val callbacks: Callbacks
) {
    interface Callbacks {
        fun onConnected()
        fun onDisconnected()
        fun onStatusReceived(value: String)
        fun onTranscriptReceived(text: String)
        fun onAssistantResponseReceived(text: String)
        fun onError(message: String)
    }

    private val client = OkHttpClient.Builder()
        .readTimeout(0, TimeUnit.MILLISECONDS)
        .build()

    @Volatile
    private var webSocket: WebSocket? = null

    fun connect(url: String) {
        if (webSocket != null) return

        val request = Request.Builder()
            .url(url)
            .build()

        webSocket = client.newWebSocket(
            request,
            object : WebSocketListener() {
                override fun onOpen(webSocket: WebSocket, response: Response) {
                    callbacks.onConnected()
                }

                override fun onMessage(webSocket: WebSocket, text: String) {
                    handleServerText(text)
                }

                override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
                    webSocket.close(code, reason)
                }

                override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                    this@VoiceWebSocketClient.webSocket = null
                    callbacks.onDisconnected()
                }

                override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                    this@VoiceWebSocketClient.webSocket = null
                    callbacks.onError(t.message ?: "Erro no WebSocket")
                    callbacks.onDisconnected()
                }
            }
        )
    }

    fun sendStartSession() {
        webSocket?.send(VoiceProtocol.startSessionJson())
    }

    fun sendAudioChunk(bytes: ByteArray) {
        if (bytes.isEmpty()) return
        webSocket?.send(bytes.toByteString())
    }

    fun sendEndUtterance() {
        webSocket?.send(VoiceProtocol.endUtteranceJson())
    }

    fun sendCloseSession() {
        webSocket?.send(VoiceProtocol.closeSessionJson())
    }

    fun disconnect() {
        webSocket?.close(1000, "Client closed")
        webSocket = null
    }

    private fun handleServerText(text: String) {
        runCatching { VoiceProtocol.parseServerMessage(text) }
            .onSuccess { message ->
                when (message) {
                    is ServerMessage.Status -> callbacks.onStatusReceived(message.value)
                    is ServerMessage.Transcript -> callbacks.onTranscriptReceived(message.text)
                    is ServerMessage.AssistantResponse -> callbacks.onAssistantResponseReceived(message.text)
                    is ServerMessage.Error -> callbacks.onError(message.message)
                }
            }
            .onFailure { error ->
                callbacks.onError(error.message ?: "Erro ao interpretar resposta do servidor")
            }
    }
}
