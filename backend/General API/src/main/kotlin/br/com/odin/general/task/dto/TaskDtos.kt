package br.com.odin.general.task.dto

import br.com.odin.general.task.domain.TaskItem
import br.com.odin.general.task.domain.TaskPriority
import br.com.odin.general.task.domain.TaskStatus
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size
import java.time.Instant
import java.util.UUID

data class CreatePersonalTaskRequest(
    @field:NotBlank
    @field:Size(max = 180)
    val title: String,

    @field:Size(max = 4000)
    val description: String? = null,
    val priority: TaskPriority = TaskPriority.MEDIUM,
    val dueAt: Instant? = null,
)

data class CreateBoardTaskRequest(
    @field:NotBlank
    @field:Size(max = 180)
    val title: String,

    @field:Size(max = 4000)
    val description: String? = null,
    val columnId: UUID? = null,
    val priority: TaskPriority = TaskPriority.MEDIUM,
    val dueAt: Instant? = null,
    val assigneeIds: Set<UUID> = emptySet(),
)

data class UpdateTaskRequest(
    @field:NotBlank
    @field:Size(max = 180)
    val title: String,

    @field:Size(max = 4000)
    val description: String? = null,
    val priority: TaskPriority = TaskPriority.MEDIUM,
    val dueAt: Instant? = null,
)

data class MoveTaskRequest(
    val toColumnId: UUID,
)

data class AddAssigneeRequest(
    val userId: UUID,
)

data class TaskResponse(
    val id: UUID,
    val boardId: UUID?,
    val columnId: UUID?,
    val title: String,
    val description: String?,
    val priority: TaskPriority,
    val dueAt: Instant?,
    val createdBy: UUID,
    val status: TaskStatus,
    val assigneeIds: List<UUID>,
    val createdAt: Instant?,
    val updatedAt: Instant?,
) {
    companion object {
        fun from(task: TaskItem, assigneeIds: List<UUID>): TaskResponse =
            TaskResponse(
                id = task.id,
                boardId = task.board?.id,
                columnId = task.column?.id,
                title = task.title,
                description = task.description,
                priority = task.priority,
                dueAt = task.dueAt,
                createdBy = task.createdBy.id,
                status = task.status,
                assigneeIds = assigneeIds,
                createdAt = task.createdAt,
                updatedAt = task.updatedAt,
            )
    }
}
