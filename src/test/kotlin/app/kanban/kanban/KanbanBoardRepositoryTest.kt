package app.kanban.kanban

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import kotlin.test.Test
import kotlin.test.assertEquals

@SpringBootTest
class KanbanBoardRepositoryTest(
    @Autowired private val kanbanBoardRepository: KanbanBoardRepository,
    @Autowired private val kanbanCardRepository: KanbanCardRepository
) {
    @Test
    fun `group by columns query works`() {
        val boardId = kanbanBoardRepository.create("Test Board", arrayOf("testColumn1", "testColumn2"))
        kanbanCardRepository.addCard(boardId, "testCard1", "testDescription1", "testColumn1")
        kanbanCardRepository.addCard(boardId, "testCard2", "testDescription2", "testColumn2")
        kanbanCardRepository.addCard(boardId, "testCard3", "testDescription3", "testColumn1")

        val result = kanbanCardRepository.findColumnsStat(boardId)


        assert(result.size == 2)
        result.find { it.column == "testColumn1" }?.let {
            assertEquals(2, it.count)
        }
        result.find { it.column == "testColumn2" }?.let {
            assertEquals(1, it.count)
        }
    }
}