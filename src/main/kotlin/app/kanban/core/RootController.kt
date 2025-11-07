package app.kanban.core

import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.GetMapping

@Controller
class RootController {

    @GetMapping("/")
    fun redirectToKanban() = "redirect:/kanban"
}