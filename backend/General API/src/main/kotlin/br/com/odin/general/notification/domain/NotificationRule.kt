package br.com.odin.general.notification.domain

import br.com.odin.general.group.domain.OdinGroup
import br.com.odin.general.identity.domain.AppUser
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import org.hibernate.annotations.CreationTimestamp
import org.hibernate.annotations.UpdateTimestamp
import java.time.Instant
import java.time.LocalTime
import java.util.UUID

@Entity
@Table(name = "notification_rules")
class NotificationRule(
    @Id
    @Column(nullable = false, updatable = false)
    var id: UUID = UUID.randomUUID(),

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    var user: AppUser = AppUser(),

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "group_id")
    var group: OdinGroup? = null,

    @Column(name = "interval_minutes", nullable = false)
    var intervalMinutes: Int = 60,

    @Column(name = "start_time", nullable = false)
    var startTime: LocalTime = LocalTime.of(8, 0),

    @Column(name = "end_time", nullable = false)
    var endTime: LocalTime = LocalTime.of(22, 0),

    @Column(name = "days_of_week", nullable = false)
    var daysOfWeek: String = "MON,TUE,WED,THU,FRI",

    @Column(nullable = false)
    var enabled: Boolean = true,

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    var createdAt: Instant? = null,

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    var updatedAt: Instant? = null,
)
