package com.example.voiceai.network

import com.example.voiceai.audio.AudioConfig
import org.json.JSONObject

sealed class ServerMessage {
    data class Status(val value: String) : ServerMessage()
    data class Transcript(val text: String) : ServerMessage()
    data class AssistantResponse(val text: String) : ServerMessage()
    data class Error(val message: String) : ServerMessage()
}

object VoiceProtocol {
    fun startSessionJson(): String {
        return JSONObject()
            .put("type", "start_session")
            .put("sampleRate", AudioConfig.SAMPLE_RATE)
            .put("encoding", "linear16")
            .put("language", "pt-BR")
            .toString()
    }

    fun endUtteranceJson(): String {
        return JSONObject()
            .put("type", "end_utterance")
            .toString()
    }

    fun closeSessionJson(): String {
        return JSONObject()
            .put("type", "close_session")
            .toString()
    }

    fun parseServerMessage(text: String): ServerMessage {
        val json = JSONObject(text)
        return when (val type = json.getString("type")) {
            "status" -> ServerMessage.Status(json.getString("value"))
            "transcript" -> ServerMessage.Transcript(json.getString("text"))
            "assistant_response" -> ServerMessage.AssistantResponse(json.getString("text"))
            "error" -> ServerMessage.Error(json.getString("message"))
            else -> ServerMessage.Error("Mensagem desconhecida do servidor: $type")
        }
    }
}
