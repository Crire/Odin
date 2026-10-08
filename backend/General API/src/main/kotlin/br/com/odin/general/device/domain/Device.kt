package br.com.odin.general.device.domain

import br.com.odin.general.identity.domain.AppUser
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
@Table(name = "devices")
class Device(
    @Id
    @Column(nullable = false, updatable = false)
    var id: UUID = UUID.randomUUID(),

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    var user: AppUser = AppUser(),

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    var platform: DevicePlatform = DevicePlatform.ANDROID,

    @Column(name = "push_token", nullable = false)
    var pushToken: String = "",

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    var provider: PushProvider = PushProvider.FCM,

    @Column(nullable = false)
    var active: Boolean = true,

    @Column(name = "last_seen_at", nullable = false)
    var lastSeenAt: Instant = Instant.now(),

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    var createdAt: Instant? = null,

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    var updatedAt: Instant? = null,
)
