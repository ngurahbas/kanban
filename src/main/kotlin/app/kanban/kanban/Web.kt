package app.kanban.kanban

import jakarta.servlet.http.HttpServletResponse
import org.springframework.stereotype.Controller
import org.springframework.ui.Model
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping


@Controller
class KanbanController(
    private val service: KanbanService
) {

    @GetMapping("/kanban")
    fun board(model: Model): String {
        model.addAttribute("editKanbanTitle", true)
        model.addAttribute("kanban", KanbanWeb(null, "" ))
        return "kanban"
    }

    @PostMapping("/kanban/title")
    fun saveKanban(kanban: KanbanWeb, model: Model, response: HttpServletResponse): String {
        var id : Long? = kanban.id
        if (kanban.id == null) {
            val columns =  defaultColumns
            id = service.createBoard(kanban.title, columns)
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
        model.addAttribute("editKanbanTitle", false)
        model.addAttribute("kanban", KanbanWeb(kanbanDb.id, kanbanDb.title))
        return "kanban"
    }

    @GetMapping("/kanban/{id}/edit")
    fun editKanbanById(@PathVariable id: Long, kanban: KanbanWeb, model: Model): String {
        model.addAttribute("editKanbanTitle", true)
        model.addAttribute("kanban", kanban)
        return "kanban :: kanbanTitle"
    }
}

val defaultColumns = listOf("To do", "In progress", "Done")

data class KanbanWeb(
    val id: Long?,
    val title: String,
)