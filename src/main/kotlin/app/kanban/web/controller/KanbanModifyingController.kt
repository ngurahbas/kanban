package app.kanban.web.controller

import app.kanban.kanban.KanbanService
import app.kanban.security.KanbanUser
import app.kanban.web.data.CardMovement
import app.kanban.web.data.KanbanCardWeb
import app.kanban.web.data.KanbanWeb
import jakarta.servlet.http.HttpServletResponse
import jakarta.validation.Valid
import org.slf4j.LoggerFactory
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.stereotype.Controller
import org.springframework.ui.Model
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.ResponseBody

@Controller
class KanbanModifyingController(
    private val service: KanbanService
) {
    companion object {
        private val logger = LoggerFactory.getLogger(KanbanModifyingController::class.java)
        private val defaultColumns = setOf("To do", "In progress", "Done")
    }

    @PostMapping("/kanban/title")
    fun saveKanban(@Valid kanban: KanbanWeb): ResponseEntity<String> {
        var id: Long? = kanban.id
        if (kanban.id == null) {
            id = service.createBoard(kanban.title, defaultColumns)
        }
        return ResponseEntity.ok().header("HX-Redirect", "/kanban/${id}").build()
    }

    @PutMapping("/kanban/title")
    fun updateTitle(@AuthenticationPrincipal user: KanbanUser, @Valid kanban: KanbanWeb, model: Model): String {
        service.updateBoardTitle(kanban.id!!, kanban.title)
        val kanbans = service.getKanbans(user.identifierId).map { KanbanWeb(it.id, it.title) }.toList()
        model.addAttribute("user", user)
        model.addAttribute("kanban", kanban)
        model.addAttribute("kanbans", kanbans)

        return "titleUpdate"
    }

    @PostMapping("/kanban/{kanbanId}/column/{column}/card")
    @PreAuthorize("@kanbanService.hasKanbanAccess(#user.identifierId, #kanbanId)")
    fun addCard(
        @AuthenticationPrincipal user: KanbanUser,
        @PathVariable kanbanId: Long,
        @PathVariable column: String,
        @Valid card: KanbanCardWeb,
        model: Model
    ): String {
        service.addCard(kanbanId, card.title, card.description, column)
        val cards = service.getCards(kanbanId, column).map { KanbanCardWeb(it.id, it.index, it.title, it.description) }
        model.addAttribute("kanbanId", kanbanId)
        model.addAttribute("column", column)
        model.addAttribute("cards", cards)
        return "column"
    }

    @PutMapping("/kanban/{kanbanId}/{cardId}")
    @PreAuthorize("@kanbanService.hasKanbanAccess(#user.identifierId, #kanbanId)")
    fun editCard(
        @AuthenticationPrincipal user: KanbanUser,
        @PathVariable kanbanId: Long,
        @PathVariable cardId: Int,
        @Valid card: KanbanCardWeb,
        model: Model
    ): String {
        service.updateCard(kanbanId, cardId, card.title, card.description)
        model.addAttribute("kanbanId", kanbanId)
        model.addAttribute("card", card)
        return "card"
    }

    @PutMapping("/kanban/move/{kanbanId}/{cardId}/{movement}")
    @PreAuthorize("@kanbanService.hasKanbanAccess(#user.identifierId, #kanbanId)")
    fun moveCard(
        @AuthenticationPrincipal user: KanbanUser,
        @PathVariable kanbanId: Long,
        @PathVariable cardId: Int,
        @PathVariable movement: CardMovement,
        model: Model,
        response: HttpServletResponse
    ): String? {
        val columns = service.getColumns(kanbanId)
        val card = service.getCard(kanbanId, cardId)
        val colIdx = columns.indexOf(card.column)
        val newIdx = if (movement == CardMovement.NEXT) colIdx + 1 else colIdx - 1
        if (newIdx < 0 || newIdx > (columns.size - 1)) {
            response.status = HttpServletResponse.SC_BAD_REQUEST
            return null;
        }
        val newColumn = columns.elementAt(newIdx)
        service.moveCard(kanbanId, cardId, newColumn)

        val oldColumnCards = card.column to service.getCards(kanbanId, card.column)
            .map { KanbanCardWeb(it.id, it.index, it.title, it.description) }.sortedBy { it.index }
        val newColumnCards = newColumn to service.getCards(kanbanId, newColumn)
            .map { KanbanCardWeb(it.id, it.index, it.title, it.description) }.sortedBy { it.index }
        val columnCards = mapOf(oldColumnCards, newColumnCards);

        model.addAttribute("kanbanId", kanbanId)
        model.addAttribute("columnCards", columnCards)
        return "cardMoveUpdate"
    }

    @DeleteMapping("/kanban/{kanbanId}/{cardId}")
    @PreAuthorize("@kanbanService.hasKanbanAccess(#user.identifierId, #kanbanId)")
    fun deleteCard(
        @AuthenticationPrincipal user: KanbanUser,
        @PathVariable kanbanId: Long,
        @PathVariable cardId: Int,
        model: Model
    ): String {
        val card = service.getCard(kanbanId, cardId)

        service.deleteCard(kanbanId, cardId)

        val cards = service.getCards(kanbanId, card.column)
            .map { KanbanCardWeb(it.id, it.index, it.title, it.description) }
            .sortedBy { it.index }

        model.addAttribute("kanbanId", kanbanId)
        model.addAttribute("column", card.column)
        model.addAttribute("cards", cards)
        return "column"
    }

    @PostMapping("/kanban/{kanbanId}/new-column-after")
    @PreAuthorize("@kanbanService.hasKanbanAccess(#user.identifierId, #kanbanId)")
    fun addColumn(
        @AuthenticationPrincipal user: KanbanUser,
        @PathVariable kanbanId: Long,
        @RequestParam refColumn: String?,
        @RequestParam newColumn: String,
        model: Model
    ): String {
        val columns = service.getColumns(kanbanId)
        val newColumns = arrayListOf<String>()

        if (refColumn == null) {
            newColumns.add(newColumn)
            newColumns.addAll(columns)
        } else {
            for (column in columns) {
                newColumns.add(column)
                if (refColumn == column) {
                    newColumns.add(newColumn)
                }
            }
        }
        val columnsSet = newColumns.toSet()
        service.updateColumns(kanbanId, columnsSet)

        val columnsStat = service.getColumnsStat(kanbanId)

        model.addAttribute("kanbanId", kanbanId)
        model.addAttribute("columnsStat", columnsStat)
        return "configureColumns"
    }

    @DeleteMapping("/kanban/{kanbanId}/delete-column/{column}")
    @PreAuthorize("@kanbanService.hasKanbanAccess(#user.identifierId, #kanbanId)")
    fun deleteColumn(
        @AuthenticationPrincipal user: KanbanUser,
        @PathVariable kanbanId: Long,
        @PathVariable column: String,
        model: Model
    ): String {
        service.deleteColumn(kanbanId, column)

        val columnsStat = service.getColumnsStat(kanbanId)

        model.addAttribute("kanbanId", kanbanId)
        model.addAttribute("columnsStat", columnsStat)

        return "configureColumns"
    }

    @DeleteMapping("/kanban/{kanbanId}")
    @PreAuthorize("@kanbanService.hasKanbanAccess(#user.identifierId, #kanbanId)")
    @ResponseBody
    fun deleteKanban(@AuthenticationPrincipal user: KanbanUser, @PathVariable kanbanId: Long): String {
        service.deleteBoard(kanbanId)
        return ""
    }
}