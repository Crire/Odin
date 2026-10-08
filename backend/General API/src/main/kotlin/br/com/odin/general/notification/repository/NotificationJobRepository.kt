package br.com.odin.general.notification.repository

import br.com.odin.general.notification.domain.NotificationJob
import br.com.odin.general.notification.domain.NotificationStatus
import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface NotificationJobRepository : JpaRepository<NotificationJob, UUID> {
    fun existsByIdempotencyKey(idempotencyKey: String): Boolean

    fun findAllByUser_IdOrderByCreatedAtDesc(userId: UUID): List<NotificationJob>

    fun findByIdAndUser_Id(id: UUID, userId: UUID): NotificationJob?

    fun findAllByTask_IdAndStatus(taskId: UUID, status: NotificationStatus): List<NotificationJob>
}
