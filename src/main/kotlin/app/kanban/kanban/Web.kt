package app.kanban.kanban

import app.kanban.security.KanbanUser
import jakarta.servlet.http.HttpServletResponse
import jakarta.validation.Valid
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size
import org.slf4j.LoggerFactory
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.stereotype.Controller
import org.springframework.ui.Model
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.ResponseBody
import java.math.BigInteger


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
        val columns = service.getColumns(id)
        model.addAttribute("kanbanId", id)
        model.addAttribute("columns", columns)
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

@Controller
class KanbanModifyingController(
    private val service: KanbanService
) {
    companion object {
        private val logger = LoggerFactory.getLogger(KanbanModifyingController::class.java)
    }

    @PostMapping("/kanban/title")
    fun saveKanban(kanban: KanbanWeb): ResponseEntity<String> {
        var id: Long? = kanban.id
        if (kanban.id == null) {
            id = service.createBoard(kanban.title, defaultColumns)
        }
        return ResponseEntity.ok().header("HX-Redirect", "/kanban/${id}").build()
    }

    @PutMapping("/kanban/title")
    fun updateTitle(@AuthenticationPrincipal user: KanbanUser, kanban: KanbanWeb, model: Model): String {
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
    @ResponseBody
    fun deleteCard(
        @AuthenticationPrincipal user: KanbanUser,
        @PathVariable kanbanId: Long,
        @PathVariable cardId: Int
    ): String {
        service.deleteCard(kanbanId, cardId)
        return ""
    }

    @PostMapping("/kanban/{kanbanId}/new-column-after/{refColumn}")
    @PreAuthorize("@kanbanService.hasKanbanAccess(#user.identifierId, #kanbanId)")
    fun addColumn(
        @AuthenticationPrincipal user: KanbanUser,
        @PathVariable kanbanId: Long,
        @PathVariable refColumn: String,
        @RequestParam newColumn: String,
        model: Model
    ): String {
        val columns = service.getColumns(kanbanId)
        val newColumns = arrayListOf<String>()

        for (column in columns) {
            newColumns.add(column)
            if (refColumn == column) {
                newColumns.add(newColumn)
            }
        }
        service.updateColumns(kanbanId, newColumns.toSet())

        val kanbanCardByColumn = service.getCards(kanbanId).groupBy { it.column }
            .mapValues { it.value.map { card -> KanbanCardWeb(card.id, card.index, card.title, card.description) }.sortedBy { it.index} }
        val columnCards = newColumns.associateWith { kanbanCardByColumn[it] ?: listOf() }

        model.addAttribute("columnCards", columnCards)
        model.addAttribute("kanbanId", kanbanId)
        model.addAttribute("swapOob", false)
        return "kanban/columns"
    }

    @DeleteMapping("/kanban/{kanbanId}/delete-column/{column}")
    @PreAuthorize("@kanbanService.hasKanbanAccess(#user.identifierId, #kanbanId)")
    fun deleteColumn(
        @AuthenticationPrincipal user: KanbanUser,
        @PathVariable kanbanId: Long,
        @PathVariable column: String,
        model: Model
    ): String {
        val newColumns = service.deleteColumn(kanbanId, column)

        val kanbanCardByColumn = service.getCards(kanbanId).groupBy { it.column }
            .mapValues { it.value.map { card -> KanbanCardWeb(card.id, card.index, card.title, card.description) }.sortedBy { it.index} }
        val columnCards = newColumns.associateWith { kanbanCardByColumn[it] ?: listOf() }
        model.addAttribute("columnCards", columnCards)
        model.addAttribute("kanbanId", kanbanId)
        model.addAttribute("swapOob", false)
        return "kanban/columns"
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
    @field:NotBlank
    @field:Size(min = 4, max = 256)
    val title: String,
    @field:NotBlank
    @field:Size(min = 4, max = 1024)
    val description: String,
)

enum class CardMovement {
    NEXT, PREV
}

private val base62chars = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz"

fun toBase62(input: String): String {
    val textBytes = input.toByteArray(Charsets.UTF_8)
    var num = BigInteger(1, textBytes)

    if (num == BigInteger.ZERO) {
        return base62chars[0].toString()
    }

    val result = StringBuilder()
    val base = BigInteger.valueOf(62)

    while (num > BigInteger.ZERO) {
        val remainder = num.mod(base).toInt()
        result.append(base62chars[remainder])
        num = num.divide(base)
    }

    return result.reverse().toString()
}
