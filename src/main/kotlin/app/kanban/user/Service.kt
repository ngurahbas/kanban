package app.kanban.user

import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Table

@Table
data class Identifier(
    @Id
    val id: Long,
    val type: IdentifierType,
    val value: String
)

enum class IdentifierType {
    EMAIL, PHONE_NUMBER
}
