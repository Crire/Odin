package br.com.odin.general.board.repository

import br.com.odin.general.board.domain.BoardColumn
import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface BoardColumnRepository : JpaRepository<BoardColumn, UUID> {
    fun findAllByBoard_IdOrderByPositionAsc(boardId: UUID): List<BoardColumn>

    fun findByIdAndBoard_Id(id: UUID, boardId: UUID): BoardColumn?
}
