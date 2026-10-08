package br.com.odin.general.group.controller

import br.com.odin.general.group.dto.CreateGroupRequest
import br.com.odin.general.group.dto.GroupResponse
import br.com.odin.general.group.dto.UpdateGroupRequest
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
@RequestMapping("/api/v1/groups")
class GroupController(
    private val groupService: GroupService,
) {
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    fun create(
        @AuthenticationPrincipal currentUser: CurrentUser,
        @Valid @RequestBody request: CreateGroupRequest,
    ): GroupResponse = groupService.create(currentUser, request)

    @GetMapping
    fun list(@AuthenticationPrincipal currentUser: CurrentUser): List<GroupResponse> =
        groupService.list(currentUser)

    @GetMapping("/{groupId}")
    fun get(
        @AuthenticationPrincipal currentUser: CurrentUser,
        @PathVariable groupId: UUID,
    ): GroupResponse = groupService.get(currentUser, groupId)

    @PatchMapping("/{groupId}")
    fun update(
        @AuthenticationPrincipal currentUser: CurrentUser,
        @PathVariable groupId: UUID,
        @Valid @RequestBody request: UpdateGroupRequest,
    ): GroupResponse = groupService.update(currentUser, groupId, request)

    @DeleteMapping("/{groupId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun delete(
        @AuthenticationPrincipal currentUser: CurrentUser,
        @PathVariable groupId: UUID,
    ) {
        groupService.delete(currentUser, groupId)
    }
}
