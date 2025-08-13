package app.kanban.kanban

import jakarta.servlet.http.HttpServletResponse
import org.springframework.stereotype.Controller
import org.springframework.ui.Model
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping


@Controller
class KanbanController(
    private val service: KanbanService
) {

    @GetMapping("/kanban")
    fun board(model: Model): String {
        model.addAttribute("editKanbanTitle", true)
        model.addAttribute("kanban", KanbanWeb(null, "", listOf()))
        return "kanban"
    }

    @PostMapping("/kanban")
    fun saveKanban(kanban: KanbanWeb, model: Model, response: HttpServletResponse): String {
        val columns = kanban.columns ?: listOf("To do", "In Progress", "Done")
        val returnedId = service.saveBoard(kanban.id, kanban.title, columns)
        model.addAttribute("editKanbanTitle", false)
        model.addAttribute("kanban", KanbanWeb(returnedId, kanban.title, columns))
        response.addHeader("HX-PUSH", "/kanban/${returnedId}")
        return "kanban :: kanbanTitle"
    }
}

data class KanbanWeb(
    val id: Long?,
    val title: String,
    val columns: List<String>?
)