package br.com.odin.general.device.controller

import br.com.odin.general.device.dto.DeviceResponse
import br.com.odin.general.device.dto.RegisterDeviceRequest
import br.com.odin.general.device.service.DeviceService
import br.com.odin.general.identity.service.CurrentUser
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
@RequestMapping("/api/v1/devices")
class DeviceController(
    private val deviceService: DeviceService,
) {
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    fun register(
        @AuthenticationPrincipal currentUser: CurrentUser,
        @Valid @RequestBody request: RegisterDeviceRequest,
    ): DeviceResponse = deviceService.register(currentUser, request)

    @GetMapping
    fun list(@AuthenticationPrincipal currentUser: CurrentUser): List<DeviceResponse> =
        deviceService.list(currentUser)

    @DeleteMapping("/{deviceId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun delete(
        @AuthenticationPrincipal currentUser: CurrentUser,
        @PathVariable deviceId: UUID,
    ) {
        deviceService.deactivate(currentUser, deviceId)
    }
}
