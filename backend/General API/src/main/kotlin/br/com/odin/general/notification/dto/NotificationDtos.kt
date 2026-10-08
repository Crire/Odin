package br.com.odin.general.notification.dto

import br.com.odin.general.notification.domain.NotificationJob
import br.com.odin.general.notification.domain.NotificationRule
import br.com.odin.general.notification.domain.NotificationStatus
import br.com.odin.general.notification.domain.NotificationType
import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotBlank
import java.time.Instant
import java.time.LocalTime
import java.util.UUID

data class CreateNotificationRuleRequest(
    val groupId: UUID? = null,

    @field:Min(5)
    @field:Max(1440)
    val intervalMinutes: Int,

    val startTime: LocalTime,
    val endTime: LocalTime,

    @field:NotBlank
    val daysOfWeek: String,
    val enabled: Boolean = true,
)

data class UpdateNotificationRuleRequest(
    @field:Min(5)
    @field:Max(1440)
    val intervalMinutes: Int,
    val startTime: LocalTime,
    val endTime: LocalTime,

    @field:NotBlank
    val daysOfWeek: String,
    val enabled: Boolean,
)

data class NotificationRuleResponse(
    val id: UUID,
    val groupId: UUID?,
    val intervalMinutes: Int,
    val startTime: LocalTime,
    val endTime: LocalTime,
    val daysOfWeek: String,
    val enabled: Boolean,
) {
    companion object {
        fun from(rule: NotificationRule): NotificationRuleResponse =
            NotificationRuleResponse(
                id = rule.id,
                groupId = rule.group?.id,
                intervalMinutes = rule.intervalMinutes,
                startTime = rule.startTime,
                endTime = rule.endTime,
                daysOfWeek = rule.daysOfWeek,
                enabled = rule.enabled,
            )
    }
}

data class NotificationResponse(
    val id: UUID,
    val taskId: UUID?,
    val groupId: UUID?,
    val type: NotificationType,
    val title: String,
    val message: String,
    val scheduledAt: Instant,
    val status: NotificationStatus,
    val readAt: Instant?,
    val createdAt: Instant?,
) {
    companion object {
        fun from(job: NotificationJob): NotificationResponse =
            NotificationResponse(
                id = job.id,
                taskId = job.task?.id,
                groupId = job.group?.id,
                type = job.type,
                title = job.title,
                message = job.message,
                scheduledAt = job.scheduledAt,
                status = job.status,
                readAt = job.readAt,
                createdAt = job.createdAt,
            )
    }
}
