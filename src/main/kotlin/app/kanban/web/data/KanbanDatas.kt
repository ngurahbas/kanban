package app.kanban.web.data

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size

data class KanbanWeb(
    val id: Long?,
    @field:NotBlank
    @field:Size(min = 4, max = 128)
    val title: String,
)

data class KanbanCardWeb(
    val id: Int?,
    val index: Int?,
    @field:NotBlank
    @field:Size(min = 4, max = 128)
    val title: String,
    @field:NotBlank
    @field:Size(min = 4, max = 1024)
    val description: String,
)

enum class CardMovement {
    NEXT, PREV
}
