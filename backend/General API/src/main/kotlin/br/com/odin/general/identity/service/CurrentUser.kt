package br.com.odin.general.identity.service

import br.com.odin.general.identity.domain.AppUser
import br.com.odin.general.identity.domain.UserRole
import java.util.UUID

data class CurrentUser(
    val id: UUID,
    val email: String,
    val role: UserRole,
) {
    companion object {
        fun from(user: AppUser): CurrentUser =
            CurrentUser(
                id = user.id,
                email = user.email,
                role = user.role,
            )
    }
}
