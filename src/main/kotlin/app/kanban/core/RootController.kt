package app.kanban.core

import app.kanban.security.KanbanUser
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.security.core.annotation.AuthenticationPrincipal

@Controller
class RootController {

    @GetMapping("/")
    fun root(@AuthenticationPrincipal user: KanbanUser?): String {
        if (user != null) {
            return "redirect:/kanban"
        }
        return "root"
    }
}