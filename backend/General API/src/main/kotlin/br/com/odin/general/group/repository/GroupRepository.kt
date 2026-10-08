package br.com.odin.general.group.repository

import br.com.odin.general.group.domain.OdinGroup
import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface GroupRepository : JpaRepository<OdinGroup, UUID>
