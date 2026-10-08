package br.com.odin.general.notification.repository

import br.com.odin.general.notification.domain.NotificationRule
import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface NotificationRuleRepository : JpaRepository<NotificationRule, UUID> {
    fun findAllByUser_IdOrderByCreatedAtDesc(userId: UUID): List<NotificationRule>

    fun findByIdAndUser_Id(id: UUID, userId: UUID): NotificationRule?
}
