package br.com.odin.general.task.repository

import br.com.odin.general.task.domain.TaskMovement
import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface TaskMovementRepository : JpaRepository<TaskMovement, UUID>
