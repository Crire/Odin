package br.com.odin.general.notification.service

import br.com.odin.general.group.domain.OdinGroup
import br.com.odin.general.identity.domain.AppUser
import br.com.odin.general.notification.domain.NotificationJob
import br.com.odin.general.notification.domain.NotificationStatus
import br.com.odin.general.notification.domain.NotificationType
import br.com.odin.general.notification.repository.NotificationJobRepository
import br.com.odin.general.outbox.service.OutboxEventPublisher
import br.com.odin.general.task.domain.TaskItem
import br.com.odin.general.websocket.RealtimeEventPublisher
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Clock
import java.time.Instant
import java.time.temporal.ChronoUnit

@Service
class NotificationSchedulingService(
    private val notificationJobRepository: NotificationJobRepository,
    private val outboxEventPublisher: OutboxEventPublisher,
    private val realtimeEventPublisher: RealtimeEventPublisher,
    private val clock: Clock,
    @Value("\${odin.notifications.default-reminder-minutes-before-due}") private val defaultReminderMinutes: Long,
) {
    @Transactional
    fun scheduleAssignment(task: TaskItem, user: AppUser, group: OdinGroup?) {
        createJob(
            user = user,
            task = task,
            group = group,
            type = NotificationType.ASSIGNMENT,
            title = "Nova tarefa atribuida",
            message = "Voce recebeu a tarefa: ${task.title}",
            scheduledAt = Instant.now(clock),
        )
    }

    @Transactional
    fun scheduleDueReminder(task: TaskItem, user: AppUser, group: OdinGroup?) {
        val dueAt = task.dueAt ?: return
        val scheduledAt = dueAt.minus(defaultReminderMinutes, ChronoUnit.MINUTES).let {
            if (it.isBefore(Instant.now(clock))) Instant.now(clock) else it
        }
        createJob(
            user = user,
            task = task,
            group = group,
            type = NotificationType.DUE_SOON,
            title = "Prazo proximo",
            message = "A tarefa ${task.title} vence em breve.",
            scheduledAt = scheduledAt,
        )
    }

    @Transactional
    fun scheduleMovement(task: TaskItem, user: AppUser, group: OdinGroup?) {
        createJob(
            user = user,
            task = task,
            group = group,
            type = NotificationType.MOVEMENT,
            title = "Card movimentado",
            message = "A tarefa ${task.title} foi movimentada no quadro.",
            scheduledAt = Instant.now(clock),
        )
    }

    @Transactional
    fun cancelPendingForTask(task: TaskItem) {
        notificationJobRepository.findAllByTask_IdAndStatus(task.id, NotificationStatus.PENDING).forEach {
            it.status = NotificationStatus.CANCELLED
        }
    }

    private fun createJob(
        user: AppUser,
        task: TaskItem,
        group: OdinGroup?,
        type: NotificationType,
        title: String,
        message: String,
        scheduledAt: Instant,
    ): NotificationJob? {
        val idempotencyKey = "user:${user.id}:task:${task.id}:type:$type:scheduled:${scheduledAt.epochSecond}"
        if (notificationJobRepository.existsByIdempotencyKey(idempotencyKey)) {
            return null
        }
        val job = notificationJobRepository.save(
            NotificationJob(
                user = user,
                task = task,
                group = group,
                type = type,
                title = title,
                message = message,
                scheduledAt = scheduledAt,
                idempotencyKey = idempotencyKey,
            ),
        )
        val payload = mapOf(
            "notificationJobId" to job.id.toString(),
            "userId" to user.id.toString(),
            "taskId" to task.id.toString(),
            "type" to type.name,
            "scheduledAt" to scheduledAt.toString(),
        )
        outboxEventPublisher.publish("NotificationJobCreated", "notification_job", job.id, "notification.job.created", payload)
        realtimeEventPublisher.publishUserNotification(user.id, payload)
        return job
    }
}
