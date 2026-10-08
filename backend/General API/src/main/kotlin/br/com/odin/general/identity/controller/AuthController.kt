package br.com.odin.general.identity.controller

import br.com.odin.general.identity.dto.AuthResponse
import br.com.odin.general.identity.dto.LoginRequest
import br.com.odin.general.identity.dto.RegisterRequest
import br.com.odin.general.identity.dto.UserResponse
import br.com.odin.general.identity.repository.UserRepository
import br.com.odin.general.identity.service.AuthService
import br.com.odin.general.identity.service.CurrentUser
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/auth")
class AuthController(
    private val authService: AuthService,
    private val userRepository: UserRepository,
) {
    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    fun register(@Valid @RequestBody request: RegisterRequest): AuthResponse =
        authService.register(request)

    @PostMapping("/login")
    fun login(@Valid @RequestBody request: LoginRequest): AuthResponse =
        authService.login(request)

    @GetMapping("/me")
    fun me(@AuthenticationPrincipal currentUser: CurrentUser): UserResponse =
        UserResponse.from(userRepository.findById(currentUser.id).orElseThrow())

    @PostMapping("/refresh")
    fun refresh(@AuthenticationPrincipal currentUser: CurrentUser): AuthResponse {
        val user = userRepository.findById(currentUser.id).orElseThrow()
        return authService.issueTokenForAuthenticatedUser(user)
    }

    @PostMapping("/logout")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun logout() {
    }
}
