package br.com.odin.general.notification.domain

import br.com.odin.general.group.domain.OdinGroup
import br.com.odin.general.identity.domain.AppUser
import br.com.odin.general.task.domain.TaskItem
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.FetchType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import org.hibernate.annotations.CreationTimestamp
import org.hibernate.annotations.UpdateTimestamp
import java.time.Instant
import java.util.UUID

@Entity
@Table(name = "notification_jobs")
class NotificationJob(
    @Id
    @Column(nullable = false, updatable = false)
    var id: UUID = UUID.randomUUID(),

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    var user: AppUser = AppUser(),

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "task_id")
    var task: TaskItem? = null,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "group_id")
    var group: OdinGroup? = null,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    var type: NotificationType = NotificationType.REMINDER,

    @Column(nullable = false)
    var title: String = "",

    @Column(nullable = false)
    var message: String = "",

    @Column(name = "scheduled_at", nullable = false)
    var scheduledAt: Instant = Instant.now(),

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    var status: NotificationStatus = NotificationStatus.PENDING,

    @Column(nullable = false)
    var attempts: Int = 0,

    @Column(name = "max_attempts", nullable = false)
    var maxAttempts: Int = 3,

    @Column(name = "idempotency_key", nullable = false, unique = true)
    var idempotencyKey: String = "",

    @Column(name = "read_at")
    var readAt: Instant? = null,

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    var createdAt: Instant? = null,

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    var updatedAt: Instant? = null,
)
