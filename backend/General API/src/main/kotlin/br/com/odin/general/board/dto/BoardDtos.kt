package br.com.odin.general.board.dto

import br.com.odin.general.board.domain.Board
import br.com.odin.general.board.domain.BoardColumn
import br.com.odin.general.board.domain.ColumnType
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size
import java.time.Instant
import java.util.UUID

data class CreateBoardRequest(
    @field:NotBlank
    @field:Size(max = 160)
    val name: String,
)

data class UpdateBoardRequest(
    @field:NotBlank
    @field:Size(max = 160)
    val name: String,
)

data class CreateColumnRequest(
    @field:NotBlank
    @field:Size(max = 120)
    val name: String,
    val position: Int,
    val type: ColumnType = ColumnType.TODO,
)

data class UpdateColumnRequest(
    @field:NotBlank
    @field:Size(max = 120)
    val name: String,
    val position: Int,
    val type: ColumnType,
)

data class BoardResponse(
    val id: UUID,
    val groupId: UUID,
    val name: String,
    val createdBy: UUID,
    val createdAt: Instant?,
) {
    companion object {
        fun from(board: Board): BoardResponse =
            BoardResponse(
                id = board.id,
                groupId = board.group.id,
                name = board.name,
                createdBy = board.createdBy.id,
                createdAt = board.createdAt,
            )
    }
}

data class ColumnResponse(
    val id: UUID,
    val boardId: UUID,
    val name: String,
    val position: Int,
    val type: ColumnType,
) {
    companion object {
        fun from(column: BoardColumn): ColumnResponse =
            ColumnResponse(
                id = column.id,
                boardId = column.board.id,
                name = column.name,
                position = column.position,
                type = column.type,
            )
    }
}
