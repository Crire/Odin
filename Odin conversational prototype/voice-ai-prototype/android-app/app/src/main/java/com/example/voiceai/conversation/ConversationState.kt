package com.example.voiceai.conversation

import java.util.concurrent.atomic.AtomicLong

private val messageId = AtomicLong(0)

enum class MessageAuthor {
    USER,
    ASSISTANT
}

enum class ConversationStatus {
    DISCONNECTED,
    CONNECTED,
    LISTENING,
    TRANSCRIBING,
    THINKING,
    RESPONDING,
    ERROR
}

data class ConversationMessageUi(
    val id: Long = messageId.incrementAndGet(),
    val author: MessageAuthor,
    val text: String
)

data class ConversationState(
    val isConnected: Boolean = false,
    val isRecording: Boolean = false,
    val status: ConversationStatus = ConversationStatus.DISCONNECTED,
    val errorMessage: String? = null,
    val messages: List<ConversationMessageUi> = emptyList()
)
