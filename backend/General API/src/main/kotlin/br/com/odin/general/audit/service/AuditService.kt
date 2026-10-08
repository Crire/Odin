package br.com.odin.general.audit.service

import br.com.odin.general.audit.domain.AuditLog
import br.com.odin.general.audit.repository.AuditLogRepository
import br.com.odin.general.identity.repository.UserRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
class AuditService(
    private val auditLogRepository: AuditLogRepository,
    private val userRepository: UserRepository,
) {
    @Transactional
    fun record(
        actorUserId: UUID?,
        action: String,
        aggregateType: String,
        aggregateId: UUID,
        metadata: Map<String, Any?> = emptyMap(),
    ) {
        val actor = actorUserId?.let { userRepository.findById(it).orElse(null) }
        auditLogRepository.save(
            AuditLog(
                actor = actor,
                action = action,
                aggregateType = aggregateType,
                aggregateId = aggregateId,
                metadata = metadata,
            ),
        )
    }
}
