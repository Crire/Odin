package br.com.odin.general.outbox.repository

import br.com.odin.general.outbox.domain.OutboxEvent
import br.com.odin.general.outbox.domain.OutboxStatus
import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface OutboxEventRepository : JpaRepository<OutboxEvent, UUID> {
    fun findTop100ByStatusOrderByCreatedAtAsc(status: OutboxStatus): List<OutboxEvent>
}
