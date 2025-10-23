package app.kanban.kanban

import app.kanban.security.KanbanUser
import org.springframework.cache.annotation.CacheEvict
import org.springframework.cache.annotation.Cacheable
import org.springframework.data.annotation.Id
import org.springframework.data.jdbc.repository.query.Modifying
import org.springframework.data.jdbc.repository.query.Query
import org.springframework.data.relational.core.mapping.Table
import org.springframework.data.repository.CrudRepository
import org.springframework.data.repository.Repository
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Service
import java.time.Instant

@Table
data class KanbanBoard(
    @Id
    val id: Long,
    val title: String,
    val columns: Set<String>,
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

@Table
data class KanbanOwnerShip(
    val identifierId: Long,
    val boardIds: Set<Long>
)

data class CardInsertResult(val id: Int, val index: Int)

@Service
class KanbanService(
    private val kanbanBoardRepository: KanbanBoardRepository,
    private val kanbanCardRepository: KanbanCardRepository,
    private val kanbanOwnershipRepository: KanbanOwnershipRepository
) {
    fun createBoard(title: String, columns: Set<String>): Long {
        val columnsAsArray = columns.toTypedArray()
        val boardId = kanbanBoardRepository.create(title, columnsAsArray)

        val user = SecurityContextHolder.getContext().authentication.principal as KanbanUser
        kanbanOwnershipRepository.addToOwnershipBoardIds(user.identifierId, boardId)

        return boardId
    }

    fun updateBoardTitle(id: Long, title: String) {
        kanbanBoardRepository.updateTitle(id, title)
    }

    fun addCard(boardId: Long, title: String, description: String, column: String): CardInsertResult {
        return kanbanCardRepository.addCard(boardId, title, description, column)
    }

    fun getBoard(id: Long) = kanbanBoardRepository.findById(id)

    fun getCard(boardId: Long, id: Int) = kanbanCardRepository.findById(boardId, id)

    fun getCards(boardId: Long, column: String) = kanbanCardRepository.findKanbanCards(boardId, column)

    fun getCards(boardId: Long) = kanbanCardRepository.findKanbanCards(boardId)

    fun updateCard(kanbanId: Long, cardId: Int, title: String, description: String) {
        kanbanCardRepository.update(kanbanId, cardId, title, description)
    }

    fun moveCard(kanbanId: Long, cardId: Int, column: String): List<KanbanCard> {
        kanbanCardRepository.move(kanbanId, cardId, column)
        return getCards(kanbanId, column)
    }

    fun deleteCard(kanbanId: Long, cardId: Int) {
        kanbanCardRepository.delete(kanbanId, cardId)
    }

    @Cacheable(cacheNames = ["kanbanColumns"], key = "#kanbanId")
    fun getColumns(kanbanId: Long) = kanbanBoardRepository.findColumns(kanbanId)

    @CacheEvict(cacheNames = ["kanbanColumns"], key = "#kanbanId")
    fun updateColumns(kanbanId: Long, columns: Set<String>) = kanbanBoardRepository.updateColumns(kanbanId, columns.toTypedArray())

    @CacheEvict(cacheNames = ["kanbanColumns"], key = "#kanbanId")
    fun deleteColumn(kanbanId: Long, column: String): Set<String> {
        val columns = kanbanBoardRepository.findColumns(kanbanId)
        val updatedColumns = columns.minus(column)
        kanbanBoardRepository.updateColumns(kanbanId, updatedColumns.toTypedArray())
        return updatedColumns;
    }

    fun getKanbans(identifierId: Long) = kanbanBoardRepository.findKanbansByOwnerIdentifierId(identifierId)

    fun hasKanbanAccess(identifierId: Long, kanbanId: Long) = kanbanBoardRepository.findKanbansByOwnerIdentifierId(identifierId)
        .any { it.id == kanbanId}
}

interface KanbanBoardRepository : Repository<KanbanBoard, Long> {

    @Query("INSERT INTO kanban_board (title, columns) VALUES (:title, :columns) RETURNING id")
    fun create(title: String, columns: Array<String>): Long

    @Query("UPDATE kanban_board SET title = :title, columns = :columns WHERE id = :id RETURNING id")
    fun update(id: Long, title: String, columns: Array<String>): Long

    @Query("UPDATE kanban_board SET title = :title WHERE id = :id")
    @Modifying
    fun updateTitle(id: Long, title: String)

    fun findById(id: Long): KanbanBoard

    @Query("SELECT unnest(columns) FROM kanban_board WHERE id = :id")
    fun findColumns(id: Long): Set<String>

    @Modifying
    @Query("UPDATE kanban_board SET columns = :columns WHERE id = :kanbanId")
    fun updateColumns(kanbanId: Long, columns: Array<String>)

    @Query("""
        SELECT k.*
        FROM kanban_ownership o
        JOIN kanban_board k 
        ON o.identifier_id = :identifierId AND k.id = ANY(o.board_ids)
    """
    )
    fun findKanbansByOwnerIdentifierId(identifierId: Long): List<KanbanBoard>
}

interface KanbanCardRepository : CrudRepository<KanbanCard, Long> {

    @Query(
        """
        INSERT INTO kanban_card (board_id, id, title, description, index, "column") 
        VALUES (
            :boardId,
             (SELECT coalesce(max(id), 0) + 1 FROM kanban_card WHERE board_id = :boardId),
            :title, 
            :description, 
            (SELECT coalesce(max(index), 0) + 1 FROM kanban_card WHERE board_id = :boardId AND "column" = :column),
            :column) 
        RETURNING id, index
    """
    )
    fun addCard(boardId: Long, title: String, description: String, column: String): CardInsertResult

    @Query("SELECT * FROM kanban_card WHERE board_id = :boardId AND \"column\" = :column ORDER BY index")
    fun findKanbanCards(boardId: Long, column: String): List<KanbanCard>

    @Query("SELECT * FROM kanban_card WHERE board_id = :boardId")
    fun findKanbanCards(boardId: Long): List<KanbanCard>

    @Query("SELECT * FROM kanban_card WHERE board_id = :boardId AND id = :id")
    fun findById(boardId: Long, id: Int): KanbanCard

    @Modifying
    @Query("UPDATE kanban_card SET title = :title, description = :description WHERE board_id = :boardId AND id = :cardId")
    fun update(boardId: Long, cardId: Int, title: String, description: String)

    @Modifying
    @Query(
        """
        UPDATE kanban_card 
        SET "column" = :column, index = (SELECT coalesce(max(index), 0) FROM kanban_card WHERE board_id = :kanbanId AND "column" = :column) 
        WHERE board_id = :kanbanId AND id = :cardId
    """
    )
    fun move(kanbanId: Long, cardId: Int, column: String)

    @Modifying
    @Query("DELETE FROM kanban_card WHERE board_id = :boardId AND id = :cardId")
    fun delete(boardId: Long, cardId: Int)
}

interface KanbanOwnershipRepository : Repository<KanbanOwnerShip, Long> {

    @Modifying
    @Query("""
        INSERT INTO kanban_ownership (identifier_id, board_ids)
        VALUES (:identifierId, ARRAY[:boardId])
        ON CONFLICT (identifier_id) 
        DO UPDATE SET 
            board_ids = array_append(kanban_ownership.board_ids, :boardId)
    """)
    fun addToOwnershipBoardIds(identifierId: Long, boardId: Long)
}
