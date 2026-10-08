package br.com.odin.general.board.service

import br.com.odin.general.board.domain.Board
import br.com.odin.general.board.domain.BoardColumn
import br.com.odin.general.board.domain.ColumnType
import br.com.odin.general.board.dto.BoardResponse
import br.com.odin.general.board.dto.ColumnResponse
import br.com.odin.general.board.dto.CreateBoardRequest
import br.com.odin.general.board.dto.CreateColumnRequest
import br.com.odin.general.board.dto.UpdateBoardRequest
import br.com.odin.general.board.dto.UpdateColumnRequest
import br.com.odin.general.board.repository.BoardColumnRepository
import br.com.odin.general.board.repository.BoardRepository
import br.com.odin.general.common.exception.NotFoundException
import br.com.odin.general.group.domain.OdinGroup
import br.com.odin.general.group.repository.GroupRepository
import br.com.odin.general.group.service.GroupPermissionService
import br.com.odin.general.identity.domain.AppUser
import br.com.odin.general.identity.repository.UserRepository
import br.com.odin.general.identity.service.CurrentUser
import br.com.odin.general.outbox.service.OutboxEventPublisher
import br.com.odin.general.websocket.RealtimeEventPublisher
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
class BoardService(
    private val boardRepository: BoardRepository,
    private val boardColumnRepository: BoardColumnRepository,
    private val groupRepository: GroupRepository,
    private val userRepository: UserRepository,
    private val groupPermissionService: GroupPermissionService,
    private val outboxEventPublisher: OutboxEventPublisher,
    private val realtimeEventPublisher: RealtimeEventPublisher,
) {
    @Transactional
    fun create(currentUser: CurrentUser, groupId: UUID, request: CreateBoardRequest): BoardResponse {
        groupPermissionService.requireEditorOrAdmin(groupId, currentUser.id)
        val group = groupRepository.findById(groupId).orElseThrow { NotFoundException("Grupo") }
        val user = userRepository.findById(currentUser.id).orElseThrow { NotFoundException("Usuario") }
        val board = createBoardWithDefaultColumns(group, user, request.name.trim())
        publishBoardCreated(board)
        return BoardResponse.from(board)
    }

    @Transactional(readOnly = true)
    fun list(currentUser: CurrentUser, groupId: UUID): List<BoardResponse> {
        groupPermissionService.requireActiveMember(groupId, currentUser.id)
        return boardRepository.findAllByGroup_IdOrderByCreatedAtAsc(groupId).map(BoardResponse::from)
    }

    @Transactional(readOnly = true)
    fun get(currentUser: CurrentUser, boardId: UUID): BoardResponse {
        val board = findBoard(boardId)
        groupPermissionService.requireActiveMember(board.group.id, currentUser.id)
        return BoardResponse.from(board)
    }

    @Transactional
    fun update(currentUser: CurrentUser, boardId: UUID, request: UpdateBoardRequest): BoardResponse {
        val board = findBoard(boardId)
        groupPermissionService.requireEditorOrAdmin(board.group.id, currentUser.id)
        board.name = request.name.trim()
        realtimeEventPublisher.publishGroupEvent(board.group.id, "board.updated", BoardResponse.from(board))
        return BoardResponse.from(board)
    }

    @Transactional
    fun createColumn(currentUser: CurrentUser, boardId: UUID, request: CreateColumnRequest): ColumnResponse {
        val board = findBoard(boardId)
        groupPermissionService.requireEditorOrAdmin(board.group.id, currentUser.id)
        val column = boardColumnRepository.save(
            BoardColumn(
                board = board,
                name = request.name.trim(),
                position = request.position,
                type = request.type,
            ),
        )
        realtimeEventPublisher.publishBoardEvent(board.id, "column.created", ColumnResponse.from(column))
        return ColumnResponse.from(column)
    }

    @Transactional(readOnly = true)
    fun listColumns(currentUser: CurrentUser, boardId: UUID): List<ColumnResponse> {
        val board = findBoard(boardId)
        groupPermissionService.requireActiveMember(board.group.id, currentUser.id)
        return boardColumnRepository.findAllByBoard_IdOrderByPositionAsc(boardId).map(ColumnResponse::from)
    }

    @Transactional
    fun updateColumn(currentUser: CurrentUser, columnId: UUID, request: UpdateColumnRequest): ColumnResponse {
        val column = boardColumnRepository.findById(columnId).orElseThrow { NotFoundException("Coluna") }
        groupPermissionService.requireEditorOrAdmin(column.board.group.id, currentUser.id)
        column.name = request.name.trim()
        column.position = request.position
        column.type = request.type
        realtimeEventPublisher.publishBoardEvent(column.board.id, "column.updated", ColumnResponse.from(column))
        return ColumnResponse.from(column)
    }

    @Transactional
    fun deleteColumn(currentUser: CurrentUser, columnId: UUID) {
        val column = boardColumnRepository.findById(columnId).orElseThrow { NotFoundException("Coluna") }
        groupPermissionService.requireEditorOrAdmin(column.board.group.id, currentUser.id)
        boardColumnRepository.delete(column)
        realtimeEventPublisher.publishBoardEvent(column.board.id, "column.deleted", mapOf("columnId" to columnId.toString()))
    }

    @Transactional
    fun createDefaultBoard(group: OdinGroup, createdBy: AppUser): Board =
        createBoardWithDefaultColumns(group, createdBy, "Quadro Principal")

    @Transactional(readOnly = true)
    fun findBoard(boardId: UUID): Board =
        boardRepository.findById(boardId).orElseThrow { NotFoundException("Quadro") }

    @Transactional(readOnly = true)
    fun findColumn(boardId: UUID, columnId: UUID): BoardColumn =
        boardColumnRepository.findByIdAndBoard_Id(columnId, boardId) ?: throw NotFoundException("Coluna")

    private fun createBoardWithDefaultColumns(group: OdinGroup, createdBy: AppUser, name: String): Board {
        val board = boardRepository.save(Board(group = group, name = name, createdBy = createdBy))
        defaultColumns().forEach {
            boardColumnRepository.save(BoardColumn(board = board, name = it.name, position = it.position, type = it.type))
        }
        return board
    }

    private fun publishBoardCreated(board: Board) {
        val payload = mapOf("boardId" to board.id.toString(), "groupId" to board.group.id.toString(), "name" to board.name)
        outboxEventPublisher.publish("BoardCreated", "board", board.id, "board.created", payload)
        realtimeEventPublisher.publishGroupEvent(board.group.id, "board.created", payload)
    }

    private fun defaultColumns(): List<DefaultColumn> =
        listOf(
            DefaultColumn("Backlog", 0, ColumnType.BACKLOG),
            DefaultColumn("A Fazer", 1, ColumnType.TODO),
            DefaultColumn("Fazendo", 2, ColumnType.DOING),
            DefaultColumn("Bloqueado", 3, ColumnType.BLOCKED),
            DefaultColumn("Revisao", 4, ColumnType.REVIEW),
            DefaultColumn("Concluido", 5, ColumnType.DONE),
        )
}

private data class DefaultColumn(
    val name: String,
    val position: Int,
    val type: ColumnType,
)
