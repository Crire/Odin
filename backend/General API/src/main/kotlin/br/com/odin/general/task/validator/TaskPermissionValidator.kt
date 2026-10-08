package br.com.odin.general.task.validator

import br.com.odin.general.common.exception.ForbiddenException
import br.com.odin.general.group.domain.GroupRole
import br.com.odin.general.group.service.GroupPermissionService
import br.com.odin.general.task.domain.TaskItem
import br.com.odin.general.task.repository.TaskAssigneeRepository
import org.springframework.stereotype.Component
import java.util.UUID

@Component
class TaskPermissionValidator(
    private val groupPermissionService: GroupPermissionService,
    private val taskAssigneeRepository: TaskAssigneeRepository,
) {
    fun requireCanView(task: TaskItem, userId: UUID) {
        val groupId = task.board?.group?.id
        if (groupId != null) {
            groupPermissionService.requireActiveMember(groupId, userId)
            return
        }
        if (task.createdBy.id != userId && !taskAssigneeRepository.existsByTask_IdAndUser_Id(task.id, userId)) {
            throw ForbiddenException("Usuario nao pode visualizar esta tarefa.")
        }
    }

    fun requireCanEdit(task: TaskItem, userId: UUID) {
        val groupId = task.board?.group?.id
        if (groupId != null) {
            groupPermissionService.requireEditorOrAdmin(groupId, userId)
            return
        }
        if (task.createdBy.id != userId) {
            throw ForbiddenException("Somente o criador pode editar esta tarefa pessoal.")
        }
    }

    fun requireCanMove(task: TaskItem, userId: UUID) {
        val groupId = task.board?.group?.id
        if (groupId == null) {
            if (task.createdBy.id != userId) throw ForbiddenException()
            return
        }
        val member = groupPermissionService.requireActiveMember(groupId, userId)
        if (member.role in setOf(GroupRole.ADMIN, GroupRole.EDITOR)) {
            return
        }
        if (member.role == GroupRole.MEMBER && taskAssigneeRepository.existsByTask_IdAndUser_Id(task.id, userId)) {
            return
        }
        throw ForbiddenException("Membro comum so pode mover cards atribuidos a ele.")
    }

    fun requireCanComplete(task: TaskItem, userId: UUID) {
        val groupId = task.board?.group?.id
        if (groupId == null) {
            if (task.createdBy.id != userId) throw ForbiddenException()
            return
        }
        val member = groupPermissionService.requireActiveMember(groupId, userId)
        if (member.role in setOf(GroupRole.ADMIN, GroupRole.EDITOR)) {
            return
        }
        if (taskAssigneeRepository.existsByTask_IdAndUser_Id(task.id, userId)) {
            return
        }
        throw ForbiddenException("Usuario nao pode concluir esta tarefa.")
    }
}
