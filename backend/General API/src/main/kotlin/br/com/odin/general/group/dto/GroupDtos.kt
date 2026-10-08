package br.com.odin.general.group.dto

import br.com.odin.general.group.domain.GroupMember
import br.com.odin.general.group.domain.GroupRole
import br.com.odin.general.group.domain.MemberStatus
import br.com.odin.general.group.domain.OdinGroup
import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size
import java.time.Instant
import java.util.UUID

data class CreateGroupRequest(
    @field:NotBlank
    @field:Size(max = 160)
    val name: String,

    @field:Size(max = 4000)
    val description: String? = null,
)

data class UpdateGroupRequest(
    @field:NotBlank
    @field:Size(max = 160)
    val name: String,

    @field:Size(max = 4000)
    val description: String? = null,
)

data class InviteMemberRequest(
    @field:Email
    @field:NotBlank
    val email: String,
    val role: GroupRole = GroupRole.MEMBER,
)

data class UpdateMemberRoleRequest(
    val role: GroupRole,
)

data class GroupResponse(
    val id: UUID,
    val name: String,
    val description: String?,
    val ownerId: UUID,
    val createdAt: Instant?,
) {
    companion object {
        fun from(group: OdinGroup): GroupResponse =
            GroupResponse(
                id = group.id,
                name = group.name,
                description = group.description,
                ownerId = group.owner.id,
                createdAt = group.createdAt,
            )
    }
}

data class GroupMemberResponse(
    val id: UUID,
    val groupId: UUID,
    val userId: UUID,
    val name: String,
    val email: String,
    val role: GroupRole,
    val status: MemberStatus,
    val joinedAt: Instant?,
) {
    companion object {
        fun from(member: GroupMember): GroupMemberResponse =
            GroupMemberResponse(
                id = member.id,
                groupId = member.group.id,
                userId = member.user.id,
                name = member.user.name,
                email = member.user.email,
                role = member.role,
                status = member.status,
                joinedAt = member.joinedAt,
            )
    }
}
