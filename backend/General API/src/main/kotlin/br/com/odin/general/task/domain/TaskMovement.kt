package br.com.odin.general.task.domain

import br.com.odin.general.board.domain.BoardColumn
import br.com.odin.general.identity.domain.AppUser
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import org.hibernate.annotations.CreationTimestamp
import java.time.Instant
import java.util.UUID

@Entity
@Table(name = "task_movements")
class TaskMovement(
    @Id
    @Column(nullable = false, updatable = false)
    var id: UUID = UUID.randomUUID(),

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "task_id", nullable = false)
    var task: TaskItem = TaskItem(),

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "from_column_id")
    var fromColumn: BoardColumn? = null,

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "to_column_id", nullable = false)
    var toColumn: BoardColumn = BoardColumn(),

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "moved_by", nullable = false)
    var movedBy: AppUser = AppUser(),

    @CreationTimestamp
    @Column(name = "moved_at", nullable = false, updatable = false)
    var movedAt: Instant? = null,
)
