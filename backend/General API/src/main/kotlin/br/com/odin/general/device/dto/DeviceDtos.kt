package br.com.odin.general.device.dto

import br.com.odin.general.device.domain.Device
import br.com.odin.general.device.domain.DevicePlatform
import br.com.odin.general.device.domain.PushProvider
import jakarta.validation.constraints.NotBlank
import java.time.Instant
import java.util.UUID

data class RegisterDeviceRequest(
    val platform: DevicePlatform,
    val provider: PushProvider = PushProvider.FCM,

    @field:NotBlank
    val pushToken: String,
)

data class DeviceResponse(
    val id: UUID,
    val platform: DevicePlatform,
    val provider: PushProvider,
    val active: Boolean,
    val lastSeenAt: Instant,
    val createdAt: Instant?,
) {
    companion object {
        fun from(device: Device): DeviceResponse =
            DeviceResponse(
                id = device.id,
                platform = device.platform,
                provider = device.provider,
                active = device.active,
                lastSeenAt = device.lastSeenAt,
                createdAt = device.createdAt,
            )
    }
}
