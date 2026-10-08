package br.com.odin.general.task.service

import br.com.odin.general.audit.service.AuditService
import br.com.odin.general.board.domain.BoardColumn
import br.com.odin.general.board.domain.ColumnType
import br.com.odin.general.board.repository.BoardColumnRepository
import br.com.odin.general.board.service.BoardService
import br.com.odin.general.common.exception.BadRequestException
import br.com.odin.general.common.exception.ConflictException
import br.com.odin.general.common.exception.NotFoundException
import br.com.odin.general.group.service.GroupPermissionService
import br.com.odin.general.identity.repository.UserRepository
import br.com.odin.general.identity.service.CurrentUser
import br.com.odin.general.notification.service.NotificationSchedulingService
import br.com.odin.general.outbox.service.OutboxEventPublisher
import br.com.odin.general.task.domain.TaskAssignee
import br.com.odin.general.task.domain.TaskItem
import br.com.odin.general.task.domain.TaskMovement
import br.com.odin.general.task.domain.TaskStatus
import br.com.odin.general.task.dto.AddAssigneeRequest
import br.com.odin.general.task.dto.CreateBoardTaskRequest
import br.com.odin.general.task.dto.CreatePersonalTaskRequest
import br.com.odin.general.task.dto.MoveTaskRequest
import br.com.odin.general.task.dto.TaskResponse
import br.com.odin.general.task.dto.UpdateTaskRequest
import br.com.odin.general.task.repository.TaskAssigneeRepository
import br.com.odin.general.task.repository.TaskMovementRepository
import br.com.odin.general.task.repository.TaskRepository
import br.com.odin.general.task.validator.TaskPermissionValidator
import br.com.odin.general.websocket.RealtimeEventPublisher
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
class TaskService(
    private val taskRepository: TaskRepository,
    private val taskAssigneeRepository: TaskAssigneeRepository,
    private val taskMovementRepository: TaskMovementRepository,
    private val boardColumnRepository: BoardColumnRepository,
    private val boardService: BoardService,
    private val userRepository: UserRepository,
    private val groupPermissionService: GroupPermissionService,
    private val taskPermissionValidator: TaskPermissionValidator,
    private val notificationSchedulingService: NotificationSchedulingService,
    private val outboxEventPublisher: OutboxEventPublisher,
    private val realtimeEventPublisher: RealtimeEventPublisher,
    private val auditService: AuditService,
) {
    @Transactional
    fun createPersonal(currentUser: CurrentUser, request: CreatePersonalTaskRequest): TaskResponse {
        val user = userRepository.findById(currentUser.id).orElseThrow { NotFoundException("Usuario") }
        val task = taskRepository.save(
            TaskItem(
                title = request.title.trim(),
                description = request.description?.trim(),
                priority = request.priority,
                dueAt = request.dueAt,
                createdBy = user,
            ),
        )
        notificationSchedulingService.scheduleDueReminder(task, user, null)
        publishTaskEvent("TaskCreated", "task.created", task)
        return response(task)
    }

    @Transactional(readOnly = true)
    fun listPersonal(currentUser: CurrentUser): List<TaskResponse> =
        taskRepository.findAllByCreatedBy_IdAndBoardIsNullOrderByDueAtAsc(currentUser.id).map(::response)

    @Transactional
    fun createBoardTask(currentUser: CurrentUser, boardId: UUID, request: CreateBoardTaskRequest): TaskResponse {
        val board = boardService.findBoard(boardId)
        groupPermissionService.requireEditorOrAdmin(board.group.id, currentUser.id)
        val creator = userRepository.findById(currentUser.id).orElseThrow { NotFoundException("Usuario") }
        val column = request.columnId?.let { boardService.findColumn(boardId, it) }
            ?: boardColumnRepository.findAllByBoard_IdOrderByPositionAsc(boardId).firstOrNull()
            ?: throw BadRequestException("Quadro nao possui colunas.")

        val task = taskRepository.save(
            TaskItem(
                board = board,
                column = column,
                title = request.title.trim(),
                description = request.description?.trim(),
                priority = request.priority,
                dueAt = request.dueAt,
                createdBy = creator,
                status = statusFromColumn(column),
            ),
        )
        request.assigneeIds.forEach { addAssigneeInternal(task, it, creator.id) }
        publishTaskEvent("TaskCreated", "task.created", task)
        realtimeEventPublisher.publishBoardEvent(boardId, "card.created", response(task))
        return response(task)
    }

    @Transactional(readOnly = true)
    fun listBoardTasks(currentUser: CurrentUser, boardId: UUID): List<TaskResponse> {
        val board = boardService.findBoard(boardId)
        groupPermissionService.requireActiveMember(board.group.id, currentUser.id)
        return taskRepository.findAllByBoard_IdOrderByCreatedAtDesc(boardId).map(::response)
    }

    @Transactional(readOnly = true)
    fun get(currentUser: CurrentUser, taskId: UUID): TaskResponse {
        val task = findTask(taskId)
        taskPermissionValidator.requireCanView(task, currentUser.id)
        return response(task)
    }

    @Transactional
    fun update(currentUser: CurrentUser, taskId: UUID, request: UpdateTaskRequest): TaskResponse {
        val task = findTask(taskId)
        taskPermissionValidator.requireCanEdit(task, currentUser.id)
        task.title = request.title.trim()
        task.description = request.description?.trim()
        task.priority = request.priority
        task.dueAt = request.dueAt
        publishTaskEvent("TaskUpdated", "task.updated", task)
        task.board?.id?.let { realtimeEventPublisher.publishBoardEvent(it, "card.updated", response(task)) }
        return response(task)
    }

    @Transactional
    fun delete(currentUser: CurrentUser, taskId: UUID) {
        val task = findTask(taskId)
        taskPermissionValidator.requireCanEdit(task, currentUser.id)
        task.status = TaskStatus.CANCELLED
        notificationSchedulingService.cancelPendingForTask(task)
        publishTaskEvent("TaskCancelled", "task.cancelled", task)
        auditService.record(currentUser.id, "TASK_CANCELLED", "task", task.id)
    }

    @Transactional
    fun move(currentUser: CurrentUser, taskId: UUID, request: MoveTaskRequest): TaskResponse {
        val task = findTask(taskId)
        val board = task.board ?: throw BadRequestException("Tarefa pessoal nao pode ser movida entre colunas.")
        taskPermissionValidator.requireCanMove(task, currentUser.id)
        val toColumn = boardService.findColumn(board.id, request.toColumnId)
        val fromColumn = task.column
        task.column = toColumn
        task.status = statusFromColumn(toColumn)
        val actor = userRepository.findById(currentUser.id).orElseThrow { NotFoundException("Usuario") }
        taskMovementRepository.save(TaskMovement(task = task, fromColumn = fromColumn, toColumn = toColumn, movedBy = actor))
        publishTaskEvent("TaskMoved", "task.moved", task)
        realtimeEventPublisher.publishBoardEvent(board.id, "card.moved", response(task))
        auditService.record(
            currentUser.id,
            "TASK_MOVED",
            "task",
            task.id,
            mapOf("fromColumnId" to fromColumn?.id?.toString(), "toColumnId" to toColumn.id.toString()),
        )
        assignees(task).forEach { notificationSchedulingService.scheduleMovement(task, it.user, board.group) }
        return response(task)
    }

    @Transactional
    fun complete(currentUser: CurrentUser, taskId: UUID): TaskResponse {
        val task = findTask(taskId)
        taskPermissionValidator.requireCanComplete(task, currentUser.id)
        task.status = TaskStatus.DONE
        task.board?.id?.let { boardId ->
            boardColumnRepository.findAllByBoard_IdOrderByPositionAsc(boardId).firstOrNull { it.type == ColumnType.DONE }?.let {
                task.column = it
            }
        }
        notificationSchedulingService.cancelPendingForTask(task)
        publishTaskEvent("TaskCompleted", "task.completed", task)
        task.board?.id?.let { realtimeEventPublisher.publishBoardEvent(it, "card.updated", response(task)) }
        auditService.record(currentUser.id, "TASK_COMPLETED", "task", task.id)
        return response(task)
    }

    @Transactional
    fun addAssignee(currentUser: CurrentUser, taskId: UUID, request: AddAssigneeRequest): TaskResponse {
        val task = findTask(taskId)
        taskPermissionValidator.requireCanEdit(task, currentUser.id)
        addAssigneeInternal(task, request.userId, currentUser.id)
        publishTaskEvent("TaskAssigned", "task.assigned", task)
        task.board?.id?.let { realtimeEventPublisher.publishBoardEvent(it, "card.updated", response(task)) }
        return response(task)
    }

    @Transactional
    fun removeAssignee(currentUser: CurrentUser, taskId: UUID, userId: UUID): TaskResponse {
        val task = findTask(taskId)
        taskPermissionValidator.requireCanEdit(task, currentUser.id)
        if (!taskAssigneeRepository.existsByTask_IdAndUser_Id(task.id, userId)) {
            throw NotFoundException("Responsavel")
        }
        taskAssigneeRepository.deleteByTask_IdAndUser_Id(task.id, userId)
        publishTaskEvent("TaskUnassigned", "task.unassigned", task)
        return response(task)
    }

    private fun addAssigneeInternal(task: TaskItem, userId: UUID, assignedById: UUID) {
        if (taskAssigneeRepository.existsByTask_IdAndUser_Id(task.id, userId)) {
            throw ConflictException("Usuario ja esta atribuido a tarefa.")
        }
        val user = userRepository.findById(userId).orElseThrow { NotFoundException("Usuario responsavel") }
        val assignedBy = userRepository.findById(assignedById).orElseThrow { NotFoundException("Usuario") }
        val group = task.board?.group
        if (group != null) {
            groupPermissionService.requireActiveMember(group.id, user.id)
        }
        taskAssigneeRepository.save(TaskAssignee(task = task, user = user, assignedBy = assignedBy))
        notificationSchedulingService.scheduleAssignment(task, user, group)
        notificationSchedulingService.scheduleDueReminder(task, user, group)
    }

    private fun publishTaskEvent(eventType: String, routingKey: String, task: TaskItem) {
        outboxEventPublisher.publish(
            eventType = eventType,
            aggregateType = "task",
            aggregateId = task.id,
            routingKey = routingKey,
            payload = mapOf(
                "taskId" to task.id.toString(),
                "boardId" to task.board?.id?.toString(),
                "groupId" to task.board?.group?.id?.toString(),
                "status" to task.status.name,
            ),
        )
    }

    private fun findTask(taskId: UUID): TaskItem =
        taskRepository.findById(taskId).orElseThrow { NotFoundException("Tarefa") }

    private fun response(task: TaskItem): TaskResponse =
        TaskResponse.from(task, assignees(task).map { it.user.id })

    private fun assignees(task: TaskItem): List<TaskAssignee> =
        taskAssigneeRepository.findAllByTask_Id(task.id)

    private fun statusFromColumn(column: BoardColumn): TaskStatus =
        when (column.type) {
            ColumnType.BACKLOG, ColumnType.TODO -> TaskStatus.PENDING
            ColumnType.DOING -> TaskStatus.IN_PROGRESS
            ColumnType.BLOCKED -> TaskStatus.BLOCKED
            ColumnType.REVIEW -> TaskStatus.REVIEW
            ColumnType.DONE -> TaskStatus.DONE
        }
}
