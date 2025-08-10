package app.kanban.kanban

import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.assertNotNull
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import kotlin.test.Test


@SpringBootTest
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class KanbanServiceTest @Autowired constructor(
    val service: KanbanService,
    val boardRepository: KanbanBoardRepository,
    val cardRepository: KanbanCardRepository

) {

    @BeforeAll
    fun setup() {
        cardRepository.deleteAll()
        boardRepository.deleteAll()

        val boardId = service.saveBoard(null, "Test Board", listOf("To do", "In progress", "Done"))

        service.addCard(boardId, "Test Card 1", "Test Description 1", "To do")
        service.addCard(boardId, "Test Card 2", "Test Description 2", "In progress")
        service.addCard(boardId, "Test Card 3", "Test Description 3", "In progress")
        service.addCard(boardId, "Test Card 4", "Test Description 4", "Done")
        service.addCard(boardId, "Test Card 5", "Test Description 5", "Done")

    }

    @Test
    fun `get board and get cards`() {
        val board = boardRepository.findAll()[0]

        val board1 = service.getBoard(board.id)
        assertNotNull(board1)

        val card =  service.getCard(board.id, board1.cards[0].id)
        assertNotNull(card) }

    @Test
    fun `return only cards of a given column`() {
        val board = boardRepository.findAll()[0]

        val cards = service.getCards(board.id, "In progress")
        assert(cards.size == 2)
        assert(cards[0].title == "Test Card 2")
        assert(cards[1].title == "Test Card 3")
    }

}