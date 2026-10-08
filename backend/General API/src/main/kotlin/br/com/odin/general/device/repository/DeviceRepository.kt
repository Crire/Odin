package br.com.odin.general.device.repository

import br.com.odin.general.device.domain.Device
import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface DeviceRepository : JpaRepository<Device, UUID> {
    fun findAllByUser_IdAndActiveTrueOrderByLastSeenAtDesc(userId: UUID): List<Device>

    fun findByUser_IdAndPushToken(userId: UUID, pushToken: String): Device?

    fun findByIdAndUser_Id(id: UUID, userId: UUID): Device?
}
