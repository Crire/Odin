package com.example.voiceai.conversation

import android.annotation.SuppressLint
import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.voiceai.AppConfig
import com.example.voiceai.audio.AndroidTtsEngine
import com.example.voiceai.audio.MicrophoneRecorder
import com.example.voiceai.network.VoiceWebSocketClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class ConversationViewModel(application: Application) : AndroidViewModel(application) {
    private val _state = MutableStateFlow(ConversationState())
    val state: StateFlow<ConversationState> = _state.asStateFlow()

    private val recorder = MicrophoneRecorder(viewModelScope)
    private val ttsEngine = AndroidTtsEngine(application)
    private lateinit var webSocketClient: VoiceWebSocketClient

    init {
        webSocketClient = VoiceWebSocketClient(
            object : VoiceWebSocketClient.Callbacks {
                override fun onConnected() {
                    _state.update {
                        it.copy(
                            isConnected = true,
                            status = ConversationStatus.CONNECTED,
                            errorMessage = null
                        )
                    }
                    webSocketClient.sendStartSession()
                }

                override fun onDisconnected() {
                    recorder.stop()
                    _state.update {
                        it.copy(
                            isConnected = false,
                            isRecording = false,
                            status = ConversationStatus.DISCONNECTED
                        )
                    }
                }

                override fun onStatusReceived(value: String) {
                    _state.update {
                        it.copy(status = value.toConversationStatus(), errorMessage = null)
                    }
                }

                override fun onTranscriptReceived(text: String) {
                    appendMessage(MessageAuthor.USER, text)
                }

                override fun onAssistantResponseReceived(text: String) {
                    appendMessage(MessageAuthor.ASSISTANT, text)
                    _state.update { it.copy(status = ConversationStatus.RESPONDING) }
                    viewModelScope.launch(Dispatchers.Main) {
                        ttsEngine.speak(text)
                    }
                }

                override fun onError(message: String) {
                    recorder.stop()
                    _state.update {
                        it.copy(
                            isRecording = false,
                            status = ConversationStatus.ERROR,
                            errorMessage = message
                        )
                    }
                }
            }
        )
    }

    fun handlePrimaryAction(hasAudioPermission: Boolean) {
        val current = _state.value

        if (!current.isConnected) {
            connect()
            return
        }

        if (!hasAudioPermission) {
            _state.update {
                it.copy(
                    status = ConversationStatus.ERROR,
                    errorMessage = "Permissão de microfone necessária"
                )
            }
            return
        }

        if (current.isRecording) {
            stopRecording()
        } else {
            startRecording()
        }
    }

    fun connect() {
        if (_state.value.isConnected) return
        webSocketClient.connect(AppConfig.DEFAULT_WS_URL)
    }

    @SuppressLint("MissingPermission")
    private fun startRecording() {
        recorder.start(
            onChunk = webSocketClient::sendAudioChunk,
            onError = { error ->
                _state.update {
                    it.copy(
                        isRecording = false,
                        status = ConversationStatus.ERROR,
                        errorMessage = error.message ?: "Erro ao capturar áudio"
                    )
                }
            }
        )

        _state.update {
            it.copy(
                isRecording = true,
                status = ConversationStatus.LISTENING,
                errorMessage = null
            )
        }
    }

    private fun stopRecording() {
        recorder.stop()
        webSocketClient.sendEndUtterance()
        _state.update {
            it.copy(
                isRecording = false,
                status = ConversationStatus.TRANSCRIBING
            )
        }
    }

    fun closeSession() {
        recorder.stop()
        ttsEngine.stop()
        webSocketClient.sendCloseSession()
        webSocketClient.disconnect()
        _state.update {
            it.copy(
                isConnected = false,
                isRecording = false,
                status = ConversationStatus.DISCONNECTED
            )
        }
    }

    fun shutdown() {
        recorder.stop()
        webSocketClient.disconnect()
        ttsEngine.shutdown()
    }

    override fun onCleared() {
        shutdown()
        super.onCleared()
    }

    private fun appendMessage(author: MessageAuthor, text: String) {
        _state.update {
            it.copy(
                messages = it.messages + ConversationMessageUi(
                    author = author,
                    text = text
                )
            )
        }
    }
}

private fun String.toConversationStatus(): ConversationStatus {
    return when (this) {
        "listening" -> ConversationStatus.LISTENING
        "transcribing" -> ConversationStatus.TRANSCRIBING
        "thinking" -> ConversationStatus.THINKING
        "responding" -> ConversationStatus.RESPONDING
        "error" -> ConversationStatus.ERROR
        else -> ConversationStatus.CONNECTED
    }
}
