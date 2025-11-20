package app.kanban.core

import app.kanban.security.KanbanUser
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.security.core.annotation.AuthenticationPrincipal

@Controller
class RootController {

    @GetMapping("/")
    fun root(@AuthenticationPrincipal user: KanbanUser?): String {
        // If authenticated, go straight to kanban
        if (user != null) {
            return "redirect:/kanban"
        }
        // Otherwise render a simple SEO-friendly page that indicates redirect to /login
        return "root"
    }
}