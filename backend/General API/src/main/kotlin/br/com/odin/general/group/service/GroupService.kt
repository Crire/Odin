package br.com.odin.general.group.service

import br.com.odin.general.audit.service.AuditService
import br.com.odin.general.board.service.BoardService
import br.com.odin.general.common.exception.BadRequestException
import br.com.odin.general.common.exception.ConflictException
import br.com.odin.general.common.exception.NotFoundException
import br.com.odin.general.group.domain.GroupMember
import br.com.odin.general.group.domain.GroupRole
import br.com.odin.general.group.domain.MemberStatus
import br.com.odin.general.group.domain.OdinGroup
import br.com.odin.general.group.dto.CreateGroupRequest
import br.com.odin.general.group.dto.GroupMemberResponse
import br.com.odin.general.group.dto.GroupResponse
import br.com.odin.general.group.dto.InviteMemberRequest
import br.com.odin.general.group.dto.UpdateGroupRequest
import br.com.odin.general.group.dto.UpdateMemberRoleRequest
import br.com.odin.general.group.repository.GroupMemberRepository
import br.com.odin.general.group.repository.GroupRepository
import br.com.odin.general.identity.repository.UserRepository
import br.com.odin.general.identity.service.CurrentUser
import br.com.odin.general.outbox.service.OutboxEventPublisher
import br.com.odin.general.websocket.RealtimeEventPublisher
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Clock
import java.time.Instant
import java.util.UUID

@Service
class GroupService(
    private val groupRepository: GroupRepository,
    private val groupMemberRepository: GroupMemberRepository,
    private val userRepository: UserRepository,
    private val groupPermissionService: GroupPermissionService,
    private val boardService: BoardService,
    private val outboxEventPublisher: OutboxEventPublisher,
    private val realtimeEventPublisher: RealtimeEventPublisher,
    private val auditService: AuditService,
    private val clock: Clock,
) {
    @Transactional
    fun create(currentUser: CurrentUser, request: CreateGroupRequest): GroupResponse {
        val owner = userRepository.findById(currentUser.id).orElseThrow { NotFoundException("Usuario") }
        val group = groupRepository.save(
            OdinGroup(
                name = request.name.trim(),
                description = request.description?.trim(),
                owner = owner,
            ),
        )
        groupMemberRepository.save(
            GroupMember(
                group = group,
                user = owner,
                role = GroupRole.ADMIN,
                status = MemberStatus.ACTIVE,
                joinedAt = Instant.now(clock),
            ),
        )
        boardService.createDefaultBoard(group, owner)
        outboxEventPublisher.publish(
            eventType = "GroupCreated",
            aggregateType = "group",
            aggregateId = group.id,
            routingKey = "group.created",
            payload = mapOf("groupId" to group.id.toString(), "ownerId" to owner.id.toString()),
        )
        auditService.record(currentUser.id, "GROUP_CREATED", "group", group.id)
        return GroupResponse.from(group)
    }

    @Transactional(readOnly = true)
    fun list(currentUser: CurrentUser): List<GroupResponse> =
        groupMemberRepository
            .findAllByUser_IdAndStatusOrderByCreatedAtDesc(currentUser.id, MemberStatus.ACTIVE)
            .map { GroupResponse.from(it.group) }

    @Transactional(readOnly = true)
    fun get(currentUser: CurrentUser, groupId: UUID): GroupResponse {
        groupPermissionService.requireActiveMember(groupId, currentUser.id)
        return GroupResponse.from(groupRepository.findById(groupId).orElseThrow { NotFoundException("Grupo") })
    }

    @Transactional
    fun update(currentUser: CurrentUser, groupId: UUID, request: UpdateGroupRequest): GroupResponse {
        groupPermissionService.requireAdmin(groupId, currentUser.id)
        val group = groupRepository.findById(groupId).orElseThrow { NotFoundException("Grupo") }
        group.name = request.name.trim()
        group.description = request.description?.trim()
        realtimeEventPublisher.publishGroupEvent(group.id, "group.updated", GroupResponse.from(group))
        auditService.record(currentUser.id, "GROUP_UPDATED", "group", group.id)
        return GroupResponse.from(group)
    }

    @Transactional
    fun delete(currentUser: CurrentUser, groupId: UUID) {
        groupPermissionService.requireAdmin(groupId, currentUser.id)
        val group = groupRepository.findById(groupId).orElseThrow { NotFoundException("Grupo") }
        groupRepository.delete(group)
        auditService.record(currentUser.id, "GROUP_DELETED", "group", groupId)
    }

    @Transactional
    fun invite(currentUser: CurrentUser, groupId: UUID, request: InviteMemberRequest): GroupMemberResponse {
        groupPermissionService.requireAdmin(groupId, currentUser.id)
        if (request.role == GroupRole.ADMIN) {
            throw BadRequestException("Convites nao podem conceder ADMIN diretamente.")
        }
        val group = groupRepository.findById(groupId).orElseThrow { NotFoundException("Grupo") }
        val user = userRepository.findByEmailIgnoreCase(request.email.trim()).orElseThrow { NotFoundException("Usuario convidado") }
        val existing = groupMemberRepository.findByGroup_IdAndUser_Id(groupId, user.id)
        if (existing?.status == MemberStatus.ACTIVE || existing?.status == MemberStatus.PENDING) {
            throw ConflictException("Usuario ja faz parte do grupo ou possui convite pendente.")
        }
        val member = groupMemberRepository.save(
            GroupMember(
                group = group,
                user = user,
                role = request.role,
                status = MemberStatus.PENDING,
            ),
        )
        outboxEventPublisher.publish(
            "GroupMemberInvited",
            "group",
            groupId,
            "notification.group.member_invited",
            mapOf("groupId" to groupId.toString(), "userId" to user.id.toString(), "memberId" to member.id.toString()),
        )
        auditService.record(currentUser.id, "GROUP_MEMBER_INVITED", "group", groupId, mapOf("userId" to user.id.toString()))
        return GroupMemberResponse.from(member)
    }

    @Transactional
    fun acceptInvite(currentUser: CurrentUser, groupId: UUID): GroupMemberResponse {
        val member = groupMemberRepository.findByGroup_IdAndUser_Id(groupId, currentUser.id)
            ?: throw NotFoundException("Convite")
        if (member.status != MemberStatus.PENDING) {
            throw BadRequestException("Convite nao esta pendente.")
        }
        member.status = MemberStatus.ACTIVE
        member.joinedAt = Instant.now(clock)
        realtimeEventPublisher.publishGroupEvent(groupId, "member.joined", GroupMemberResponse.from(member))
        return GroupMemberResponse.from(member)
    }

    @Transactional(readOnly = true)
    fun listMembers(currentUser: CurrentUser, groupId: UUID): List<GroupMemberResponse> {
        groupPermissionService.requireActiveMember(groupId, currentUser.id)
        return groupMemberRepository.findAllByGroup_IdOrderByCreatedAtAsc(groupId).map(GroupMemberResponse::from)
    }

    @Transactional
    fun updateRole(
        currentUser: CurrentUser,
        groupId: UUID,
        memberId: UUID,
        request: UpdateMemberRoleRequest,
    ): GroupMemberResponse {
        groupPermissionService.requireAdmin(groupId, currentUser.id)
        val member = groupMemberRepository.findByIdAndGroup_Id(memberId, groupId) ?: throw NotFoundException("Membro")
        ensureNotRemovingLastAdmin(member, request.role)
        member.role = request.role
        realtimeEventPublisher.publishGroupEvent(groupId, "member.role_changed", GroupMemberResponse.from(member))
        auditService.record(currentUser.id, "GROUP_MEMBER_ROLE_CHANGED", "group", groupId, mapOf("memberId" to memberId.toString()))
        return GroupMemberResponse.from(member)
    }

    @Transactional
    fun removeMember(currentUser: CurrentUser, groupId: UUID, memberId: UUID) {
        groupPermissionService.requireAdmin(groupId, currentUser.id)
        val member = groupMemberRepository.findByIdAndGroup_Id(memberId, groupId) ?: throw NotFoundException("Membro")
        ensureNotRemovingLastAdmin(member, GroupRole.MEMBER)
        member.status = MemberStatus.REMOVED
        realtimeEventPublisher.publishGroupEvent(groupId, "member.removed", mapOf("memberId" to memberId.toString()))
        auditService.record(currentUser.id, "GROUP_MEMBER_REMOVED", "group", groupId, mapOf("memberId" to memberId.toString()))
    }

    private fun ensureNotRemovingLastAdmin(member: GroupMember, targetRole: GroupRole) {
        if (member.role == GroupRole.ADMIN && targetRole != GroupRole.ADMIN) {
            val admins = groupMemberRepository.countByGroup_IdAndRoleAndStatus(member.group.id, GroupRole.ADMIN, MemberStatus.ACTIVE)
            if (admins <= 1) {
                throw BadRequestException("O grupo deve manter pelo menos um administrador ativo.")
            }
        }
    }
}
