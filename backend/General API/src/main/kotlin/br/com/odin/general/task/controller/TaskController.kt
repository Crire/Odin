package br.com.odin.general.task.controller

import br.com.odin.general.identity.service.CurrentUser
import br.com.odin.general.task.dto.AddAssigneeRequest
import br.com.odin.general.task.dto.CreateBoardTaskRequest
import br.com.odin.general.task.dto.CreatePersonalTaskRequest
import br.com.odin.general.task.dto.MoveTaskRequest
import br.com.odin.general.task.dto.TaskResponse
import br.com.odin.general.task.dto.UpdateTaskRequest
import br.com.odin.general.task.service.TaskService
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
@RequestMapping("/api/v1")
class TaskController(
    private val taskService: TaskService,
) {
    @PostMapping("/tasks/personal")
    @ResponseStatus(HttpStatus.CREATED)
    fun createPersonal(
        @AuthenticationPrincipal currentUser: CurrentUser,
        @Valid @RequestBody request: CreatePersonalTaskRequest,
    ): TaskResponse = taskService.createPersonal(currentUser, request)

    @GetMapping("/tasks/personal")
    fun listPersonal(@AuthenticationPrincipal currentUser: CurrentUser): List<TaskResponse> =
        taskService.listPersonal(currentUser)

    @PostMapping("/boards/{boardId}/tasks")
    @ResponseStatus(HttpStatus.CREATED)
    fun createBoardTask(
        @AuthenticationPrincipal currentUser: CurrentUser,
        @PathVariable boardId: UUID,
        @Valid @RequestBody request: CreateBoardTaskRequest,
    ): TaskResponse = taskService.createBoardTask(currentUser, boardId, request)

    @GetMapping("/boards/{boardId}/tasks")
    fun listBoardTasks(
        @AuthenticationPrincipal currentUser: CurrentUser,
        @PathVariable boardId: UUID,
    ): List<TaskResponse> = taskService.listBoardTasks(currentUser, boardId)

    @GetMapping("/tasks/{taskId}")
    fun get(
        @AuthenticationPrincipal currentUser: CurrentUser,
        @PathVariable taskId: UUID,
    ): TaskResponse = taskService.get(currentUser, taskId)

    @PatchMapping("/tasks/{taskId}")
    fun update(
        @AuthenticationPrincipal currentUser: CurrentUser,
        @PathVariable taskId: UUID,
        @Valid @RequestBody request: UpdateTaskRequest,
    ): TaskResponse = taskService.update(currentUser, taskId, request)

    @DeleteMapping("/tasks/{taskId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun delete(
        @AuthenticationPrincipal currentUser: CurrentUser,
        @PathVariable taskId: UUID,
    ) {
        taskService.delete(currentUser, taskId)
    }

    @PatchMapping("/tasks/{taskId}/move")
    fun move(
        @AuthenticationPrincipal currentUser: CurrentUser,
        @PathVariable taskId: UUID,
        @RequestBody request: MoveTaskRequest,
    ): TaskResponse = taskService.move(currentUser, taskId, request)

    @PatchMapping("/tasks/{taskId}/complete")
    fun complete(
        @AuthenticationPrincipal currentUser: CurrentUser,
        @PathVariable taskId: UUID,
    ): TaskResponse = taskService.complete(currentUser, taskId)

    @PostMapping("/tasks/{taskId}/assignees")
    fun addAssignee(
        @AuthenticationPrincipal currentUser: CurrentUser,
        @PathVariable taskId: UUID,
        @RequestBody request: AddAssigneeRequest,
    ): TaskResponse = taskService.addAssignee(currentUser, taskId, request)

    @DeleteMapping("/tasks/{taskId}/assignees/{userId}")
    fun removeAssignee(
        @AuthenticationPrincipal currentUser: CurrentUser,
        @PathVariable taskId: UUID,
        @PathVariable userId: UUID,
    ): TaskResponse = taskService.removeAssignee(currentUser, taskId, userId)
}
