package br.com.odin.general.board.repository

import br.com.odin.general.board.domain.Board
import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface BoardRepository : JpaRepository<Board, UUID> {
    fun findAllByGroup_IdOrderByCreatedAtAsc(groupId: UUID): List<Board>
}
