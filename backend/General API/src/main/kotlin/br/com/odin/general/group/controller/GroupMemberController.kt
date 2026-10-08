package br.com.odin.general.group.controller

import br.com.odin.general.group.dto.GroupMemberResponse
import br.com.odin.general.group.dto.InviteMemberRequest
import br.com.odin.general.group.dto.UpdateMemberRoleRequest
import br.com.odin.general.group.service.GroupService
import br.com.odin.general.identity.service.CurrentUser
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
@RequestMapping("/api/v1/groups/{groupId}/members")
class GroupMemberController(
    private val groupService: GroupService,
) {
    @PostMapping("/invite")
    @ResponseStatus(HttpStatus.CREATED)
    fun invite(
        @AuthenticationPrincipal currentUser: CurrentUser,
        @PathVariable groupId: UUID,
        @Valid @RequestBody request: InviteMemberRequest,
    ): GroupMemberResponse = groupService.invite(currentUser, groupId, request)

    @PostMapping("/accept")
    fun accept(
        @AuthenticationPrincipal currentUser: CurrentUser,
        @PathVariable groupId: UUID,
    ): GroupMemberResponse = groupService.acceptInvite(currentUser, groupId)

    @GetMapping
    fun list(
        @AuthenticationPrincipal currentUser: CurrentUser,
        @PathVariable groupId: UUID,
    ): List<GroupMemberResponse> = groupService.listMembers(currentUser, groupId)

    @PatchMapping("/{memberId}/role")
    fun updateRole(
        @AuthenticationPrincipal currentUser: CurrentUser,
        @PathVariable groupId: UUID,
        @PathVariable memberId: UUID,
        @RequestBody request: UpdateMemberRoleRequest,
    ): GroupMemberResponse = groupService.updateRole(currentUser, groupId, memberId, request)

    @DeleteMapping("/{memberId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun remove(
        @AuthenticationPrincipal currentUser: CurrentUser,
        @PathVariable groupId: UUID,
        @PathVariable memberId: UUID,
    ) {
        groupService.removeMember(currentUser, groupId, memberId)
    }
}
