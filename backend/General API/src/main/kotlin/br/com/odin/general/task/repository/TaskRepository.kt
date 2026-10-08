package br.com.odin.general.task.repository

import br.com.odin.general.task.domain.TaskItem
import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface TaskRepository : JpaRepository<TaskItem, UUID> {
    fun findAllByCreatedBy_IdAndBoardIsNullOrderByDueAtAsc(userId: UUID): List<TaskItem>

    fun findAllByBoard_IdOrderByCreatedAtDesc(boardId: UUID): List<TaskItem>
}
