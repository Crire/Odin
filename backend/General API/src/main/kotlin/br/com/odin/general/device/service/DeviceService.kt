package br.com.odin.general.device.service

import br.com.odin.general.common.exception.NotFoundException
import br.com.odin.general.device.domain.Device
import br.com.odin.general.device.dto.DeviceResponse
import br.com.odin.general.device.dto.RegisterDeviceRequest
import br.com.odin.general.device.repository.DeviceRepository
import br.com.odin.general.identity.repository.UserRepository
import br.com.odin.general.identity.service.CurrentUser
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Clock
import java.time.Instant
import java.util.UUID

@Service
class DeviceService(
    private val deviceRepository: DeviceRepository,
    private val userRepository: UserRepository,
    private val clock: Clock,
) {
    @Transactional
    fun register(currentUser: CurrentUser, request: RegisterDeviceRequest): DeviceResponse {
        val user = userRepository.findById(currentUser.id).orElseThrow { NotFoundException("Usuario") }
        val device = deviceRepository.findByUser_IdAndPushToken(currentUser.id, request.pushToken)
            ?: Device(user = user, pushToken = request.pushToken)

        device.platform = request.platform
        device.provider = request.provider
        device.active = true
        device.lastSeenAt = Instant.now(clock)
        return DeviceResponse.from(deviceRepository.save(device))
    }

    @Transactional(readOnly = true)
    fun list(currentUser: CurrentUser): List<DeviceResponse> =
        deviceRepository.findAllByUser_IdAndActiveTrueOrderByLastSeenAtDesc(currentUser.id).map(DeviceResponse::from)

    @Transactional
    fun deactivate(currentUser: CurrentUser, deviceId: UUID) {
        val device = deviceRepository.findByIdAndUser_Id(deviceId, currentUser.id) ?: throw NotFoundException("Dispositivo")
        device.active = false
    }
}
