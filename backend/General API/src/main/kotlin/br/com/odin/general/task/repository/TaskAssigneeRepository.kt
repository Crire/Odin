package br.com.odin.general.task.repository

import br.com.odin.general.task.domain.TaskAssignee
import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface TaskAssigneeRepository : JpaRepository<TaskAssignee, UUID> {
    fun findAllByTask_Id(taskId: UUID): List<TaskAssignee>

    fun existsByTask_IdAndUser_Id(taskId: UUID, userId: UUID): Boolean

    fun findByTask_IdAndUser_Id(taskId: UUID, userId: UUID): TaskAssignee?

    fun deleteByTask_IdAndUser_Id(taskId: UUID, userId: UUID)
}
