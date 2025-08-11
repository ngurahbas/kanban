package app.kanban.kanban

import org.springframework.stereotype.Controller
import org.springframework.ui.Model
import org.springframework.web.bind.annotation.GetMapping


@Controller
class KanbanController {

    @GetMapping("/kanban")
    fun board(model: Model): String {
        model.addAttribute("kanban", KanbanWeb(null, "", listOf("To Do", "In Progress", "Done")))
        return "kanban"
    }
}

data class KanbanWeb(
    val id: Long?,
    val title: String,
    val columns: List<String>
)