package app.kanban.kanban

import jakarta.servlet.http.HttpServletResponse
import org.springframework.stereotype.Controller
import org.springframework.ui.Model
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.ResponseBody


@Controller
class KanbanController(
    private val service: KanbanService
) {

    @GetMapping("/kanban")
    fun board(model: Model): String {
        model.addAttribute("editKanbanTitle", true)
        model.addAttribute("kanban", KanbanWeb(null, ""))
        model.addAttribute("columnCards", mapOf<String, List<KanbanCardWeb>>())
        model.addAttribute("kanbanCreated", false)
        return "kanban"
    }

    @PostMapping("/kanban/title")
    fun saveKanban(kanban: KanbanWeb, model: Model, response: HttpServletResponse): String {
        var id: Long? = kanban.id
        var page = "kanban/kanbanTitle"
        if (kanban.id == null) {
            id = service.createBoard(kanban.title, defaultColumns)
            model.addAttribute("kanbanCreated", true)
            model.addAttribute("columnCards", defaultColumns.associateWith { listOf<KanbanCardWeb>() })
            page = "kanban/kanbanTitleWithColumns"
        } else {
            service.updateBoardTitle(kanban.id, kanban.title)
            model.addAttribute("kanbanCreated", false)
        }
        model.addAttribute("editKanbanTitle", false)
        model.addAttribute("kanban", KanbanWeb(id, kanban.title))
        response.addHeader("HX-PUSH", "/kanban/${id}")
        return page
    }

    @GetMapping("/kanban/{id}")
    fun getKanbanById(@PathVariable id: Long, model: Model): String {
        val kanbanDb = service.getBoard(id)
        val kanbanCardByColumn = service.getCards(id).groupBy { it.column }
            .mapValues { it.value.map { card -> KanbanCardWeb(card.id, card.index, card.title, card.description) }.sortedBy { it.index} }
        val columnCards = kanbanDb.columns.associateWith { kanbanCardByColumn[it] ?: listOf() }
        model.addAttribute("kanbanCreated", false)
        model.addAttribute("editKanbanTitle", false)
        model.addAttribute("kanban", KanbanWeb(kanbanDb.id, kanbanDb.title))
        model.addAttribute("columnCards", columnCards)
        return "kanban"
    }

    @GetMapping("/kanban/{id}/edit")
    fun editKanbanById(@PathVariable id: Long, kanban: KanbanWeb, model: Model): String {
        model.addAttribute("editKanbanTitle", true)
        model.addAttribute("kanban", kanban)
        model.addAttribute("kanbanCreated", false)
        return "kanban/kanbanTitle"
    }

    @GetMapping("/kanban/{kanbanId}/column/{column}/card")
    fun addCard(
        @PathVariable kanbanId: Long,
        @PathVariable column: String,
        @RequestParam columns: Set<String>,
        model: Model
    ): String {
        model.addAttribute("kanbanId", kanbanId)
        model.addAttribute("column", column)
        model.addAttribute("columns", columns)
        return "kanban/cardModal"
    }

    @PostMapping("/kanban/{kanbanId}/column/{column}/card")
    fun addCard(
        @PathVariable kanbanId: Long,
        @PathVariable column: String,
        @RequestParam columns: Set<String>,
        card: KanbanCardWeb,
        model: Model
    ): String {
        val cardIdIndex = service.addCard(kanbanId, card.title, card.description, column)
        model.addAttribute("card", KanbanCardWeb(cardIdIndex.id, cardIdIndex.index, card.title, card.description))
        model.addAttribute("kanbanId", kanbanId)
        model.addAttribute("column", column)
        model.addAttribute("columns", columns)
        model.addAttribute("closeModal", true)
        return "kanban/card"
    }

    @GetMapping("/kanban/{kanbanId}/column/{column}/card/{cardId}")
    fun editCard(
        @PathVariable kanbanId: Long,
        @PathVariable column: String,
        @PathVariable cardId: Int,
        @RequestParam columns: Set<String>,
        model: Model
    ): String {
        val card = service.getCard(kanbanId, cardId)
        model.addAttribute("kanbanId", kanbanId)
        model.addAttribute("column", column)
        model.addAttribute("columns", columns)
        model.addAttribute("card", KanbanCardWeb(card.id, card.index, card.title, card.description))
        return "kanban/cardModal"
    }

    @PutMapping("/kanban/{kanbanId}/column/{column}/card/{cardId}")
    fun editCard(
        @PathVariable kanbanId: Long,
        @PathVariable column: String,
        @PathVariable cardId: Int,
        @RequestParam columns: Set<String>,
        card: KanbanCardWeb,
        model: Model
    ): String {
        service.updateCard(kanbanId, cardId, card.title, card.description)
        model.addAttribute("card", card)
        model.addAttribute("kanbanId", kanbanId)
        model.addAttribute("column", column)
        model.addAttribute("columns", columns)
        model.addAttribute("closeModal", true)
        return "kanban/card"
    }

    @PutMapping("/kanban/move/{kanbanId}/{cardId}/{column}")
    fun moveCard(
        @PathVariable kanbanId: Long,
        @PathVariable cardId: Int,
        @PathVariable column: String,
        model: Model
    ): String {
        val cards = service.moveCard(kanbanId, cardId, column)
        .map { KanbanCardWeb(it.id, it.index, it.title, it.description) }
        val columns = service.getColumns(kanbanId)
        model.addAttribute("cards", cards)
        model.addAttribute("column", column)
        model.addAttribute("columns", columns)
        model.addAttribute("kanbanId", kanbanId)
        return "kanban/column"
    }

    @DeleteMapping("/kanban/{kanbanId}/column/{column}/card/{cardId}")
    @ResponseBody
    fun deleteCard(
        @PathVariable kanbanId: Long,
        @PathVariable column: String,
        @PathVariable cardId: Int
    ): String {
        service.deleteCard(kanbanId, cardId)
        return ""
    }
}

val defaultColumns = setOf("To do", "In progress", "Done")

data class KanbanWeb(
    val id: Long?,
    val title: String,
)

data class KanbanCardWeb(
    val id: Int?,
    val index: Int?,
    val title: String,
    val description: String,
)