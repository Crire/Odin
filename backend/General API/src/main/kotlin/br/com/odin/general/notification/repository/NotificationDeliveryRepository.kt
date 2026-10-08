package br.com.odin.general.notification.repository

import br.com.odin.general.notification.domain.NotificationDelivery
import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface NotificationDeliveryRepository : JpaRepository<NotificationDelivery, UUID>
