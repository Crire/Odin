package br.com.odin.general.board.controller

import br.com.odin.general.board.dto.BoardResponse
import br.com.odin.general.board.dto.CreateBoardRequest
import br.com.odin.general.board.dto.UpdateBoardRequest
import br.com.odin.general.board.service.BoardService
import br.com.odin.general.identity.service.CurrentUser
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.security.core.annotation.AuthenticationPrincipal
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
class BoardController(
    private val boardService: BoardService,
) {
    @PostMapping("/groups/{groupId}/boards")
    @ResponseStatus(HttpStatus.CREATED)
    fun create(
        @AuthenticationPrincipal currentUser: CurrentUser,
        @PathVariable groupId: UUID,
        @Valid @RequestBody request: CreateBoardRequest,
    ): BoardResponse = boardService.create(currentUser, groupId, request)

    @GetMapping("/groups/{groupId}/boards")
    fun list(
        @AuthenticationPrincipal currentUser: CurrentUser,
        @PathVariable groupId: UUID,
    ): List<BoardResponse> = boardService.list(currentUser, groupId)

    @GetMapping("/boards/{boardId}")
    fun get(
        @AuthenticationPrincipal currentUser: CurrentUser,
        @PathVariable boardId: UUID,
    ): BoardResponse = boardService.get(currentUser, boardId)

    @PatchMapping("/boards/{boardId}")
    fun update(
        @AuthenticationPrincipal currentUser: CurrentUser,
        @PathVariable boardId: UUID,
        @Valid @RequestBody request: UpdateBoardRequest,
    ): BoardResponse = boardService.update(currentUser, boardId, request)
}
