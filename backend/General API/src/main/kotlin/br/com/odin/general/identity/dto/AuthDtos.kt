package br.com.odin.general.identity.dto

import br.com.odin.general.identity.domain.AppUser
import br.com.odin.general.identity.domain.UserRole
import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size
import java.time.Instant
import java.util.UUID

data class RegisterRequest(
    @field:NotBlank
    @field:Size(max = 120)
    val name: String,

    @field:Email
    @field:NotBlank
    val email: String,

    @field:NotBlank
    @field:Size(min = 8, max = 120)
    val password: String,

    @field:Size(max = 80)
    val timezone: String = "UTC",
)

data class LoginRequest(
    @field:Email
    @field:NotBlank
    val email: String,

    @field:NotBlank
    val password: String,
)

data class AuthResponse(
    val accessToken: String,
    val tokenType: String = "Bearer",
    val expiresAt: Instant,
    val user: UserResponse,
)

data class UserResponse(
    val id: UUID,
    val name: String,
    val email: String,
    val timezone: String,
    val role: UserRole,
) {
    companion object {
        fun from(user: AppUser): UserResponse =
            UserResponse(
                id = user.id,
                name = user.name,
                email = user.email,
                timezone = user.timezone,
                role = user.role,
            )
    }
}

data class UpdateUserRequest(
    @field:NotBlank
    @field:Size(max = 120)
    val name: String,

    @field:NotBlank
    @field:Size(max = 80)
    val timezone: String,
)
