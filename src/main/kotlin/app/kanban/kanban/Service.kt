package app.kanban.kanban

import org.springframework.data.annotation.Id
import org.springframework.data.jdbc.repository.query.Modifying
import org.springframework.data.jdbc.repository.query.Query
import org.springframework.data.relational.core.mapping.MappedCollection
import org.springframework.data.relational.core.mapping.Table
import org.springframework.data.repository.CrudRepository
import org.springframework.data.repository.Repository
import org.springframework.stereotype.Service
import java.time.Instant

@Table
data class KanbanBoard(
    @Id
    val id: Long,
    val title: String,
    val columns: List<String>,
    @MappedCollection(idColumn = "board_id", keyColumn = "index")
    val cards: List<KanbanCard>,
    val createdAt: Instant?,
    val updatedAt: Instant?
)

@Table
data class KanbanCard(
    val boardId: Long,
    val id: Int,
    val title: String,
    val description: String,
    val index: Int,
    val column: String,
    val createdAt: Instant?,
    val updatedAt: Instant?
)

class KanbanCardId(val boardId: Long, val id: Int)

@Service
class KanbanService(
    private val kanbanBoardRepository: KanbanBoardRepository,
    private val kanbanCardRepository: KanbanCardRepository
) {
    fun createBoard(title: String, columns: List<String>): Long {
        val columnsAsArray = columns.toTypedArray()
        return kanbanBoardRepository.create(title, columnsAsArray)
    }

    fun updateBoardTitle(id: Long, title: String) {
        kanbanBoardRepository.updateTitle(id, title)
    }

    fun addCard(boardId: Long, title: String, description: String, column: String): Pair<Int, Int> {
        return kanbanCardRepository.addCard(boardId, title, description, column)
    }

    fun getBoard(id: Long) = kanbanBoardRepository.findById(id)

    fun getCard(boardId: Long, id: Int) = kanbanCardRepository.findById(boardId, id)

    fun getCards(boardId: Long, column: String) = kanbanCardRepository.findKanbanCards(boardId, column)

    fun mapColumnToCards(kanbanBoard: KanbanBoard): Map<String, List<KanbanCard>> {
        val cardsByColumn = kanbanBoard.cards.groupBy { it.column }
            .mapValues { (_, cards) -> cards.sortedBy { it.index } }
        return kanbanBoard.columns.associateWith { column -> cardsByColumn.getOrDefault(column, emptyList()) }
    }

}

interface KanbanBoardRepository : Repository<KanbanBoard, Long> {
    fun findAll(): List<KanbanBoard>
    fun deleteAll()

    @Query("INSERT INTO kanban_board (title, columns) VALUES (:title, :columns) RETURNING id")
    fun create(title: String, columns: Array<String>): Long

    @Query("UPDATE kanban_board SET title = :title, columns = :columns WHERE id = :id RETURNING id")
    fun update(id: Long, title: String, columns: Array<String>): Long

    @Query("UPDATE kanban_board SET title = :title WHERE id = :id")
    @Modifying
    fun updateTitle(id: Long, title: String)

    fun findById(id: Long): KanbanBoard
}

interface KanbanCardRepository : CrudRepository<KanbanCard, Int> {

    @Query(
        """
        INSERT INTO kanban_card (board_id, id, title, description, index, "column") 
        VALUES (
            :boardId,
             (SELECT coalesce(max(id), 0) + 1 FROM kanban_card WHERE board_id = :boardId),
            :title, 
            :description, 
            (SELECT coalesce(max(index), 0) + 1 FROM kanban_card WHERE board_id = :boardId),
            :column) 
        RETURNING id, index
    """
    )
    fun addCard(boardId: Long, title: String, description: String, column: String): Pair<Int, Int>

    @Query("SELECT * FROM kanban_card WHERE board_id = :boardId AND \"column\" = :column ORDER BY index")
    fun findKanbanCards(boardId: Long, column: String): List<KanbanCard>

    @Query("SELECT * FROM kanban_card WHERE board_id = :boardId AND id = :id")
    fun findById(boardId: Long, id: Int): KanbanCard
}
