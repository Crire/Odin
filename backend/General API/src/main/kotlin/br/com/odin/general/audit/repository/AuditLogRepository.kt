package br.com.odin.general.audit.repository

import br.com.odin.general.audit.domain.AuditLog
import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface AuditLogRepository : JpaRepository<AuditLog, UUID>
