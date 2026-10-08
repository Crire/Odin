package br.com.odin.general.identity.repository

import br.com.odin.general.identity.domain.AppUser
import org.springframework.data.jpa.repository.JpaRepository
import java.util.Optional
import java.util.UUID

interface UserRepository : JpaRepository<AppUser, UUID> {
    fun existsByEmailIgnoreCase(email: String): Boolean

    fun findByEmailIgnoreCase(email: String): Optional<AppUser>
}
