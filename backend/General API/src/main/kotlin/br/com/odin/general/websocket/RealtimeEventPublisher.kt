package br.com.odin.general.websocket

import org.springframework.messaging.simp.SimpMessagingTemplate
import org.springframework.stereotype.Service
import java.util.UUID

@Service
class RealtimeEventPublisher(
    private val messagingTemplate: SimpMessagingTemplate,
) {
    fun publishBoardEvent(boardId: UUID, eventName: String, payload: Any) {
        val body: Any = mapOf("event" to eventName, "payload" to payload)
        messagingTemplate.convertAndSend("/topic/boards/$boardId", body)
    }

    fun publishGroupEvent(groupId: UUID, eventName: String, payload: Any) {
        val body: Any = mapOf("event" to eventName, "payload" to payload)
        messagingTemplate.convertAndSend("/topic/groups/$groupId", body)
    }

    fun publishUserNotification(userId: UUID, payload: Any) {
        messagingTemplate.convertAndSendToUser(userId.toString(), "/queue/notifications", payload)
    }
}
