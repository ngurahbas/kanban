package app.kanban.user

import org.springframework.data.annotation.Id
import org.springframework.data.jdbc.repository.query.Modifying
import org.springframework.data.jdbc.repository.query.Query
import org.springframework.data.relational.core.mapping.Table
import org.springframework.data.repository.CrudRepository

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

interface IdentifierRepository : CrudRepository<Identifier, Long> {

    @Modifying
    @Query("INSERT INTO identifier (type, value) VALUES (:type, :value) ON CONFLICT DO NOTHING")
    fun insertIfNotExist(type: IdentifierType, value: String)
}