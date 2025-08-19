package app.kanban.kanban

import jakarta.servlet.http.HttpServletResponse
import org.springframework.stereotype.Controller
import org.springframework.ui.Model
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping


@Controller
class KanbanController(
    private val service: KanbanService
) {

    @GetMapping("/kanban")
    fun board(model: Model): String {
        model.addAttribute("editKanbanTitle", true)
        model.addAttribute("kanban", KanbanWeb(null, ""))
        model.addAttribute("columnCards", mapOf<String, List<KanbanCardWeb>>())
        return "kanban"
    }

    @PostMapping("/kanban/title")
    fun saveKanban(kanban: KanbanWeb, model: Model, response: HttpServletResponse): String {
        var id: Long? = kanban.id
        if (kanban.id == null) {
            id = service.createBoard(kanban.title, defaultColumns)
            model.addAttribute("kanbanCreated", true)
            model.addAttribute("columnCards", defaultColumns.associateWith { listOf<KanbanCardWeb>() })
        } else {
            service.updateBoardTitle(kanban.id, kanban.title)
        }
        model.addAttribute("editKanbanTitle", false)
        model.addAttribute("kanban", KanbanWeb(id, kanban.title))
        response.addHeader("HX-PUSH", "/kanban/${id}")
        return "kanban :: kanbanTitle"
    }

    @GetMapping("/kanban/{id}")
    fun getKanbanById(@PathVariable id: Long, model: Model): String {
        val kanbanDb = service.getBoard(id)
        val columnCards = service.mapColumnToCards(kanbanDb).mapValues { (_, cards) ->
            cards.map { KanbanCardWeb(it.id, it.index, it.title, it.description) }
        }
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
        return "kanban :: kanbanTitle"
    }

    @PostMapping("/kanban/{kanbanId}/column/{column}/card")
    fun saveCard(@PathVariable kanbanId: Long, @PathVariable column: String, card: KanbanCardWeb, model: Model): String {
        val cardIdIndex = service.addCard(kanbanId, card.title, card.description, column)
        model.addAttribute("card", KanbanCardWeb(cardIdIndex.first, cardIdIndex.second, card.title, card.description))
        return "kanban :: card"
    }
}

val defaultColumns = listOf("To do", "In progress", "Done")

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