package app.kanban.web.controller

import app.kanban.kanban.KanbanService
import app.kanban.security.KanbanUser
import app.kanban.web.data.KanbanCardWeb
import app.kanban.web.data.KanbanWeb
import org.slf4j.LoggerFactory
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.stereotype.Controller
import org.springframework.ui.Model
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable

@Controller
class KanbanController(
    private val service: KanbanService
) {
    companion object {
        private val logger = LoggerFactory.getLogger(KanbanController::class.java)
    }

    @GetMapping("/kanban")
    fun board(@AuthenticationPrincipal user: KanbanUser, model: Model): String {
        val kanbans = service.getKanbans(user.identifierId).map { KanbanWeb(it.id, it.title) }.toList()

        model.addAttribute("kanban", KanbanWeb(null, ""))
        model.addAttribute("kanbans", kanbans)
        model.addAttribute("columnCards", mapOf<String, List<KanbanCardWeb>>())
        model.addAttribute("user", user)

        return "kanban"
    }

    @GetMapping("/kanban/{id}")
    @PreAuthorize("@kanbanService.hasKanbanAccess(#user.identifierId, #id)")
    fun getKanbanById(@AuthenticationPrincipal user: KanbanUser, @PathVariable id: Long, model: Model): String {
        val kanbanDb = service.getBoard(id)
        val kanbanCardByColumn = service.getCards(id).groupBy { it.column }
            .mapValues { it.value.map { card -> KanbanCardWeb(card.id, card.index, card.title, card.description) }.sortedBy { it.index} }
        val columnCards = kanbanDb.columns.associateWith { kanbanCardByColumn[it] ?: listOf() }
        val kanbans = service.getKanbans(user.identifierId).map { KanbanWeb(it.id, it.title) }.toList()

        model.addAttribute("kanban", KanbanWeb(kanbanDb.id, kanbanDb.title))
        model.addAttribute("kanbans", kanbans)
        model.addAttribute("columnCards", columnCards)
        model.addAttribute("user", user)

        return "kanban"
    }

    @GetMapping("/kanban/{id}/configure-columns")
    @PreAuthorize("@kanbanService.hasKanbanAccess(#user.identifierId, #id)")
    fun configureColumns(@AuthenticationPrincipal user: KanbanUser, @PathVariable id: Long, model: Model): String {
        val columnsStat = service.getColumnsStat(id)
        model.addAttribute("kanbanId", id)
        model.addAttribute("columnsStat", columnsStat)
        return "configureColumns"
    }

    @GetMapping("/kanban/{id}/columns")
    @PreAuthorize("@kanbanService.hasKanbanAccess(#user.identifierId, #id)")
    fun getColumns(@AuthenticationPrincipal user: KanbanUser, @PathVariable id: Long, model: Model): String {
        val kanbanDb = service.getBoard(id)
        val kanbanCardByColumn = service.getCards(id).groupBy { it.column }
            .mapValues { it.value.map { card -> KanbanCardWeb(card.id, card.index, card.title, card.description) }.sortedBy { it.index} }
        val columnCards = kanbanDb.columns.associateWith { kanbanCardByColumn[it] ?: listOf() }

        model.addAttribute("kanbanId", id)
        model.addAttribute("columnCards", columnCards)

        return "columns"
    }
}