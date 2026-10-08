package br.com.odin.general.identity.service

import br.com.odin.general.common.exception.ConflictException
import br.com.odin.general.common.exception.UnauthorizedException
import br.com.odin.general.identity.domain.AppUser
import br.com.odin.general.identity.dto.AuthResponse
import br.com.odin.general.identity.dto.LoginRequest
import br.com.odin.general.identity.dto.RegisterRequest
import br.com.odin.general.identity.dto.UserResponse
import br.com.odin.general.identity.repository.UserRepository
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class AuthService(
    private val userRepository: UserRepository,
    private val passwordEncoder: PasswordEncoder,
    private val jwtTokenService: JwtTokenService,
) {
    @Transactional
    fun register(request: RegisterRequest): AuthResponse {
        val email = request.email.trim().lowercase()
        if (userRepository.existsByEmailIgnoreCase(email)) {
            throw ConflictException("E-mail ja cadastrado.")
        }

        val user = userRepository.save(
            AppUser(
                name = request.name.trim(),
                email = email,
                passwordHash = passwordEncoder.encode(request.password) ?: error("Password encoder returned null."),
                timezone = request.timezone.trim().ifBlank { "UTC" },
            ),
        )
        return issueToken(user)
    }

    @Transactional(readOnly = true)
    fun login(request: LoginRequest): AuthResponse {
        val user = userRepository.findByEmailIgnoreCase(request.email.trim()).orElseThrow { UnauthorizedException() }
        if (!user.active || !passwordEncoder.matches(request.password, user.passwordHash)) {
            throw UnauthorizedException()
        }
        return issueToken(user)
    }

    fun issueTokenForAuthenticatedUser(user: AppUser): AuthResponse = issueToken(user)

    private fun issueToken(user: AppUser): AuthResponse {
        val token = jwtTokenService.createAccessToken(user)
        return AuthResponse(
            accessToken = token.token,
            expiresAt = token.expiresAt,
            user = UserResponse.from(user),
        )
    }
}
