package br.com.odin.general.notification.domain

import br.com.odin.general.device.domain.Device
import br.com.odin.general.device.domain.PushProvider
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
import java.time.Instant
import java.util.UUID

@Entity
@Table(name = "notification_deliveries")
class NotificationDelivery(
    @Id
    @Column(nullable = false, updatable = false)
    var id: UUID = UUID.randomUUID(),

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "notification_job_id", nullable = false)
    var notificationJob: NotificationJob = NotificationJob(),

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "device_id", nullable = false)
    var device: Device = Device(),

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    var provider: PushProvider = PushProvider.FCM,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    var status: DeliveryStatus = DeliveryStatus.SENT,

    @Column(name = "provider_response")
    var providerResponse: String? = null,

    @Column(name = "sent_at")
    var sentAt: Instant? = null,

    @Column(name = "read_at")
    var readAt: Instant? = null,

    @Column(name = "error_message")
    var errorMessage: String? = null,

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    var createdAt: Instant? = null,
)
