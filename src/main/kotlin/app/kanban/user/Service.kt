package app.kanban.user

import org.springframework.data.annotation.Id
import org.springframework.data.jdbc.repository.query.Query
import org.springframework.data.relational.core.mapping.Table
import org.springframework.data.repository.CrudRepository

@Table
data class Identifier(
    @Id val id: Long, val type: IdentifierType, val value: String
)

enum class IdentifierType {
    EMAIL, PHONE_NUMBER
}

interface IdentifierRepository : CrudRepository<Identifier, Long> {

    @Query(
        """
        WITH ins AS (
            INSERT INTO identifier (type, value)
                VALUES (:type, :value)
                ON CONFLICT (type, value) DO NOTHING
                RETURNING id)
        SELECT COALESCE(
                       (SELECT id FROM ins),
                       (SELECT id FROM identifier WHERE type = :type AND value = :value)
               ) AS id
    """
    )
    fun insertOrGet(type: IdentifierType, value: String): Long
}