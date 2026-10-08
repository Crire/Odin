package br.com.odin.general.group.repository

import br.com.odin.general.group.domain.GroupMember
import br.com.odin.general.group.domain.MemberStatus
import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface GroupMemberRepository : JpaRepository<GroupMember, UUID> {
    fun findByGroup_IdAndUser_Id(groupId: UUID, userId: UUID): GroupMember?

    fun findByIdAndGroup_Id(id: UUID, groupId: UUID): GroupMember?

    fun findAllByGroup_IdOrderByCreatedAtAsc(groupId: UUID): List<GroupMember>

    fun findAllByUser_IdAndStatusOrderByCreatedAtDesc(userId: UUID, status: MemberStatus): List<GroupMember>

    fun existsByGroup_IdAndUser_IdAndStatus(groupId: UUID, userId: UUID, status: MemberStatus): Boolean

    fun countByGroup_IdAndRoleAndStatus(groupId: UUID, role: br.com.odin.general.group.domain.GroupRole, status: MemberStatus): Long
}
