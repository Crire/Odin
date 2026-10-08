package br.com.odin.general.group.service

import br.com.odin.general.common.exception.ForbiddenException
import br.com.odin.general.common.exception.NotFoundException
import br.com.odin.general.group.domain.GroupMember
import br.com.odin.general.group.domain.GroupRole
import br.com.odin.general.group.domain.MemberStatus
import br.com.odin.general.group.repository.GroupMemberRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
class GroupPermissionService(
    private val groupMemberRepository: GroupMemberRepository,
) {
    @Transactional(readOnly = true)
    fun requireActiveMember(groupId: UUID, userId: UUID): GroupMember {
        val member = groupMemberRepository.findByGroup_IdAndUser_Id(groupId, userId)
            ?: throw NotFoundException("Membro do grupo")
        if (member.status != MemberStatus.ACTIVE) {
            throw ForbiddenException("Usuario nao esta ativo no grupo.")
        }
        return member
    }

    @Transactional(readOnly = true)
    fun requireAdmin(groupId: UUID, userId: UUID): GroupMember {
        val member = requireActiveMember(groupId, userId)
        if (member.role != GroupRole.ADMIN) {
            throw ForbiddenException("Somente administradores podem executar esta acao.")
        }
        return member
    }

    @Transactional(readOnly = true)
    fun requireEditorOrAdmin(groupId: UUID, userId: UUID): GroupMember {
        val member = requireActiveMember(groupId, userId)
        if (member.role !in setOf(GroupRole.ADMIN, GroupRole.EDITOR)) {
            throw ForbiddenException("Somente administradores ou editores podem executar esta acao.")
        }
        return member
    }
}
