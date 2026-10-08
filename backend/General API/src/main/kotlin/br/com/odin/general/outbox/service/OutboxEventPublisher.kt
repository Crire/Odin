package br.com.odin.general.outbox.service

import br.com.odin.general.common.web.CorrelationIdFilter
import br.com.odin.general.outbox.domain.OutboxEvent
import br.com.odin.general.outbox.domain.OutboxStatus
import br.com.odin.general.outbox.repository.OutboxEventRepository
import org.slf4j.MDC
import org.springframework.amqp.rabbit.core.RabbitTemplate
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Clock
import java.time.Instant
import java.util.UUID

@Service
class OutboxEventPublisher(
    private val outboxEventRepository: OutboxEventRepository,
    private val rabbitTemplate: RabbitTemplate,
    private val clock: Clock,
    @Value("\${odin.events.exchange}") private val exchangeName: String,
) {
    @Transactional
    fun publish(
        eventType: String,
        aggregateType: String,
        aggregateId: UUID,
        routingKey: String,
        payload: Map<String, Any?>,
    ): OutboxEvent {
        val event = outboxEventRepository.save(
            OutboxEvent(
                eventType = eventType,
                aggregateType = aggregateType,
                aggregateId = aggregateId,
                payload = payload,
                correlationId = MDC.get(CorrelationIdFilter.MDC_KEY),
            ),
        )

        runCatching {
            rabbitTemplate.convertAndSend(exchangeName, routingKey, eventEnvelope(event))
        }.onSuccess {
            event.status = OutboxStatus.PUBLISHED
            event.processedAt = Instant.now(clock)
        }.onFailure {
            event.status = OutboxStatus.PENDING
        }

        return event
    }

    private fun eventEnvelope(event: OutboxEvent): Map<String, Any?> =
        mapOf(
            "eventId" to event.id.toString(),
            "eventType" to event.eventType,
            "aggregateType" to event.aggregateType,
            "aggregateId" to event.aggregateId.toString(),
            "occurredAt" to Instant.now(clock).toString(),
            "correlationId" to event.correlationId,
            "payloadVersion" to 1,
            "payload" to event.payload,
        )
}
