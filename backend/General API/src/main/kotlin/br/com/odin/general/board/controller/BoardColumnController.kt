package br.com.odin.general.board.controller

import br.com.odin.general.board.dto.ColumnResponse
import br.com.odin.general.board.dto.CreateColumnRequest
import br.com.odin.general.board.dto.UpdateColumnRequest
import br.com.odin.general.board.service.BoardService
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
@RequestMapping("/api/v1")
class BoardColumnController(
    private val boardService: BoardService,
) {
    @PostMapping("/boards/{boardId}/columns")
    @ResponseStatus(HttpStatus.CREATED)
    fun create(
        @AuthenticationPrincipal currentUser: CurrentUser,
        @PathVariable boardId: UUID,
        @Valid @RequestBody request: CreateColumnRequest,
    ): ColumnResponse = boardService.createColumn(currentUser, boardId, request)

    @GetMapping("/boards/{boardId}/columns")
    fun list(
        @AuthenticationPrincipal currentUser: CurrentUser,
        @PathVariable boardId: UUID,
    ): List<ColumnResponse> = boardService.listColumns(currentUser, boardId)

    @PatchMapping("/columns/{columnId}")
    fun update(
        @AuthenticationPrincipal currentUser: CurrentUser,
        @PathVariable columnId: UUID,
        @Valid @RequestBody request: UpdateColumnRequest,
    ): ColumnResponse = boardService.updateColumn(currentUser, columnId, request)

    @DeleteMapping("/columns/{columnId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun delete(
        @AuthenticationPrincipal currentUser: CurrentUser,
        @PathVariable columnId: UUID,
    ) {
        boardService.deleteColumn(currentUser, columnId)
    }
}
