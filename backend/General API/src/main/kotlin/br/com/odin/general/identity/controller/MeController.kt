package br.com.odin.general.identity.controller

import br.com.odin.general.common.exception.NotFoundException
import br.com.odin.general.identity.dto.UpdateUserRequest
import br.com.odin.general.identity.dto.UserResponse
import br.com.odin.general.identity.repository.UserRepository
import br.com.odin.general.identity.service.CurrentUser
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/users/me")
class MeController(
    private val userRepository: UserRepository,
) {
    @GetMapping
    fun me(@AuthenticationPrincipal currentUser: CurrentUser): UserResponse {
        val user = userRepository.findById(currentUser.id).orElseThrow { NotFoundException("Usuario") }
        return UserResponse.from(user)
    }

    @PatchMapping
    fun update(
        @AuthenticationPrincipal currentUser: CurrentUser,
        @Valid @RequestBody request: UpdateUserRequest,
    ): UserResponse {
        val user = userRepository.findById(currentUser.id).orElseThrow { NotFoundException("Usuario") }
        user.name = request.name.trim()
        user.timezone = request.timezone.trim()
        return UserResponse.from(userRepository.save(user))
    }

    @DeleteMapping
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun delete(@AuthenticationPrincipal currentUser: CurrentUser) {
        val user = userRepository.findById(currentUser.id).orElseThrow { NotFoundException("Usuario") }
        user.active = false
        userRepository.save(user)
    }
}
